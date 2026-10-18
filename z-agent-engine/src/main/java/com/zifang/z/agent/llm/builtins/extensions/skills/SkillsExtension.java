package com.zifang.z.agent.llm.builtins.extensions.skills;

import com.zifang.z.agent.llm.agent.extension.AgentExtension;
import com.zifang.z.agent.llm.agent.runtime.ReactAgent;

/**
 * Skills extension — injects skill-related context into agent conversations.
 */
public class SkillsExtension implements AgentExtension {

    private final CatalogSkillRegistry registry;

    public SkillsExtension(CatalogSkillRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void install(ReactAgent agent) {
        // Skills context injection would happen here
    }
}
