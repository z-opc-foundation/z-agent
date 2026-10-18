package com.zifang.z.agent.center.core.agent.flow;

import com.zifang.util.core.lang.RandomUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流程编排引擎 - 负责执行有向无环图(DAG)结构的流程
 */
public class FlowEngine {

    private static final Logger log = LogManager.getLogger(FlowEngine.class);

    /**
     * 节点执行器池
     */
    private final ExecutorService executorPool;

    /**
     * 节点注册表
     */
    private final Map<String, FlowNode> nodeRegistry = new ConcurrentHashMap<>();

    /**
     * 正在运行的流程
     */
    private final Map<String, FlowExecution> runningExecutions = new ConcurrentHashMap<>();

    /**
     * FEATURE013 A7: 流程追踪记录器 (Spring @Component, 由 FlowOrchestrationService 注入).
     * 没有注入时 (单元测试场景) 自动降级为空操作.
     */
    private FlowTraceRecorder traceRecorder;

    public FlowEngine() {
        this(4);
    }

    public FlowEngine(int concurrency) {
        this.executorPool = Executors.newFixedThreadPool(concurrency);
    }

    /**
     * FEATURE013 A7: 注入追踪记录器 (Spring 容器回调)
     */
    public void setTraceRecorder(FlowTraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    public void registerNode(String type, FlowNode node) {
        nodeRegistry.put(type, node);
    }

    public FlowNode getNode(String type) {
        FlowNode node = nodeRegistry.get(type);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node type: " + type);
        }
        return node;
    }

    public FlowResult execute(FlowDefinition definition, FlowContext context) {
        String executionId = RandomUtil.uuid();
        context.setRootFlowId(executionId);

        FlowExecution execution = new FlowExecution(executionId, definition, context);
        runningExecutions.put(executionId, execution);

        long startMs = System.currentTimeMillis();

        // FEATURE013 A7: 开始 trace (写 z_agent_flow_execution RUNNING)
        if (traceRecorder != null) {
            try {
                traceRecorder.startExecution(
                        executionId,
                        definition.getFlowId(),
                        null,
                        "MANUAL",
                        context.getUserId(),
                        null,
                        FlowTraceRecorder.toJson(context.getVariables()),
                        null,
                        null,
                        definition.getNodeIds().size());
            } catch (Exception e) {
                // 追踪失败不阻塞主流程
            }
        }

        try {
            return executeDAG(definition, context, execution, executionId, startMs);
        } finally {
            runningExecutions.remove(executionId);
        }
    }

