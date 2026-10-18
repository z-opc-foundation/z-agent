package com.zifang.z.agent.llm.agent.runtime;

/**
 * Agent context — runtime context for an agent unit, including session, mode, and identity.
 */
public class AgentContext {

    private final String unitId;
    private final String sessionId;
    private String mode;

    public AgentContext(String unitId, String sessionId, String mode) {
        this.unitId = unitId;
        this.sessionId = sessionId;
        this.mode = mode;
    }

    public String unitId() {
        return unitId;
    }

    public String sessionId() {
        return sessionId;
    }

    public String mode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
