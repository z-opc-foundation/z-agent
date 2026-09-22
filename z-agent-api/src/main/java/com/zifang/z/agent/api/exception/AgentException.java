package com.zifang.z.agent.api.exception;

/**
 * z-agent 平台异常. 上层 HTTP 调用方捕获此异常 → 渲染 4xx/5xx 响应.
 *
 * <p>code: 错误码 (NOT_FOUND / INVALID_REQUEST / RUN_FAILED / TOOL_ERROR / TIMEOUT / INTERNAL).
 */
public class AgentException extends RuntimeException {

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String RUN_FAILED = "RUN_FAILED";
    public static final String TOOL_ERROR = "TOOL_ERROR";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String INTERNAL = "INTERNAL";

    private final String code;

    public AgentException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AgentException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() { return code; }

    public static AgentException agentNotFound(String name) {
        return new AgentException(NOT_FOUND, "agent not found: " + name);
    }

    public static AgentException invalidRequest(String message) {
        return new AgentException(INVALID_REQUEST, message);
    }

    public static AgentException runFailed(String agentName, Throwable cause) {
        return new AgentException(RUN_FAILED, "agent " + agentName + " run failed: " + cause.getMessage(), cause);
    }

    public static AgentException toolError(String toolName, Throwable cause) {
        return new AgentException(TOOL_ERROR, "tool " + toolName + " failed: " + cause.getMessage(), cause);
    }

    public static AgentException timeout(long timeoutMs) {
        return new AgentException(TIMEOUT, "agent run timeout (" + timeoutMs + "ms)");
    }
}