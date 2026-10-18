package com.zifang.z.agent.engine.agent.observe;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.engine.agent.observe.model.AgentSpan;
import com.zifang.z.agent.engine.agent.observe.model.AgentTrace;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 基于 java.util.logging 的追踪器实现。
 * 输出 JSON 格式日志，可被 logstash 收集。
 */
public class LoggingAgentTracer implements AgentTracer {
    private static final Logger log = Logger.getLogger(LoggingAgentTracer.class.getName());
    private final ThreadLocal<AgentTrace> currentTrace = new ThreadLocal<>();

    @Override
    public AgentTrace beginTrace(String sessionId, String agentName, String userGoal) {
        AgentTrace trace = AgentTrace.create(sessionId, agentName, userGoal);
        currentTrace.set(trace);
        log.log(Level.INFO, toJson("TRACE_START", trace));
        return trace;
    }

    @Override
    public AgentSpan beginSpan(AgentSpan parent, String name) {
        AgentTrace trace = currentTrace.get();
        AgentSpan span = AgentSpan.create(trace != null ? trace.getTraceId() : null, name);
        if (parent != null) {
            span.parentSpanId(parent.getSpanId());
        }

        if (trace != null) {
            trace.addRootSpan(span);
        }

        log.log(Level.INFO, toJson("SPAN_START", span));
        return span;
    }

    @Override
    public void endSpan(AgentSpan span, String status, String errorMsg) {
        span.end();
        span.status(status);
        span.errorMsg(errorMsg);
        log.log(Level.INFO, toJson("SPAN_END", span));
    }

    @Override
    public void endTrace(AgentTrace trace, String status) {
        trace.end();
        trace.status(status);
        trace.buildSummary();
        log.log(Level.INFO, toJson("TRACE_END", trace));
        currentTrace.remove();
    }

    @Override
    public AgentTrace currentTrace() {
        return currentTrace.get();
    }

    private String toJson(String eventType, Object payload) {
        try {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("event", eventType);
            if (payload instanceof AgentTrace) {
                AgentTrace t = (AgentTrace) payload;
                map.put("traceId", t.getTraceId());
                map.put("sessionId", t.getSessionId());
                map.put("agentName", t.getAgentName());
                map.put("userGoal", t.getUserGoal());
                map.put("duration_ms", t.getTotalDurationMs());
                map.put("status", t.getStatus());
            } else if (payload instanceof AgentSpan) {
                AgentSpan s = (AgentSpan) payload;
                map.put("traceId", s.getTraceId());
                map.put("spanId", s.getSpanId());
                map.put("parentSpanId", s.getParentSpanId());
                map.put("name", s.getName());
                map.put("duration_ms", s.getDurationMs());
                map.put("status", s.getStatus());
                map.put("error", s.getErrorMsg());
            }
            return JsonUtil.toJsonExcludingNulls(map);
        } catch (Exception e) {
            return "{\"event\":\"" + eventType + "\",\"error\":\"json_serialize_failed\"}";
        }
    }
}