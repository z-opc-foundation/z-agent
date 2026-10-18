package com.zifang.z.agent.engine.agent.plugin;


/**
 * Agent 生命周期钩子接口。
 * 插件在 Agent 执行的关键节点收到回调。
 */
public interface AgentLifecycleHook {
    /**
     * Agent 初始化完成后（Builder.build() 时触发）
     */
    default void onAgentCreated(Object agent) {
    }

    /**
     * Agent 开始执行（stream() 被订阅时）
     */
    default void onAgentStarted(String sessionId, String input) {
    }

    /**
     * 每一轮 ReAct 步骤开始前
     */
    default void onStepStart(String sessionId, int step) {
    }

    /**
     * Tool 调用前（参数已解析，尚未执行）
     */
    default void onToolCall(String sessionId, String toolName, java.util.Map<String, Object> args) {
    }

    /**
     * Tool 调用完成后（无论成功失败）
     */
    default void onToolResult(String sessionId, String toolName, String result, boolean success) {
    }

    /**
     * LLM 调用前
     */
    default void onLlmCall(String sessionId, String model, int promptTokens) {
    }

    /**
     * LLM 调用完成后
     */
    default void onLlmResult(String sessionId, String model, int completionTokens, long elapsedMs) {
    }

    /**
     * Agent 执行完成（正常/超时/错误）
     */
    default void onAgentFinished(String sessionId, String status, String finalAnswer) {
    }

    /**
     * Agent 执行异常（未捕获的异常）
     */
    default void onAgentError(String sessionId, Throwable error) {
    }
}