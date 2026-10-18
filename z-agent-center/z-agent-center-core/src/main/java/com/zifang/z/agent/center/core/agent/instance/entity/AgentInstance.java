package com.zifang.z.agent.center.core.agent.instance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("z_agent_instance")
public class AgentInstance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String instanceCode;
    private String appCode;
    private String appVersion;
    private String instanceName;
    private String ownerId;
    private String ownerName;
    private String config;
    private String status;
    private Integer visitCount;
    private LocalDateTime lastVisitTime;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    private Integer isDeleted;
    private String runMode;
    private String containerId;
    private Integer containerPort;
    private String agentType;
    private String callbackUrl;
    private String skillRootPath;
    private String authToken;
    private LocalDateTime lastHeartbeat;
    private String agentVersion;
    private String capabilities;

    // ===== FEATURE013 T2.2: Widget 嵌入字段 =====
    private Integer widgetEnabled;
    private String widgetApiKey;
    private Integer widgetRateLimit;
    private String widgetCors;
    private Long widgetEmbedCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(Integer visitCount) {
        this.visitCount = visitCount;
    }

    public LocalDateTime getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(LocalDateTime lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
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

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public Integer getContainerPort() {
        return containerPort;
    }

    public void setContainerPort(Integer containerPort) {
        this.containerPort = containerPort;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getSkillRootPath() {
        return skillRootPath;
    }

    public void setSkillRootPath(String skillRootPath) {
        this.skillRootPath = skillRootPath;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public Integer getWidgetEnabled() {
        return widgetEnabled;
    }

    public void setWidgetEnabled(Integer widgetEnabled) {
        this.widgetEnabled = widgetEnabled;
    }

    public String getWidgetApiKey() {
        return widgetApiKey;
    }

    public void setWidgetApiKey(String widgetApiKey) {
        this.widgetApiKey = widgetApiKey;
    }

    public Integer getWidgetRateLimit() {
        return widgetRateLimit;
    }

    public void setWidgetRateLimit(Integer widgetRateLimit) {
        this.widgetRateLimit = widgetRateLimit;
    }

    public String getWidgetCors() {
        return widgetCors;
    }

    public void setWidgetCors(String widgetCors) {
        this.widgetCors = widgetCors;
    }

    public Long getWidgetEmbedCount() {
        return widgetEmbedCount;
    }

    public void setWidgetEmbedCount(Long widgetEmbedCount) {
        this.widgetEmbedCount = widgetEmbedCount;
    }
}
