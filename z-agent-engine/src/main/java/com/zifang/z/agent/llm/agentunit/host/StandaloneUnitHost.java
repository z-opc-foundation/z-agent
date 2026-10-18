package com.zifang.z.agent.llm.agentunit.host;

import com.zifang.z.agent.llm.agentunit.AgentUnit;

/**
 * Standalone agent unit host — manages AgentUnit instances without external dependencies.
 */
public class StandaloneUnitHost {

    private AgentUnit unit;

    public StandaloneUnitHost() {
    }

    public AgentUnit getUnit() {
        return unit;
    }

    public void setUnit(AgentUnit unit) {
        this.unit = unit;
    }

    public AgentUnit agent() {
        return unit;
    }
}
