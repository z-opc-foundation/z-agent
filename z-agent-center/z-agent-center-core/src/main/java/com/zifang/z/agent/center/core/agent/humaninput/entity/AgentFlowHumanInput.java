package com.zifang.z.agent.center.core.agent.humaninput.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * FEATURE013 T3.5: Human-in-the-Loop 实体
 */
@BizModel(name = "FEATURE013 T3.5: Human-in-the-Loop 实体", module = "z-agent", description = "FEATURE013 T3.5: Human-in-the-Loop 实体")
@TableName("z_agent_flow_human_input")
public class AgentFlowHumanInput {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "requestCode", type = BizField.FieldType.STRING)
    private String requestCode;
    @BizField(name = "flowCode", type = BizField.FieldType.STRING)
    private String flowCode;
    @BizField(name = "executionId", type = BizField.FieldType.STRING)
    private String executionId;
    @BizField(name = "nodeId", type = BizField.FieldType.STRING)
    private String nodeId;
    @BizField(name = "nodeName", type = BizField.FieldType.STRING)
    private String nodeName;
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "prompt", type = BizField.FieldType.STRING)
    private String prompt;
    @BizField(name = "inputData", type = BizField.FieldType.STRING)
    private String inputData;
    @BizField(name = "formSchema", type = BizField.FieldType.STRING)
    private String formSchema;
    @BizField(name = "assignee", type = BizField.FieldType.STRING)
    private String assignee;
    @BizField(name = "approverRole", type = BizField.FieldType.STRING)
    private String approverRole;
    @BizField(name = "status", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "priority", type = BizField.FieldType.STRING)
    private String priority;
    @BizField(name = "dueAt", type = BizField.FieldType.DATETIME)
    private LocalDateTime dueAt;
    @BizField(name = "decidedBy", type = BizField.FieldType.STRING)
    private String decidedBy;
    @BizField(name = "decidedAt", type = BizField.FieldType.DATETIME)
    private LocalDateTime decidedAt;
    @BizField(name = "resultJson", type = BizField.FieldType.STRING)
    private String resultJson;
    @BizField(name = "resultText", type = BizField.FieldType.STRING)
    private String resultText;
    @BizField(name = "comments", type = BizField.FieldType.STRING)
    private String comments;
    @BizField(name = "requesterId", type = BizField.FieldType.STRING)
    private String requesterId;
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "gmtModified", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getFlowCode() {
        return flowCode;
    }

    public void setFlowCode(String flowCode) {
        this.flowCode = flowCode;
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

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getFormSchema() {
        return formSchema;
    }

    public void setFormSchema(String formSchema) {
        this.formSchema = formSchema;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getApproverRole() {
        return approverRole;
    }

    public void setApproverRole(String approverRole) {
        this.approverRole = approverRole;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }
}



