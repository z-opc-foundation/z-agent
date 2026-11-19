package com.zifang.z.agent.center.core.agent.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * 节点执行记录实体 (FEATURE013 A7)
 * 对应表: z_agent_flow_node_execution
 */
@BizModel(name = "节点执行记录实体 (FEATURE013 A7)", module = "z-agent", description = "节点执行记录实体 (FEATURE013 A7) 对应表: z_agent_flow_node_execution")
@TableName("z_agent_flow_node_execution")
public class FlowNodeExecutionPO {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * 关联 flow_execution.execution_id
     */
    @BizField(name = "关联 flow_execution.execution_id", type = BizField.FieldType.STRING)
    private String executionId;

    /**
     * 节点ID (来自 flow_definition.nodes[].id)
     */
    @BizField(name = "节点ID (来自 flow_definition.nodes[].id)", type = BizField.FieldType.STRING)
    private String nodeId;

    /**
     * 节点类型: start/llm/agent/condition/http/template/parallel/sub_flow/loop/script/end
     */
    @BizField(name = "nodeType", type = BizField.FieldType.STRING)
    private String nodeType;

    /**
     * PENDING / RUNNING / SUCCESS / FAILED / SKIPPED
     */
    @BizField(name = "status", type = BizField.FieldType.STRING)
    private String status = "PENDING";

    /**
     * 执行顺序 (用于回放)
     */
    @BizField(name = "执行顺序 (用于回放)", type = BizField.FieldType.INTEGER)
    private Integer sequence = 0;

    /**
     * 第几次尝试 (重试相关)
     */
    @BizField(name = "第几次尝试 (重试相关)", type = BizField.FieldType.INTEGER)
    private Integer attempt = 1;

    /**
     * 节点入参 JSON (前置节点输出)
     */
    @BizField(name = "节点入参 JSON (前置节点输出)", type = BizField.FieldType.STRING)
    private String inputs;

    /**
     * 节点出参 JSON
     */
    @BizField(name = "节点出参 JSON", type = BizField.FieldType.STRING)
    private String outputs;

    /**
     * 节点配置快照 JSON
     */
    @BizField(name = "节点配置快照 JSON", type = BizField.FieldType.STRING)
    private String configSnapshot;

    @BizField(name = "startedAt", type = BizField.FieldType.DATETIME)
    private LocalDateTime startedAt;

    @BizField(name = "endedAt", type = BizField.FieldType.DATETIME)
    private LocalDateTime endedAt;

    /**
     * 单节点耗时 (毫秒)
     */
    @BizField(name = "单节点耗时 (毫秒)", type = BizField.FieldType.INTEGER)
    private Integer elapsedMs;

    /**
     * 错误信息
     */
    @BizField(name = "错误信息", type = BizField.FieldType.STRING)
    private String errorMessage;

    @BizField(name = "createdAt", type = BizField.FieldType.DATETIME)
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

