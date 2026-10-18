package com.zifang.z.agent.engine.agent.observe;

import com.zifang.z.agent.engine.agent.observe.model.AgentSpan;
import com.zifang.z.agent.engine.agent.observe.model.AgentTrace;
import org.junit.Test;

import static org.junit.Assert.*;

public class AgentTracerTest {
    @Test
    public void testNoOpTracer_allMethodsNoop() {
        NoOpAgentTracer tracer = new NoOpAgentTracer();
        assertNull(tracer.beginTrace("s1", "a1", "goal"));
        assertNull(tracer.beginSpan(null, "span1"));
        tracer.endSpan(null, "SUCCESS", null);
        tracer.endTrace(null, "SUCCESS");
        assertNull(tracer.currentTrace());
    }

    @Test
    public void testAgentSpan_duration() throws InterruptedException {
        AgentSpan span = AgentSpan.create("t1", "test-span");
        Thread.sleep(50);
        span.end();
        assertTrue(span.getDurationMs() >= 50);
    }

    @Test
    public void testAgentTrace_buildSummary() {
        AgentTrace trace = AgentTrace.create("s1", "test-agent", "goal");
        trace.addRootSpan(AgentSpan.create(trace.getTraceId(), "llm_call"));
        trace.addRootSpan(AgentSpan.create(trace.getTraceId(), "tool_call"));
        trace.addRootSpan(AgentSpan.create(trace.getTraceId(), "tool_call"));
        trace.addRootSpan(AgentSpan.create(trace.getTraceId(), "llm_call"));
        java.util.Map<String, Object> summary = trace.buildSummary();
        assertEquals(2, summary.get("llm_calls"));
        assertEquals(2, summary.get("tool_calls"));
    }

    @Test
    public void testLoggingAgentTracer_noException() {
        LoggingAgentTracer tracer = new LoggingAgentTracer();
        AgentTrace t = tracer.beginTrace("s1", "agent", "goal");
        assertNotNull(t);
        AgentSpan s = tracer.beginSpan(null, "step_1");
        assertNotNull(s);
        tracer.endSpan(s, "SUCCESS", null);
        tracer.endTrace(t, "SUCCESS");
    }
}