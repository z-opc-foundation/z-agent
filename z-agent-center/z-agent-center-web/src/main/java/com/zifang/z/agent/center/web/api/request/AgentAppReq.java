package com.zifang.z.agent.center.web.api.request;

public class AgentAppReq {
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
}
