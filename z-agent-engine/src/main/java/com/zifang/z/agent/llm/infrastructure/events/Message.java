package com.zifang.z.agent.llm.infrastructure.events;

import java.util.List;

/**
 * A single message in the conversation history.
 * Extends AgentMessage with additional convenience methods.
 */
public class Message extends AgentMessage {

    private List<ToolCallData> toolCalls;

    public Message(String role, String content) {
        super(role, content);
    }

    public static Message user(String content) {
        return new Message("user", content != null ? content : "");
    }

    public static Message system(String content) {
        return new Message("system", content != null ? content : "");
    }

    public static Message assistant(String content) {
        return new Message("assistant", content != null ? content : "");
    }

    public static Message tool(String content, String toolCallId) {
        return new Message("tool", content != null ? content : "");
    }

    /**
     * Returns the text content of this message — used when Message is used
     * as an AgentMessage and needs to expose text for LLM context building.
     */
    public String getTextContent() {
        return getContent();
    }

    public List<ToolCallData> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCallData> toolCalls) {
        this.toolCalls = toolCalls;
    }

    @Override
    public String toString() {
        return "Message{role='" + getRole() + "', content='" + (getContent() != null && getContent().length() > 80 ? getContent().substring(0, 80) + "..." : getContent()) + "'}";
    }
}
