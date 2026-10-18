package com.zifang.z.agent.center.core.app.dto;

import java.io.Serializable;

public class AgentAppReq implements Serializable {
    private Long id;
    private String appCode;
    private String appName;
    private String description;
    private String iconUrl;
    private String prompt;
    private String modelName;
    private String modelProvider;
    private Object tools;
    private Object knowledgeIds;
    private Object skillCodes;
    private Object variables;
    private String status;
    private String groupCode;
    // 应用类型: chat-对话 / agent-智能体 / workflow-工作流
    private String agentMode;
    // 模型参数配置
    private Float temperature;
    private Float topP;
    private Integer maxTokens;
    private String responseFormat;
    // 记忆配置
    private Integer historyWindowSize;
    private Boolean enableLongMemory;
    private String memoryPrompt;

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

    public Object getTools() {
        return tools;
    }

    public void setTools(Object tools) {
        this.tools = tools;
    }

    public Object getKnowledgeIds() {
        return knowledgeIds;
    }

    public void setKnowledgeIds(Object knowledgeIds) {
        this.knowledgeIds = knowledgeIds;
    }

    public Object getSkillCodes() {
        return skillCodes;
    }

    public void setSkillCodes(Object skillCodes) {
        this.skillCodes = skillCodes;
    }

    public Object getVariables() {
        return variables;
    }

    public void setVariables(Object variables) {
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

    public String getAgentMode() {
        return agentMode;
    }

    public void setAgentMode(String agentMode) {
        this.agentMode = agentMode;
    }

    // 模型参数配置
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

    // 记忆配置
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
}
