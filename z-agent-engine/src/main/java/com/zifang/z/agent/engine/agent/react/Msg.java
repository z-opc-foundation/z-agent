package com.zifang.z.agent.engine.agent.react;

import java.time.LocalDateTime;

/**
 * Message object for ReAct agent conversation.
 * Represents a single message in the conversation history.
 */
public class Msg {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_TOOL = "tool";

    public static final String CONTENT_TYPE_TEXT = "text";
    public static final String CONTENT_TYPE_TOOL_CALL = "tool_call";
    public static final String CONTENT_TYPE_TOOL_RESULT = "tool_result";

    private String role;
    private String content;
    private String contentType;
    private String toolName;
    private String toolCallId;
    private LocalDateTime timestamp;
    private boolean isFinal;

    public Msg() {
        this.timestamp = LocalDateTime.now();
        this.contentType = CONTENT_TYPE_TEXT;
        this.isFinal = false;
    }

    public Msg(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }

    public static Msg user(String content) {
        return new Msg(ROLE_USER, content);
    }

    public static Msg assistant(String content) {
        return new Msg(ROLE_ASSISTANT, content);
    }

    public static Msg system(String content) {
        return new Msg(ROLE_SYSTEM, content);
    }

    public static Msg toolResult(String toolCallId, String toolName, String content) {
        Msg msg = new Msg(ROLE_TOOL, content);
        msg.setToolCallId(toolCallId);
        msg.setToolName(toolName);
        msg.setContentType(CONTENT_TYPE_TOOL_RESULT);
        return msg;
    }

    public static Msg assistantToolCall(String toolCallId, String toolName, String args) {
        Msg msg = new Msg(ROLE_ASSISTANT, args); // content = pure args JSON
        msg.setToolCallId(toolCallId);
        msg.setToolName(toolName);
        msg.setContentType(CONTENT_TYPE_TOOL_CALL);
        return msg;
    }

    public Msg withFinal(boolean isFinal) {
        this.isFinal = isFinal;
        return this;
    }

    // Getters and Setters

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    @Override
    public String toString() {
        return "Msg{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                ", contentType='" + contentType + '\'' +
                ", toolName='" + toolName + '\'' +
                ", toolCallId='" + toolCallId + '\'' +
                ", isFinal=" + isFinal +
                '}';
    }
}