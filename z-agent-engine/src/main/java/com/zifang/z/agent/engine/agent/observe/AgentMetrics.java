package com.zifang.z.agent.engine.agent.observe;

/**
 * 指标接口。引擎定义，平台层实现（可接 Prometheus/Micrometer）
 */
public interface AgentMetrics {
    default void incAgentExecutions(String agentName, String status) {
    }

    default void incToolCalls(String toolName, String status) {
    }

    default void recordTokens(String model, int promptTokens, int completionTokens) {
    }

    default void recordToolDuration(String toolName, long durationMs) {
    }

    default void recordLlmDuration(String model, long durationMs) {
    }

    default void record(String metricName, double value, String... tags) {
    }
}