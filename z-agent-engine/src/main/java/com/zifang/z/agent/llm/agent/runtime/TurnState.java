package com.zifang.z.agent.llm.agent.runtime;

import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;

import java.util.List;

/**
 * Turn state — represents the state of a turn, including messages.
 */
public class TurnState {

    private final List<AgentMessage> messages;

    public TurnState(List<AgentMessage> messages) {
        this.messages = messages;
    }

    public List<AgentMessage> messages() {
        return messages;
    }
}
