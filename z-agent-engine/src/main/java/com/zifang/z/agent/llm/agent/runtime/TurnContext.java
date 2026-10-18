package com.zifang.z.agent.llm.agent.runtime;

import com.zifang.z.agent.llm.agent.observability.TurnResult;
import com.zifang.z.agent.llm.agent.turn.TurnRun;
import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;

import java.util.List;

/**
 * Turn context — provides contextual information for a turn, including messages, run state, and cancellation status.
 */
public class TurnContext {

    private final List<AgentMessage> messages;
    private final TurnRun run;
    private final TurnResult lastTurnResult;
    private final boolean cancelled;
    // ---- Phase 7 extensions ----
    private String agentName;
    private AgentStream agentStream;
    private String agentId;

    public TurnContext(List<AgentMessage> messages, TurnRun run, TurnResult lastTurnResult, boolean cancelled) {
        this.messages = messages;
        this.run = run;
        this.lastTurnResult = lastTurnResult;
        this.cancelled = cancelled;
    }

    public List<AgentMessage> messages() {
        return messages;
    }

    public TurnRun run() {
        return run;
    }

    public TurnResult lastTurnResult() {
        return lastTurnResult;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public String agentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public AgentStream stream() {
        return agentStream;
    }

    public void setStream(AgentStream stream) {
        this.agentStream = stream;
    }

    public String agentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
}
