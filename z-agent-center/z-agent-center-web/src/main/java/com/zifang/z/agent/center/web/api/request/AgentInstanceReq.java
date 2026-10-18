package com.zifang.z.agent.center.web.api.request;

public class AgentInstanceReq {
    private String appCode;
    private String userId;
    private String userName;

    // ===== FEATURE014 多 Agent 创建 — create-external 端点使用 =====
    /**
     * Agent 类型: ZBOT / HERMES_CONTAINER / HERMES_LOCAL / CLAUDE_CODE / OPENCODE / CUSTOM
     */
    private String agentType;
    /**
     * 外部 Agent 回调地址（HERMES_CONTAINER / HERMES_LOCAL / CLAUDE_CODE / OPENCODE 必填）
     */
    private String callbackUrl;
    /**
     * CUSTOM 类型的自定义启动命令（包含 {port} 占位符）
     */
    private String customCommand;

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
