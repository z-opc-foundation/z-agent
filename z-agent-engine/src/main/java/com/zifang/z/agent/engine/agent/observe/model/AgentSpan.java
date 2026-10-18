package com.zifang.z.agent.engine.agent.observe.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个操作单元的追踪 span
 */
public class AgentSpan implements Serializable {
    String traceId;
    String spanId;
    String parentSpanId;
    String name;
    long startTimeMs;
    long endTimeMs;
    String status;       // SUCCESS / FAILURE
    String errorMsg;
    Map<String, Object> attributes = new HashMap<>();
    List<AgentSpan> children = new ArrayList<>();

    public static String generateId() {
        return "span-" + System.nanoTime();
    }

    public static AgentSpan create(String traceId, String name) {
        AgentSpan span = new AgentSpan();
        span.traceId = traceId;
        span.name = name;
        span.spanId = generateId();
        span.startTimeMs = System.currentTimeMillis();
        return span;
    }

    public void end() {
        this.endTimeMs = System.currentTimeMillis();
    }

    public void setAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public void addChild(AgentSpan child) {
        this.children.add(child);
    }

    public long getDurationMs() {
        return endTimeMs > 0 ? endTimeMs - startTimeMs : 0;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public String getName() {
        return name;
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

    public String getErrorMsg() {
        return errorMsg;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public List<AgentSpan> getChildren() {
        return children;
    }

    public AgentSpan status(String status) {
        this.status = status;
        return this;
    }

    public AgentSpan errorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }

    public AgentSpan parentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
        return this;
    }
}