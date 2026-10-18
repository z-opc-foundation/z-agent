package com.zifang.z.agent.llm.infrastructure.events;

/**
 * Tool call data — describes a single tool call within a conversation.
 */
public class ToolCallData {

    private final String id;
    private final String name;
    private final String arguments;

    public ToolCallData(String id, String name, String arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArguments() {
        return arguments;
    }
}
