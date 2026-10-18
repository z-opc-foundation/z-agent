package com.zifang.z.agent.llm.agent.runtime;

/**
 * Agent event context — used by z-team extensions.
 */
public class AgentEventContext {

    private String sessionId;
    private int step;

    public AgentEventContext() {
    }

    public AgentEventContext(String sessionId, int step) {
        this.sessionId = sessionId;
        this.step = step;
    }

    /**
     * Two-string constructor used by CompactionExtension: (agentName, agentId)
     */
    public AgentEventContext(String agentName, String agentId) {
        this.sessionId = agentName;
        this.step = 0;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
