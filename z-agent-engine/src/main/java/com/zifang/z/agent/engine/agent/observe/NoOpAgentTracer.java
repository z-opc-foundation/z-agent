package com.zifang.z.agent.engine.agent.observe;

import com.zifang.z.agent.engine.agent.observe.model.AgentSpan;
import com.zifang.z.agent.engine.agent.observe.model.AgentTrace;

public class NoOpAgentTracer implements AgentTracer {
    public AgentTrace beginTrace(String s, String a, String u) {
        return null;
    }

    public AgentSpan beginSpan(AgentSpan p, String n) {
        return null;
    }

    public void endSpan(AgentSpan s, String st, String e) {
    }

    public void endTrace(AgentTrace t, String st) {
    }

    public AgentTrace currentTrace() {
        return null;
    }
}