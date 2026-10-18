package com.zifang.z.agent.center.core.agent.instance.dto;

import java.io.Serializable;

public class AgentInstanceDto implements Serializable {
    private Long id;
    private String instanceCode;
    private String appCode;
    private String appVersion;
    private String instanceName;
    private String ownerId;
    private String ownerName;
    private String status;
    private Integer visitCount;
    private String lastVisitTime;
    private String runMode;
    private String containerId;
    private Integer containerPort;

    // ===== FEATURE014 多 Agent 扩展字段 =====
    /**
     * Agent 类型: ZBOT / HERMES_CONTAINER / HERMES_LOCAL / CLAUDE_CODE / OPENCODE / CUSTOM
     */
    private String agentType;
    /**
     * Agent 回调地址（外部 Agent 必填，ZBOT 留空）
     */
    private String callbackUrl;
    /**
     * Skill 落盘根路径（按 agentType 自动推导，可覆盖）
     */
    private String skillRootPath;
    /**
     * Agent 最后心跳时间（格式 yyyy-MM-dd HH:mm:ss）
     */
    private String lastHeartbeat;
    /**
     * Agent 版本号
     */
    private String agentVersion;
    /**
     * Agent 能力描述（JSON 字符串）
     */
    private String capabilities;

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

    public String getLastVisitTime() {
        return lastVisitTime;
    }

    public void setLastVisitTime(String lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
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

    // ===== FEATURE014 getters/setters =====

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

    public String getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(String lastHeartbeat) {
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
}
