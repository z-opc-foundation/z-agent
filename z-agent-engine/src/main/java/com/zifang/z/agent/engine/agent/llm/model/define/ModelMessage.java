package com.zifang.z.agent.engine.agent.llm.model.define;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model message for conversation history.
 * Supports roles: user, assistant, tool, system.
 */
public class ModelMessage {

    private String role;
    private String content;
    private LocalDateTime timestamp;
    private List<ToolCall> toolCalls;

    public ModelMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ModelMessage(String role, String content) {
        this.role = role;
        this.content = StringUtils.trimToEmpty(content);
        this.timestamp = LocalDateTime.now();
    }

    public static ModelMessage of(String role, String content) {
        ModelMessage modelMessage = new ModelMessage();
        modelMessage.setRole(role);
        modelMessage.setContent(content);
        modelMessage.setTimestamp(LocalDateTime.now());
        return modelMessage;
    }

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    /**
     * Tool call definition from model.
     */
    public static class ToolCall {
        private String id;
        private String type;
        private Function function;

        public ToolCall() {
        }

        public ToolCall(String id, String type, Function function) {
            this.id = id;
            this.type = type;
            this.function = function;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Function getFunction() {
            return function;
        }

        public void setFunction(Function function) {
            this.function = function;
        }
    }

    /**
     * Function details for tool call.
     */
    public static class Function {
        private String name;
        private Object arguments;

        public Function() {
        }

        public Function(String name, Object arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getArguments() {
            return arguments;
        }

        public void setArguments(Object arguments) {
            this.arguments = arguments;
        }
    }
}
