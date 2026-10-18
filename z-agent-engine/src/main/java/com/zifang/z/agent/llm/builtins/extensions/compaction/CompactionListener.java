package com.zifang.z.agent.llm.builtins.extensions.compaction;

/**
 * Compaction listener — receives compaction events.
 */
public interface CompactionListener {

    void onCompaction(CompactionEvent event);
}
