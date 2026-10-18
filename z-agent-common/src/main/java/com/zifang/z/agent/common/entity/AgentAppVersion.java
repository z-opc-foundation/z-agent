package com.zifang.z.agent.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Agent 应用版本实体（共享模块，供 z-agent-center-core 和 z-agent-engine 使用）
 */
@TableName("z_agent_app_version")
public class AgentAppVersion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String appCode;
    private String version;
    private String prompt;
    private String modelName;
    private String modelProvider;
    private String tools;
    private String knowledgeIds;
    private String skillCodes;
    private String variables;
    private String changeLog;
    private String status;
    private Float temperature;
    private Float topP;
    private Integer maxTokens;
    private String responseFormat;
    private Integer historyWindowSize;
    private Boolean enableLongMemory;
    private String memoryPrompt;
    private LocalDateTime gmtCreate;
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