package com.zifang.z.agent.engine.agent.guardrails;

import java.util.ArrayList;
import java.util.List;

/**
 * Guardrails 防护上下文。
 * 记录一次 Agent 执行中所有的防护事件（校验失败、注入拦截、脱敏等）。
 * 用于审计和安全分析。
 */
public class GuardrailsContext {
    private final List<GuardrailsEvent> events = new ArrayList<>();
    private volatile boolean blocked = false;

    public void recordValidationFailure(String toolName, String param, String reason) {
        events.add(new GuardrailsEvent("VALIDATION_FAILURE", toolName, "param=" + param, reason));
    }

    public void recordInjectionBlocked(String toolName, String pattern) {
        events.add(new GuardrailsEvent("INJECTION_BLOCKED", toolName, null, pattern));
        blocked = true;
    }

    public void recordSanitization(String toolName, String field, int redactions) {
        events.add(new GuardrailsEvent("SANITIZATION", toolName, "redactions=" + redactions, field));
    }

    public boolean isBlocked() {
        return blocked;
    }

    public List<GuardrailsEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public static class GuardrailsEvent {
        public final String type;
        public final String toolName;
        public final String detail;
        public final String reason;
        public final long timestamp = System.currentTimeMillis();

        public GuardrailsEvent(String type, String toolName, String detail, String reason) {
            this.type = type;
            this.toolName = toolName;
            this.detail = detail;
            this.reason = reason;
        }
    }
}