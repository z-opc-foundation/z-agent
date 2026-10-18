package com.zifang.z.agent.center.core.agent.instance.dto;

import java.io.Serializable;

public class AgentInstanceReq implements Serializable {
    private String instanceCode;
    private String appCode;
    private String userId;
    private String userName;
    // 运行模式: IN_PROCESS=纯内存执行, CONTAINER=Docker容器代理
    private String runMode;

    // ===== FEATURE014 多 Agent — create-external 端点使用 =====
    /**
     * Agent 类型: ZBOT / HERMES_CONTAINER / HERMES_LOCAL / CLAUDE_CODE / OPENCODE / CUSTOM
     */
    private String agentType;
    /**
     * 外部 Agent 回调地址
     */
    private String callbackUrl;
    /**
     * CUSTOM 类型的自定义启动命令
     */
    private String customCommand;

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

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
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

    public String getCustomCommand() {
        return customCommand;
    }

    public void setCustomCommand(String customCommand) {
        this.customCommand = customCommand;
    }
}
