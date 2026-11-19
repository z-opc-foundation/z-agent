package com.zifang.z.agent.center.core.agent.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

@BizModel(name = "z_agent_conversation", module = "z-agent", description = "z_agent_conversation")
@TableName("z_agent_conversation")
public class AgentConversation {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "会话编码", type = BizField.FieldType.STRING)
    private String conversationCode;
    @BizField(name = "实例编码", type = BizField.FieldType.STRING)
    private String instanceCode;
    @BizField(name = "用户ID", type = BizField.FieldType.STRING)
    private String userId;
    @BizField(name = "用户名称", type = BizField.FieldType.STRING)
    private String userName;
    @BizField(name = "userMessage", type = BizField.FieldType.STRING)
    private String userMessage;
    @BizField(name = "assistantMessage", type = BizField.FieldType.STRING)
    private String assistantMessage;
    @BizField(name = "modelName", type = BizField.FieldType.STRING)
    private String modelName;
    @BizField(name = "tokenCount", type = BizField.FieldType.INTEGER)
    private Integer tokenCount;
    @BizField(name = "toolCalls", type = BizField.FieldType.STRING)
    private String toolCalls;
    @BizField(name = "latencyMs", type = BizField.FieldType.INTEGER)
    private Integer latencyMs;
    @BizField(name = "状态", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "errorMsg", type = BizField.FieldType.STRING)
    private String errorMsg;
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConversationCode() {
        return conversationCode;
    }

    public void setConversationCode(String conversationCode) {
        this.conversationCode = conversationCode;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(String assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(String toolCalls) {
        this.toolCalls = toolCalls;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
}



