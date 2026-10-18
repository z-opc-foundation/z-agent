package com.zifang.z.agent.llm.infrastructure.events;

/**
 * Agent-specific message with additional metadata.
 */
public class AgentMessage {

    private final String role;
    private final String content;
    private final String toolName;
    private final String toolCallId;
    private final long timestamp;

    protected AgentMessage(String role, String content) {
        this(role, content, null, null);
    }

    protected AgentMessage(String role, String content, String toolName, String toolCallId) {
        this.role = role;
        this.content = content;
        this.toolName = toolName;
        this.toolCallId = toolCallId;
        this.timestamp = System.currentTimeMillis();
    }

    public static AgentMessage user(String content) {
        return new AgentMessage("user", content, null, null);
    }

    public static AgentMessage system(String content) {
        return new AgentMessage("system", content, null, null);
    }

    public static AgentMessage assistant(String content) {
        return new AgentMessage("assistant", content, null, null);
    }

    public static AgentMessage tool(String content, String toolName, String toolCallId) {
        return new AgentMessage("tool", content, toolName, toolCallId);
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
