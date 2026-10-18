package com.zifang.z.agent.center.core.agent.flow.node;

import com.zifang.z.agent.center.core.agent.flow.FlowContext;
import com.zifang.z.agent.center.core.agent.flow.FlowNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * 并行节点 - 并行执行多个子节点并汇总结果
 * 支持等待所有节点完成（AND）或任意节点完成（OR）
 */
public class ParallelNode implements FlowNode {

    private static final Logger log = LogManager.getLogger(ParallelNode.class);

    private final String nodeId;
    private final String waitStrategy;  // ALL / ANY
    private final Map<String, Object> config = new HashMap<>();

    /**
     * 子节点列表
     */
    private List<FlowNode> childNodes = new ArrayList<>();

    /**
     * 子节点配置（用于动态创建节点）
     */
    private List<Map<String, Object>> childNodeConfigs = new ArrayList<>();

    /**
     * 超时时间(ms)
     */
    private long timeoutMs = 5 * 60 * 1000;

    /**
     * 并发执行器
     */
    private ExecutorService executor;

    public ParallelNode(String nodeId) {
        this(nodeId, "ALL");
    }

    public ParallelNode(String nodeId, String waitStrategy) {
        this.nodeId = nodeId;
        this.waitStrategy = waitStrategy;
    }

    @Override
    public String getType() {
        return TYPE_PARALLEL;
    }

    @Override
    public Object execute(Map<String, Object> inputs, FlowContext context) {
        log.info("ParallelNode[{}] starting parallel execution (strategy: {}, children: {})",
                nodeId, waitStrategy, childNodes.size());

        if (childNodes.isEmpty()) {
            log.warn("ParallelNode[{}] has no child nodes to execute", nodeId);
            return createResult(Collections.emptyMap(), null);
        }

        // 创建执行器
        if (executor == null) {
            executor = Executors.newFixedThreadPool(Math.min(childNodes.size(), 10));
        }

        // 创建子上下文列表
        List<FlowContext> subContexts = new ArrayList<>();
        for (int i = 0; i < childNodes.size(); i++) {
            FlowContext subContext = context.createSubContext(context.getRootFlowId(), nodeId + "_child_" + i);
            subContext.setVariable("_parent_inputs", inputs);
            subContexts.add(subContext);
        }

        // 准备Future列表
        List<Future<ChildExecutionResult>> futures = new ArrayList<>();

        // 提交所有子节点执行任务
        for (int i = 0; i < childNodes.size(); i++) {
            final int index = i;
            final FlowNode childNode = childNodes.get(i);
            final FlowContext subContext = subContexts.get(i);

            Future<ChildExecutionResult> future = executor.submit(() -> {
                long startTime = System.currentTimeMillis();
                try {
                    log.info("ParallelNode[{}] executing child[{}]: {}", nodeId, index, childNode.getType());

                    Object output = childNode.execute(inputs, subContext);
                    long elapsed = System.currentTimeMillis() - startTime;

                    // 合并子上下文变量到主上下文
                    synchronized (context) {
                        context.mergeSubContext(subContext);
                    }

                    return new ChildExecutionResult(index, true, output, null, elapsed);

                } catch (Exception e) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.error("ParallelNode[{}] child[{}] failed: {}", nodeId, index, e.getMessage());
                    return new ChildExecutionResult(index, false, null, e.getMessage(), elapsed);
                }
            });

            futures.add(future);
        }

        // 等待执行结果
        Map<String, Object> results = waitForResults(futures, context);

        // 清理
        executor.shutdown();

