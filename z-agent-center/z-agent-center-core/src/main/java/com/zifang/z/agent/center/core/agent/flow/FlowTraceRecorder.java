package com.zifang.z.agent.center.core.agent.flow;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.agent.flow.entity.FlowExecutionPO;
import com.zifang.z.agent.center.core.agent.flow.entity.FlowNodeExecutionPO;
import com.zifang.z.agent.center.core.agent.flow.mapper.FlowExecutionMapper;
import com.zifang.z.agent.center.core.agent.flow.mapper.FlowNodeExecutionMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FEATURE013 A7: 流程执行追踪记录器
 * 把 FlowEngine 的每次执行 (FlowExecution) 和每个节点执行 (FlowNodeExecution)
 * 落到 z_agent_flow_execution / z_agent_flow_node_execution 两张表, 用于:
 * 1. 调试回放: 可按 executionId 拉出完整执行链
 * 2. 性能分析: per-node elapsedMs + inputs/outputs
 * 3. 错误追踪: 节点级 errorMessage 落库
 *
 * <p>失败容忍: 任何 DB 写异常都被 try/catch 吃掉, 只 log warn, 不影响流程执行。
 * 也就是说, 即使这张表被 drop / schema 没建, 主流程也不会挂。
 */
@Component
public class FlowTraceRecorder {

    private static final Logger log = LogManager.getLogger(FlowTraceRecorder.class);
    /**
     * 内存中的执行上下文 (per-executionId): 保存 sequence 计数 / 触发元信息.
     * 流程结束 (finishExecution) 后清理.
     */
    private final Map<String, ExecutionContext> contexts = new ConcurrentHashMap<>();
    @Autowired
    private FlowExecutionMapper flowExecutionMapper;
    @Autowired
    private FlowNodeExecutionMapper flowNodeExecutionMapper;

    /**
     * 工具方法: 把对象序列化成 JSON 字符串 (inputs/outputs 入库用)
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return JsonUtil.toJsonExcludingNulls(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    /**
     * 启动一次流程执行 (写入 z_agent_flow_execution, status=RUNNING)
     */
    public void startExecution(String executionId, String flowId, Integer flowVersion,
                               String triggerType, String triggerUser, String triggerSource,
                               String inputVariablesJson, String inputMessage, String tenantCode,
                               int nodeCount) {
        try {
            FlowExecutionPO po = new FlowExecutionPO();
            po.setExecutionId(executionId);
            po.setFlowId(flowId);
            po.setFlowVersion(flowVersion == null ? 1 : flowVersion);
            po.setTriggerType(triggerType == null ? "MANUAL" : triggerType);
            po.setTriggerUser(triggerUser);
            po.setTriggerSource(triggerSource);
            po.setInputVariables(inputVariablesJson);
            po.setInputMessage(inputMessage);
            po.setStatus("RUNNING");
            po.setNodeCount(nodeCount);
            po.setCompletedCount(0);
            po.setStartedAt(LocalDateTime.now());
            po.setTenantCode(tenantCode);
            flowExecutionMapper.insert(po);

            ExecutionContext ctx = new ExecutionContext();
            ctx.sequence.set(0);
            ctx.flowId = flowId;
            contexts.put(executionId, ctx);
        } catch (Exception e) {
            log.warn("[FlowTraceRecorder] startExecution 失败 executionId={}: {}", executionId, e.getMessage());
        }
    }

    /**
     * 标记节点开始 (写入 z_agent_flow_node_execution, status=RUNNING)
     * 返回 nodeRecordId 用于后续 recordNodeEnd 调用
     */
    public Long recordNodeStart(String executionId, String nodeId, String nodeType,
                                String inputsJson, String configSnapshotJson) {
        try {
            ExecutionContext ctx = contexts.get(executionId);
            int seq = ctx == null ? 0 : ctx.sequence.incrementAndGet();

            FlowNodeExecutionPO po = new FlowNodeExecutionPO();
            po.setExecutionId(executionId);
            po.setNodeId(nodeId);
            po.setNodeType(nodeType);
            po.setStatus("RUNNING");
            po.setSequence(seq);
            po.setAttempt(1);
            po.setInputs(inputsJson);
            po.setConfigSnapshot(configSnapshotJson);
            po.setStartedAt(LocalDateTime.now());
            po.setCreatedAt(LocalDateTime.now());
            flowNodeExecutionMapper.insert(po);
            return po.getId();
        } catch (Exception e) {
            log.warn("[FlowTraceRecorder] recordNodeStart 失败 executionId={}, nodeId={}: {}",
                    executionId, nodeId, e.getMessage());
            return null;
        }
    }

    /**
     * 标记节点结束 (UPDATE z_agent_flow_node_execution)
     */
    public void recordNodeEnd(Long nodeRecordId, String executionId, String status,
                              String outputsJson, Integer elapsedMs, String errorMessage) {
        try {
            if (nodeRecordId == null) {
                return;
            }
            FlowNodeExecutionPO po = new FlowNodeExecutionPO();
            po.setId(nodeRecordId);
            po.setStatus(status); // SUCCESS / FAILED / TIMEOUT / SKIPPED
            po.setOutputs(outputsJson);
            po.setElapsedMs(elapsedMs);
            po.setErrorMessage(errorMessage);
            po.setEndedAt(LocalDateTime.now());
            flowNodeExecutionMapper.updateById(po);

            // 同时把 completedCount 累加到 flow_execution
            incrementCompletedCount(executionId);
        } catch (Exception e) {
            log.warn("[FlowTraceRecorder] recordNodeEnd 失败 executionId={}, nodeRecordId={}: {}",
                    executionId, nodeRecordId, e.getMessage());
        }
    }

    /**
     * 流程结束 (UPDATE z_agent_flow_execution, status=SUCCESS/FAILED/TIMEOUT/CANCELLED)
     */
    public void finishExecution(String executionId, String status, Integer completedCount,
                                Integer elapsedMs, String errorMessage,
                                String outputsJson, String finalAnswer) {
        try {
            FlowExecutionPO po = new FlowExecutionPO();
            po.setExecutionId(executionId);
            po.setStatus(status);
            po.setCompletedCount(completedCount == null ? 0 : completedCount);
            po.setElapsedMs(elapsedMs);
            po.setErrorMessage(errorMessage);
            po.setOutputs(outputsJson);
            po.setFinalAnswer(finalAnswer);
            po.setEndedAt(LocalDateTime.now());
            flowExecutionMapper.update(po,
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FlowExecutionPO>()
                            .eq("execution_id", executionId));
        } catch (Exception e) {
            log.warn("[FlowTraceRecorder] finishExecution 失败 executionId={}: {}", executionId, e.getMessage());
        } finally {
            contexts.remove(executionId);
        }
    }

    private void incrementCompletedCount(String executionId) {
        try {
            FlowExecutionPO existing = flowExecutionMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FlowExecutionPO>()
                            .eq("execution_id", executionId));
            if (existing != null && existing.getCompletedCount() != null) {
                FlowExecutionPO update = new FlowExecutionPO();
                update.setId(existing.getId());
                update.setCompletedCount(existing.getCompletedCount() + 1);
                flowExecutionMapper.updateById(update);
            }
        } catch (Exception ignored) {
            // 计数失败不影响主流程
        }
    }

    /**
     * 每个执行一份内存上下文, 记录 sequence
     */
    private static class ExecutionContext {
        final AtomicInteger sequence = new AtomicInteger(0);
        String flowId;
    }
}