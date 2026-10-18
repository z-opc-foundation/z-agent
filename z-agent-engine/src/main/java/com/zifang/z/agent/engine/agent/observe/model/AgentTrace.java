package com.zifang.z.agent.engine.agent.observe.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次 Agent 执行的完整追踪
 */
public class AgentTrace implements Serializable {
    String traceId;
    String sessionId;
    String agentName;
    String userGoal;
    long startTimeMs;
    long endTimeMs;
    String status;       // SUCCESS / FAILURE / TIMEOUT
    List<AgentSpan> rootSpans = new ArrayList<>();

    public static String generateTraceId() {
        return "trace-" + System.currentTimeMillis() + "-" + System.nanoTime();
    }

    public static AgentTrace create(String sessionId, String agentName, String userGoal) {
        AgentTrace t = new AgentTrace();
        t.traceId = generateTraceId();
        t.sessionId = sessionId;
        t.agentName = agentName;
        t.userGoal = userGoal;
        t.startTimeMs = System.currentTimeMillis();
        return t;
    }

    public void end() {
        this.endTimeMs = System.currentTimeMillis();
    }

    public void addRootSpan(AgentSpan span) {
        rootSpans.add(span);
    }

    public long getTotalDurationMs() {
        return endTimeMs - startTimeMs;
    }

    public Map<String, Object> buildSummary() {
        Map<String, Object> summary = new HashMap<>();
        int toolCalls = 0, llmCalls = 0;
        for (AgentSpan s : rootSpans) {
            if ("tool_call".equals(s.name)) {
                toolCalls++;
            }
            if ("llm_call".equals(s.name)) {
                llmCalls++;
            }
        }
        summary.put("tool_calls", toolCalls);
        summary.put("llm_calls", llmCalls);
        summary.put("total_duration_ms", getTotalDurationMs());
        summary.put("status", status);
        return summary;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getUserGoal() {
        return userGoal;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public List<AgentSpan> getRootSpans() {
        return rootSpans;
    }

    public AgentTrace status(String status) {
        this.status = status;
        return this;
    }
}