        return createResult(results, context);
    }

    private Map<String, Object> waitForResults(List<Future<ChildExecutionResult>> futures, FlowContext context) {
        Map<String, Object> results = new ConcurrentHashMap<>();

        if ("ANY".equals(waitStrategy)) {
            // 等待任意一个完成
            int completedCount = 0;
            for (Future<ChildExecutionResult> future : futures) {
                try {
                    ChildExecutionResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                    results.put("child_" + result.index, result.output);
                    context.setVariable("_parallel_first_result", result.output);
                    context.setVariable("_parallel_first_index", result.index);

                    log.info("ParallelNode[{}] first child completed (index: {}, success: {})",
                            nodeId, result.index, result.success);

                    // 取消其他任务
                    for (Future<ChildExecutionResult> f : futures) {
                        f.cancel(false);
                    }

                    return results;

                } catch (TimeoutException e) {
                    log.warn("ParallelNode[{}] timeout waiting for first child", nodeId);
                    break;
                } catch (InterruptedException | ExecutionException e) {
                    log.error("ParallelNode[{}] error waiting for children: {}", nodeId, e.getMessage());
                }
            }
        } else {
            // 等待所有完成 (ALL)
            int index = 0;
            int successCount = 0;
            int failureCount = 0;

            for (Future<ChildExecutionResult> future : futures) {
                try {
                    ChildExecutionResult result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                    results.put("child_" + result.index, result.output);

                    if (result.success) {
                        successCount++;
                    } else {
                        failureCount++;
                    }

                    log.info("ParallelNode[{}] child[{}] completed (success: {}, elapsed: {}ms)",
                            nodeId, result.index, result.success, result.elapsedMs);

                } catch (TimeoutException e) {
                    log.warn("ParallelNode[{}] child[{}] timeout", nodeId, index);
                    results.put("child_" + index, createErrorResult("Execution timeout"));
                    failureCount++;

                } catch (InterruptedException | ExecutionException e) {
                    log.error("ParallelNode[{}] child[{}] execution error: {}", nodeId, index, e.getMessage());
                    results.put("child_" + index, createErrorResult(e.getMessage()));
                    failureCount++;
                }

                index++;
            }

            // 设置汇总信息到context
            context.setVariable("_parallel_success_count", successCount);
            context.setVariable("_parallel_failure_count", failureCount);
            context.setVariable("_parallel_total_count", childNodes.size());
        }

        return results;
    }

    private Map<String, Object> createResult(Map<String, Object> childResults, FlowContext context) {
        Map<String, Object> result = new HashMap<>();
        result.put("children", childResults);
        result.put("waitStrategy", waitStrategy);

        if (context != null) {
            context.setNodeOutput(nodeId, result);
        }

        return result;
    }

    private Map<String, Object> createErrorResult(String errorMessage) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", errorMessage);
        error.put("success", false);
        return error;
    }

    /**
     * 添加子节点
     */
    public ParallelNode addChildNode(FlowNode node) {
        this.childNodes.add(node);
        return this;
    }

    /**
     * 添加子节点配置（用于通过配置创建节点）
     */
    public ParallelNode addChildNodeConfig(Map<String, Object> nodeConfig) {
        this.childNodeConfigs.add(nodeConfig);
        return this;
    }

    /**
     * 设置超时时间
     */
    public ParallelNode withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    /**
     * 设置自定义执行器
     */
    public ParallelNode withExecutor(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    public ParallelNode withConfig(String key, Object value) {
        this.config.put(key, value);
        return this;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> fullConfig = new HashMap<>(this.config);
        fullConfig.put("waitStrategy", waitStrategy);
        fullConfig.put("timeoutMs", timeoutMs);
        fullConfig.put("childCount", childNodes.size());
        return fullConfig;
    }

    @Override
    public boolean validate() {
        if (childNodes.isEmpty() && childNodeConfigs.isEmpty()) {
            log.error("ParallelNode[{}] must have at least one child node", nodeId);
            return false;
        }
        if (!Arrays.asList("ALL", "ANY").contains(waitStrategy)) {
            log.error("ParallelNode[{}] invalid wait strategy: {}", nodeId, waitStrategy);
            return false;
        }
        return true;
    }

    // Getters
    public String getNodeId() {
        return nodeId;
    }

    public String getWaitStrategy() {
        return waitStrategy;
    }

    public List<FlowNode> getChildNodes() {
        return new ArrayList<>(childNodes);
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    // ==================== 内部类 ====================

    /**
     * 子节点执行结果
     */
    private static class ChildExecutionResult {
        final int index;
        final boolean success;
        final Object output;
        final String error;
        final long elapsedMs;

        ChildExecutionResult(int index, boolean success, Object output, String error, long elapsedMs) {
            this.index = index;
            this.success = success;
            this.output = output;
            this.error = error;
            this.elapsedMs = elapsedMs;
        }
    }
}