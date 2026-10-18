package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * Agent 实例健康检查响应 DTO.
 */
public class InstanceHealthResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String instanceCode;
    private String agentType;
    private String runMode;
    private boolean healthy;
    private String containerStatus;

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public String getContainerStatus() {
        return containerStatus;
    }

    public void setContainerStatus(String containerStatus) {
        this.containerStatus = containerStatus;
    }
}
