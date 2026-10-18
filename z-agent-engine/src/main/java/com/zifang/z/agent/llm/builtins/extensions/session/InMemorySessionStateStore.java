package com.zifang.z.agent.llm.builtins.extensions.session;

import com.zifang.z.agent.llm.agent.runtime.TurnState;
import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;
import com.zifang.z.agent.llm.infrastructure.llm.model.ThinkingLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory session state store — default implementation of SessionStateStore.
 */
public class InMemorySessionStateStore implements SessionStateStore {

    private final List<AgentMessage> messages = new ArrayList<>();
    private Long leafId = 0L;

    @Override
    public TurnState loadTurnState() {
        synchronized (messages) {
            return new TurnState(new ArrayList<>(messages));
        }
    }

    @Override
    public Long appendMessage(AgentMessage message) {
        synchronized (messages) {
            messages.add(message);
            leafId++;
            return leafId;
        }
    }

    @Override
    public Long appendCompaction(String summary, Long firstKeptEntryId) {
        synchronized (messages) {
            leafId++;
            return leafId;
        }
    }

    @Override
    public Long appendModelChange(String modelCode) {
        synchronized (messages) {
            leafId++;
            return leafId;
        }
    }

    @Override
    public Long appendThinkingLevelChange(ThinkingLevel level) {
        synchronized (messages) {
            leafId++;
            return leafId;
        }
    }

    @Override
    public Long appendToolChange(List<String> toolNames) {
        synchronized (messages) {
            leafId++;
            return leafId;
        }
    }

    @Override
    public Long appendBranchSummary(String summary, Long parentLeafId) {
        synchronized (messages) {
            leafId++;
            return leafId;
        }
    }

    @Override
    public Long getLeafId() {
        synchronized (messages) {
            return leafId;
        }
    }

    @Override
    public void updateLeafId(Long leafId) {
        synchronized (messages) {
            this.leafId = leafId;
        }
    }

    @Override
    public void replaceMessages(List<AgentMessage> newMessages) {
        synchronized (messages) {
            messages.clear();
            if (newMessages != null) {
                messages.addAll(newMessages);
            }
        }
    }
}
