package com.zifang.z.agent.llm.agent.hooks;

import com.zifang.z.agent.llm.agent.runtime.TurnContext;
import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;

import java.util.List;

/**
 * Hook types and extension points for the agent runtime.
 */
public class HookTypes {

    /**
     * Context builder — enriches the message list before each LLM call.
     */
    public interface ContextBuilder {
        List<AgentMessage> build(TurnContext context);
    }
}
