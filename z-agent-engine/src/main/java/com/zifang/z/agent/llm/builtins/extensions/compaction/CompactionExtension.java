package com.zifang.z.agent.llm.builtins.extensions.compaction;

import com.zifang.z.agent.llm.agent.extension.AgentExtension;
import com.zifang.z.agent.llm.agent.runtime.ReactAgent;
import com.zifang.z.agent.llm.builtins.extensions.session.SessionStateStore;

/**
 * Extension that provides automatic compaction of conversation context.
 */
public class CompactionExtension implements AgentExtension {

    private final CombinedLlmMessagesCompactor compactor;
    private final SessionStateStore stateStore;

    public CompactionExtension(CombinedLlmMessagesCompactor compactor, SessionStateStore stateStore) {
        this.compactor = compactor;
        this.stateStore = stateStore;
    }

    @Override
    public void install(ReactAgent agent) {
        agent.addContextBuilder(context -> compactor.compact(context.messages()));
    }
}
