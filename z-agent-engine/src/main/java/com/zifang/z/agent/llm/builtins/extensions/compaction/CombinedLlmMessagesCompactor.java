package com.zifang.z.agent.llm.builtins.extensions.compaction;

import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines LLM messages with compaction — summarizes older messages while keeping recent ones intact.
 */
public class CombinedLlmMessagesCompactor {

    private final ContextLimits limits;
    private final Object archivist;
    private final CompactionListener listener;

    public CombinedLlmMessagesCompactor(ContextLimits limits, Object archivist,
                                        CompactionListener listener) {
        this.limits = limits;
        this.archivist = archivist;
        this.listener = listener;
    }

    public List<AgentMessage> compact(List<AgentMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        // Stub: in production, would implement smart compaction logic
        return new ArrayList<>(messages);
    }
}
