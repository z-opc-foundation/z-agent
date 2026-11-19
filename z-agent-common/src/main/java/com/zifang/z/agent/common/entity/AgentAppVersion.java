package com.zifang.z.agent.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * Agent 应用版本实体（共享模块，供 z-agent-center-core 和 z-agent-engine 使用）
 */
@BizModel(name = "Agent 应用版本实体（共享模块，供 z-agent-center-core 和 z-agent-engine 使用）", module = "z-agent", description = "Agent 应用版本实体（共享模块，供 z-agent-center-core 和 z-agent-engine 使用）")
@TableName("z_agent_app_version")
public class AgentAppVersion {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "version", type = BizField.FieldType.STRING)
    private String version;
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
    @BizField(name = "changeLog", type = BizField.FieldType.STRING)
    private String changeLog;
    @BizField(name = "status", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "temperature", type = BizField.FieldType.DECIMAL)
    private Float temperature;
    @BizField(name = "topP", type = BizField.FieldType.DECIMAL)
    private Float topP;
    @BizField(name = "maxTokens", type = BizField.FieldType.INTEGER)
    private Integer maxTokens;
    @BizField(name = "responseFormat", type = BizField.FieldType.STRING)
    private String responseFormat;
    @BizField(name = "historyWindowSize", type = BizField.FieldType.INTEGER)
    private Integer historyWindowSize;
    @BizField(name = "enableLongMemory", type = BizField.FieldType.BOOLEAN)
    private Boolean enableLongMemory;
    @BizField(name = "memoryPrompt", type = BizField.FieldType.STRING)
    private String memoryPrompt;
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

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

