package com.zifang.z.agent.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * Agent 应用实体（共享模块，供 z-agent-center-core 和 z-agent-engine 使用）
 * 注意：本类不含任何 mapper/DB 操作，仅作为数据载体
 */
@BizModel(name = "Agent 应用实体（共享模块，供 z-agent-center-core 和 z-agent-engine 使用）", module = "z-agent", description = "Agent 应用实体（共享模块，供 z-agent-center-core 和 z-agent-engine 使用） 注意：本类不含任何 mapper/DB 操作，仅作为数据载体")
@TableName("z_agent_app")
public class AgentApp {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "应用编码(唯一标识)", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "应用名称", type = BizField.FieldType.STRING)
    private String appName;
    @BizField(name = "应用描述", type = BizField.FieldType.STRING)
    private String description;
    @BizField(name = "iconUrl", type = BizField.FieldType.STRING)
    private String iconUrl;
    @BizField(name = "prompt", type = BizField.FieldType.STRING)
    private String prompt;
    @BizField(name = "modelName", type = BizField.FieldType.STRING)
    private String modelName;
    @BizField(name = "modelProvider", type = BizField.FieldType.STRING)
    private String modelProvider;
    @BizField(name = "tools", type = BizField.FieldType.STRING)
    private String tools;
    @BizField(name = "knowledgeIds", type = BizField.FieldType.STRING)
    private String knowledgeIds;
    @BizField(name = "skillCodes", type = BizField.FieldType.STRING)
    private String skillCodes;
    @BizField(name = "variables", type = BizField.FieldType.STRING)
    private String variables;
    @BizField(name = "状态: ENABLE/DISABLE", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "groupCode", type = BizField.FieldType.STRING)
    private String groupCode = "";
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "creator", type = BizField.FieldType.STRING)
    private String creator;
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "gmtModified", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;
    @BizField(name = "isDeleted", type = BizField.FieldType.INTEGER)
    private Integer isDeleted;
    // 应用类型: chat-对话 / agent-智能体 / workflow-工作流
    @BizField(name = "agentMode", type = BizField.FieldType.STRING)
    private String agentMode;
    // 模型参数
    @BizField(name = "temperature", type = BizField.FieldType.DECIMAL)
    private Float temperature;
    @BizField(name = "topP", type = BizField.FieldType.DECIMAL)
    private Float topP;
    @BizField(name = "maxTokens", type = BizField.FieldType.INTEGER)
    private Integer maxTokens;
    @BizField(name = "responseFormat", type = BizField.FieldType.STRING)
    private String responseFormat;
    // 记忆配置
    @BizField(name = "historyWindowSize", type = BizField.FieldType.INTEGER)
    private Integer historyWindowSize;
    @BizField(name = "enableLongMemory", type = BizField.FieldType.BOOLEAN)
    private Boolean enableLongMemory;
    @BizField(name = "memoryPrompt", type = BizField.FieldType.STRING)
    private String memoryPrompt;
    // 分享配置：开启后生成 shareCode 用于分享链接
    @BizField(name = "shareEnabled", type = BizField.FieldType.BOOLEAN)
    private Boolean shareEnabled;
    @BizField(name = "shareCode", type = BizField.FieldType.STRING)
    private String shareCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelProvider() {
        return modelProvider;
    }

    public void setModelProvider(String modelProvider) {
        this.modelProvider = modelProvider;
    }

    public String getTools() {
        return tools;
    }

    public void setTools(String tools) {
        this.tools = tools;
    }

    public String getKnowledgeIds() {
        return knowledgeIds;
    }

    public void setKnowledgeIds(String knowledgeIds) {
        this.knowledgeIds = knowledgeIds;
    }

    public String getSkillCodes() {
        return skillCodes;
    }

    public void setSkillCodes(String skillCodes) {
        this.skillCodes = skillCodes;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
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

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getAgentMode() {
        return agentMode;
    }

    public void setAgentMode(String agentMode) {
        this.agentMode = agentMode;
    }

    public Float getTemperature() {
        return temperature;
    }

    public void setTemperature(Float temperature) {
        this.temperature = temperature;
    }

    public Float getTopP() {
        return topP;
    }

    public void setTopP(Float topP) {
        this.topP = topP;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public Integer getHistoryWindowSize() {
        return historyWindowSize;
    }

    public void setHistoryWindowSize(Integer historyWindowSize) {
        this.historyWindowSize = historyWindowSize;
    }

    public Boolean getEnableLongMemory() {
        return enableLongMemory;
    }

    public void setEnableLongMemory(Boolean enableLongMemory) {
        this.enableLongMemory = enableLongMemory;
    }

    public String getMemoryPrompt() {
        return memoryPrompt;
    }

    public void setMemoryPrompt(String memoryPrompt) {
        this.memoryPrompt = memoryPrompt;
    }

    public Boolean getShareEnabled() {
        return shareEnabled;
    }

    public void setShareEnabled(Boolean shareEnabled) {
        this.shareEnabled = shareEnabled;
    }

    public String getShareCode() {
        return shareCode;
    }

    public void setShareCode(String shareCode) {
        this.shareCode = shareCode;
    }
}

