package com.zifang.z.agent.engine.agent.llm.model;

/**
 * Represents a tool call in OpenAI-compatible format.
 */
public class ToolCall {

    private Integer index;
    private String id;
    private String type = "function";
    private FunctionCall function;

    public ToolCall() {
    }

    public ToolCall(String id, String name, String arguments) {
        this.id = id;
        this.type = "function";
        this.function = new FunctionCall(name, arguments);
    }

    public ToolCall(Integer index, String id, String name, String arguments) {
        this.index = index;
        this.id = id;
        this.type = "function";
        this.function = new FunctionCall(name, arguments);
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
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

    public FunctionCall getFunction() {
        return function;
    }

    public void setFunction(FunctionCall function) {
        this.function = function;
    }

    public static class FunctionCall {
        private String name;
        private String arguments;

        public FunctionCall() {
        }

        public FunctionCall(String name, String arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
}