    private FlowResult executeDAG(FlowDefinition definition, FlowContext context,
                                  FlowExecution execution, String executionId, long startMs) {
        Set<String> nodeIdSet = definition.getNodeIds();
        List<String> nodeIds = new ArrayList<String>(nodeIdSet);
        Map<String, List<String>> edgeMap = definition.getEdges();

        Map<String, Integer> inDegree = new HashMap<String, Integer>();
        for (String nodeId : nodeIds) {
            inDegree.put(nodeId, 0);
        }
        for (Map.Entry<String, List<String>> entry : edgeMap.entrySet()) {
            for (String target : entry.getValue()) {
                Integer current = inDegree.get(target);
                inDegree.put(target, current == null ? 1 : current + 1);
            }
        }

        LinkedList<String> queue = new LinkedList<String>();
        for (String nodeId : nodeIds) {
            if (inDegree.get(nodeId) == 0) {
                queue.offer(nodeId);
            }
        }

        final AtomicInteger completedCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(nodeIds.size());
        final Map<String, Object> nodeOutputs = new ConcurrentHashMap<String, Object>();

        while (!queue.isEmpty()) {
            final List<String> batch = new ArrayList<String>();
            synchronized (queue) {
                if (!queue.isEmpty()) {
                    batch.add(queue.remove());
                }
            }

            for (final String nodeId : batch) {
                executorPool.submit(new Runnable() {
                    public void run() {
                        // FEATURE013 A7: 记录节点开始
                        Long nodeRecordId = null;
                        FlowNode flowNodeForTrace = null;
                        if (traceRecorder != null) {
                            try {
                                flowNodeForTrace = definition.getNode(nodeId);
                                nodeRecordId = traceRecorder.recordNodeStart(
                                        executionId, nodeId,
                                        flowNodeForTrace.getType(),
                                        null, null);
                            } catch (Exception ignored) {
                            }
                        }
                        long nodeStartMs = System.currentTimeMillis();
                        try {
                            long startNode = System.currentTimeMillis();

                            FlowNode node = definition.getNode(nodeId);
                            context.setCurrentNodeId(nodeId);

                            Map<String, Object> inputs = new HashMap<String, Object>();
                            for (Map.Entry<String, List<String>> e : edgeMap.entrySet()) {
                                if (e.getValue().contains(nodeId)) {
                                    Object prevOutput = nodeOutputs.get(e.getKey());
                                    if (prevOutput != null) {
                                        inputs.put(e.getKey(), prevOutput);
                                    }
                                }
                            }

                            log.info("Executing node: {} [{}]", nodeId, node.getType());
                            Object output = node.execute(inputs, context);
                            nodeOutputs.put(nodeId, output);

                            synchronized (FlowEngine.this) {
                                List<String> nextNodes = edgeMap.get(nodeId);
                                if (nextNodes != null) {
                                    for (String nextId : nextNodes) {
                                        Integer deg = inDegree.get(nextId);
                                        int newDegree = deg == null ? 0 : deg - 1;
                                        inDegree.put(nextId, newDegree);
                                        if (newDegree == 0) {
                                            queue.offer(nextId);
                                        }
                                    }
                                }
                            }

                            context.setPreviousNodeId(nodeId);
                            long elapsed = System.currentTimeMillis() - startNode;
                            log.info("Node completed: {} in {}ms", nodeId, elapsed);

                            // FEATURE013 A7: 记录节点成功
                            if (traceRecorder != null) {
                                try {
                                    traceRecorder.recordNodeEnd(nodeRecordId, executionId,
                                            "SUCCESS", FlowTraceRecorder.toJson(output),
                                            (int) (System.currentTimeMillis() - nodeStartMs), null);
                                } catch (Exception ignored) {
                                }
                            }

                        } catch (Exception e) {
                            log.error("Node execution failed: " + nodeId, e);
                            execution.addError(nodeId, e.getMessage());

                            // FEATURE013 A7: 记录节点失败
                            if (traceRecorder != null) {
                                try {
                                    traceRecorder.recordNodeEnd(nodeRecordId, executionId,
                                            "FAILED", null,
                                            (int) (System.currentTimeMillis() - nodeStartMs),
                                            e.getMessage());
                                } catch (Exception ignored) {
                                }
                            }
                        } finally {
                            latch.countDown();
                            completedCount.incrementAndGet();
                        }
                    }
                });
            }
        }

        try {
            latch.await(definition.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String execId = execution.id;
            return FlowResult.timeout(execId);
        }

        FlowResult result = new FlowResult();
        String execIdForResult = execution.id;
        result.setExecutionId(execIdForResult);
        result.setSuccess(execution.getErrors().isEmpty());
        result.setOutputs(nodeOutputs);
        result.setContext(context);
        result.setNodeCount(nodeIds.size());
        result.setCompletedCount(completedCount.get());
        List<FlowResult.ErrorInfo> errorList = new ArrayList<FlowResult.ErrorInfo>();
        for (Map.Entry<String, String> err : execution.getErrors().entrySet()) {
            errorList.add(new FlowResult.ErrorInfo(err.getKey(), err.getValue()));
        }
        result.setErrors(errorList);
        result.setEndTime(System.currentTimeMillis());
        result.setElapsedMs(result.getEndTime() - result.getStartTime());

        // FEATURE013 A7: 记录流程结束 (UPDATE z_agent_flow_execution)
        if (traceRecorder != null) {
            try {
                String finalStatus = result.isSuccess() ? "SUCCESS" : "FAILED";
                String errorMsg = errorList.isEmpty() ? null
                        : errorList.get(0).getNodeId() + ": " + errorList.get(0).getMessage();
                traceRecorder.finishExecution(
                        execIdForResult,
                        finalStatus,
                        result.getCompletedCount(),
                        (int) result.getElapsedMs(),
                        errorMsg,
                        FlowTraceRecorder.toJson(nodeOutputs),
                        null);
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public boolean validateDAG(FlowDefinition definition) {
        Set<String> nodeIds = new HashSet<String>(definition.getNodeIds());
        Map<String, List<String>> edges = definition.getEdges();

        for (Map.Entry<String, List<String>> entry : edges.entrySet()) {
            if (entry.getValue().contains(entry.getKey())) {
                log.error("Self-loop detected on node: {}", entry.getKey());
                return false;
            }
        }
        return true;
    }

    public void shutdown() {
        executorPool.shutdownNow();
    }

    static class FlowExecution {
        final String id;
        final FlowDefinition definition;
        final FlowContext context;
        final Map<String, String> errors = new ConcurrentHashMap<String, String>();

        FlowExecution(String id, FlowDefinition definition, FlowContext context) {
            this.id = id;
            this.definition = definition;
            this.context = context;
        }

        void addError(String nodeId, String error) {
            errors.put(nodeId, error);
        }

        Map<String, String> getErrors() {
            return errors;
        }
    }
}