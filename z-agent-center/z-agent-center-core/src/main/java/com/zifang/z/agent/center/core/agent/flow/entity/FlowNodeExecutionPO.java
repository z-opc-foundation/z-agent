package com.zifang.z.agent.center.core.agent.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 节点执行记录实体 (FEATURE013 A7)
 * 对应表: z_agent_flow_node_execution
 */
@TableName("z_agent_flow_node_execution")
public class FlowNodeExecutionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联 flow_execution.execution_id
     */
    private String executionId;

    /**
     * 节点ID (来自 flow_definition.nodes[].id)
     */
    private String nodeId;

    /**
     * 节点类型: start/llm/agent/condition/http/template/parallel/sub_flow/loop/script/end
     */
    private String nodeType;

    /**
     * PENDING / RUNNING / SUCCESS / FAILED / SKIPPED
     */
    private String status = "PENDING";

    /**
     * 执行顺序 (用于回放)
     */
    private Integer sequence = 0;

    /**
     * 第几次尝试 (重试相关)
     */
    private Integer attempt = 1;

    /**
     * 节点入参 JSON (前置节点输出)
     */
    private String inputs;

    /**
     * 节点出参 JSON
     */
    private String outputs;

    /**
     * 节点配置快照 JSON
     */
    private String configSnapshot;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    /**
     * 单节点耗时 (毫秒)
     */
    private Integer elapsedMs;

    /**
     * 错误信息
     */
    private String errorMessage;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public String getInputs() {
        return inputs;
    }

    public void setInputs(String inputs) {
        this.inputs = inputs;
    }

    public String getOutputs() {
        return outputs;
    }

    public void setOutputs(String outputs) {
        this.outputs = outputs;
    }

    public String getConfigSnapshot() {
        return configSnapshot;
    }

    public void setConfigSnapshot(String configSnapshot) {
        this.configSnapshot = configSnapshot;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Integer getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Integer elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}