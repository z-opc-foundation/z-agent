package com.zifang.z.agent.center.core.agent.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * 流程执行记录实体 (FEATURE013 A7)
 * 对应表: z_agent_flow_execution
 */
@BizModel(name = "流程执行记录实体 (FEATURE013 A7)", module = "z-agent", description = "流程执行记录实体 (FEATURE013 A7) 对应表: z_agent_flow_execution")
@TableName("z_agent_flow_execution")
public class FlowExecutionPO {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * 执行ID (UUID, 业务唯一)
     */
    @BizField(name = "执行ID (UUID, 业务唯一)", type = BizField.FieldType.STRING)
    private String executionId;

    /**
     * 关联 flow_definition.flow_id
     */
    @BizField(name = "关联 flow_definition.flow_id", type = BizField.FieldType.STRING)
    private String flowId;

    /**
     * 执行的流程版本号 (快照记录)
     */
    @BizField(name = "执行的流程版本号 (快照记录)", type = BizField.FieldType.INTEGER)
    private Integer flowVersion = 1;

    /**
     * MANUAL / SCHEDULED / WEBHOOK / AGENT_INNER
     */
    @BizField(name = "triggerType", type = BizField.FieldType.STRING)
    private String triggerType = "MANUAL";

    /**
     * 触发人 (MANUAL 时填)
     */
    @BizField(name = "触发人 (MANUAL 时填)", type = BizField.FieldType.STRING)
    private String triggerUser;

    /**
     * 触发来源描述
     */
    @BizField(name = "触发来源描述", type = BizField.FieldType.STRING)
    private String triggerSource;

    /**
     * 入参 JSON (流程变量)
     */
    @BizField(name = "入参 JSON (流程变量)", type = BizField.FieldType.STRING)
    private String inputVariables;

    /**
     * 入参文本 (用户消息 / Agent 输入)
     */
    @BizField(name = "入参文本 (用户消息 / Agent 输入)", type = BizField.FieldType.STRING)
    private String inputMessage;

    /**
     * RUNNING / SUCCESS / FAILED / TIMEOUT / CANCELLED
     */
    @BizField(name = "status", type = BizField.FieldType.STRING)
    private String status = "RUNNING";

    /**
     * 总节点数
     */
    @BizField(name = "总节点数", type = BizField.FieldType.INTEGER)
    private Integer nodeCount = 0;

    /**
     * 已完成节点数
     */
    @BizField(name = "已完成节点数", type = BizField.FieldType.INTEGER)
    private Integer completedCount = 0;

    /**
     * 顶层错误信息
     */
    @BizField(name = "顶层错误信息", type = BizField.FieldType.STRING)
    private String errorMessage;

    @BizField(name = "startedAt", type = BizField.FieldType.DATETIME)
    private LocalDateTime startedAt;

    @BizField(name = "endedAt", type = BizField.FieldType.DATETIME)
    private LocalDateTime endedAt;

    /**
     * 总耗时 (毫秒)
     */
    @BizField(name = "总耗时 (毫秒)", type = BizField.FieldType.INTEGER)
    private Integer elapsedMs;

    /**
     * 节点输出 JSON Map<nodeId, output>
     */
    @BizField(name = "节点输出 JSON Map<nodeId, output>", type = BizField.FieldType.STRING)
    private String outputs;

    /**
     * 最终回复文本
     */
    @BizField(name = "最终回复文本", type = BizField.FieldType.STRING)
    private String finalAnswer;

    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;

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

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public Integer getFlowVersion() {
        return flowVersion;
    }

    public void setFlowVersion(Integer flowVersion) {
        this.flowVersion = flowVersion;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerUser() {
        return triggerUser;
    }

    public void setTriggerUser(String triggerUser) {
        this.triggerUser = triggerUser;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getInputVariables() {
        return inputVariables;
    }

    public void setInputVariables(String inputVariables) {
        this.inputVariables = inputVariables;
    }

    public String getInputMessage() {
        return inputMessage;
    }

    public void setInputMessage(String inputMessage) {
        this.inputMessage = inputMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public String getOutputs() {
        return outputs;
    }

    public void setOutputs(String outputs) {
        this.outputs = outputs;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public void setFinalAnswer(String finalAnswer) {
        this.finalAnswer = finalAnswer;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}

