package com.zifang.z.agent.engine.agent.llm.provider;

import com.zifang.z.agent.engine.agent.llm.model.define.ModelMessage;

import java.util.List;

/**
 * LLM caller request with model, messages, and tool definitions.
 */
public class LlmCallerRequest {

    private String model;
    private String format = "json";
    private boolean stream = false;
    private String toolChoice = "auto";
    private List<ToolDefinition> tools;
    private List<ModelMessage> modelMessages;

    public LlmCallerRequest() {
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public String getToolChoice() {
        return toolChoice;
    }

    public void setToolChoice(String toolChoice) {
        this.toolChoice = toolChoice;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinition> tools) {
        this.tools = tools;
    }

    public List<ModelMessage> getModelMessages() {
        return modelMessages;
    }

    public void setModelMessages(List<ModelMessage> modelMessages) {
        this.modelMessages = modelMessages;
    }

    /**
     * Tool definition for function calling.
     */
    public static class ToolDefinition {
        private String type = "function";
        private FunctionDefinition function;

        public ToolDefinition() {
        }

        public ToolDefinition(String type, FunctionDefinition function) {
            this.type = type;
            this.function = function;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public FunctionDefinition getFunction() {
            return function;
        }

        public void setFunction(FunctionDefinition function) {
            this.function = function;
        }
    }

    /**
     * Function definition within a tool.
     */
    public static class FunctionDefinition {
        private String name;
        private String description;
        private Object parameters;

        public FunctionDefinition() {
        }

        public FunctionDefinition(String name, String description, Object parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Object getParameters() {
            return parameters;
        }

        public void setParameters(Object parameters) {
            this.parameters = parameters;
        }
    }
}
