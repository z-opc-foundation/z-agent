package com.zifang.z.agent.llm.agentunit.mode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agent mode — defines a named configuration for the agent, including system prompt and tool set.
 */
public class AgentMode {

    private final String code;
    private final String label;
    private final String systemPrompt;
    private final List<String> toolNames;

    private AgentMode(String code, String label, String systemPrompt, List<String> toolNames) {
        this.code = code;
        this.label = label;
        this.systemPrompt = systemPrompt;
        this.toolNames = toolNames != null ? new ArrayList<>(toolNames) : Collections.<String>emptyList();
    }

    public static AgentMode of(String code, String label, String systemPrompt, List<String> toolNames) {
        return new AgentMode(code, label, systemPrompt, toolNames);
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public List<String> getToolNames() {
        return toolNames;
    }
}
