package com.zifang.z.agent.llm.builtins.extensions.support;

import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;

import java.util.List;

/**
 * Listener for compaction observations.
 */
public interface CompactionObservationListener {

    void onCompaction(List<AgentMessage> original, List<AgentMessage> compacted);
}
