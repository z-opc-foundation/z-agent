package com.zifang.z.agent.llm.agent.extension;

import com.zifang.z.agent.llm.agent.runtime.ReactAgent;

/**
 * Extension interface — allows plugins to register tools or modify agent behavior.
 */
public interface AgentExtension {

    /**
     * Install this extension into the given agent.
     */
    void install(ReactAgent agent);
}
