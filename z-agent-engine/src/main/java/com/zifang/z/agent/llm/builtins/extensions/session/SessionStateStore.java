package com.zifang.z.agent.llm.builtins.extensions.session;

import com.zifang.z.agent.llm.agent.runtime.TurnState;
import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;
import com.zifang.z.agent.llm.infrastructure.llm.model.ThinkingLevel;

import java.util.List;

/**
 * Session state store — interface for persisting and restoring agent session state.
 */
public interface SessionStateStore {

    TurnState loadTurnState();

    Long appendMessage(AgentMessage message);

    Long appendCompaction(String summary, Long firstKeptEntryId);

    Long appendModelChange(String modelCode);

    Long appendThinkingLevelChange(ThinkingLevel level);

    Long appendToolChange(List<String> toolNames);

    Long appendBranchSummary(String summary, Long parentLeafId);

    Long getLeafId();

    void updateLeafId(Long leafId);

    void replaceMessages(List<AgentMessage> messages);
}
