package com.zifang.z.agent.center.core.agent.instance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

@BizModel(name = "z_agent_instance", module = "z-agent", description = "z_agent_instance")
@TableName("z_agent_instance")
public class AgentInstance {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "实例编码", type = BizField.FieldType.STRING)
    private String instanceCode;
    @BizField(name = "所属应用编码", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "应用版本", type = BizField.FieldType.STRING)
    private String appVersion;
    @BizField(name = "实例名称", type = BizField.FieldType.STRING)
    private String instanceName;
    @BizField(name = "所有者ID", type = BizField.FieldType.STRING)
    private String ownerId;
    @BizField(name = "所有者名称", type = BizField.FieldType.STRING)
    private String ownerName;
    @BizField(name = "实例配置", type = BizField.FieldType.STRING)
    private String config;
    @BizField(name = "状态", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "访问次数", type = BizField.FieldType.INTEGER)
    private Integer visitCount;
    @BizField(name = "最后访问时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime lastVisitTime;
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "gmtModified", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;
    @BizField(name = "是否删除", type = BizField.FieldType.INTEGER)
    private Integer isDeleted;
    @BizField(name = "运行模式: IN_PROCESS/CONTAINER", type = BizField.FieldType.STRING)
    private String runMode;
    @BizField(name = "Docker容器ID(CONTAINER模式)", type = BizField.FieldType.STRING)
    private String containerId;
    @BizField(name = "容器端口(CONTAINER模式)", type = BizField.FieldType.INTEGER)
    private Integer containerPort;
    @BizField(name = "agentType", type = BizField.FieldType.STRING)
    private String agentType;
    @BizField(name = "callbackUrl", type = BizField.FieldType.STRING)
    private String callbackUrl;
    @BizField(name = "skillRootPath", type = BizField.FieldType.STRING)
    private String skillRootPath;
    @BizField(name = "authToken", type = BizField.FieldType.STRING)
    private String authToken;
    @BizField(name = "lastHeartbeat", type = BizField.FieldType.DATETIME)
    private LocalDateTime lastHeartbeat;
    @BizField(name = "agentVersion", type = BizField.FieldType.STRING)
    private String agentVersion;
    @BizField(name = "capabilities", type = BizField.FieldType.STRING)
    private String capabilities;

    // ===== FEATURE013 T2.2: Widget 嵌入字段 =====
    @BizField(name = "widgetEnabled", type = BizField.FieldType.INTEGER)
    private Integer widgetEnabled;
    @BizField(name = "widgetApiKey", type = BizField.FieldType.STRING)
    private String widgetApiKey;
    @BizField(name = "widgetRateLimit", type = BizField.FieldType.INTEGER)
    private Integer widgetRateLimit;
    @BizField(name = "widgetCors", type = BizField.FieldType.STRING)
    private String widgetCors;
    @BizField(name = "widgetEmbedCount", type = BizField.FieldType.INTEGER)
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



