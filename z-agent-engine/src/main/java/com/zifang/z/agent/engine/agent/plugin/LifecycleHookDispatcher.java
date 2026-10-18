package com.zifang.z.agent.engine.agent.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 生命周期钩子分发器。
 * 持有 PluginRegistry，按顺序调用所有已注册的 AgentLifecycleHook。
 */
public class LifecycleHookDispatcher {
    private final List<AgentLifecycleHook> hooks;

    public LifecycleHookDispatcher() {
        this.hooks = PluginRegistry.getInstance().getLifecycleHooks();
    }

    public LifecycleHookDispatcher(List<AgentLifecycleHook> hooks) {
        this.hooks = hooks != null ? hooks : new ArrayList<>();
    }

    public void dispatchCreated(Object agent) {
        for (AgentLifecycleHook h : hooks) h.onAgentCreated(agent);
    }

    public void dispatchStarted(String sessionId, String input) {
        for (AgentLifecycleHook h : hooks) h.onAgentStarted(sessionId, input);
    }

    public void dispatchStepStart(String sessionId, int step) {
        for (AgentLifecycleHook h : hooks) h.onStepStart(sessionId, step);
    }

    public void dispatchToolCall(String sessionId, String toolName, Map<String, Object> args) {
        for (AgentLifecycleHook h : hooks) h.onToolCall(sessionId, toolName, args);
    }

    public void dispatchToolResult(String sessionId, String toolName, String result, boolean success) {
        for (AgentLifecycleHook h : hooks) h.onToolResult(sessionId, toolName, result, success);
    }

    public void dispatchLlmCall(String sessionId, String model, int promptTokens) {
        for (AgentLifecycleHook h : hooks) h.onLlmCall(sessionId, model, promptTokens);
    }

    public void dispatchLlmResult(String sessionId, String model, int completionTokens, long elapsedMs) {
        for (AgentLifecycleHook h : hooks) h.onLlmResult(sessionId, model, completionTokens, elapsedMs);
    }

    public void dispatchFinished(String sessionId, String status, String finalAnswer) {
        for (AgentLifecycleHook h : hooks) h.onAgentFinished(sessionId, status, finalAnswer);
    }

    public void dispatchError(String sessionId, Throwable error) {
        for (AgentLifecycleHook h : hooks) h.onAgentError(sessionId, error);
    }
}