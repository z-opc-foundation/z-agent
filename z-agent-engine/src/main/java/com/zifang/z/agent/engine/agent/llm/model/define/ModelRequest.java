package com.zifang.z.agent.engine.agent.llm.model.define;

import com.zifang.z.agent.engine.agent.llm.tool.core.ToolSchema;

import java.util.List;

/**
 * Model request containing messages, tools, and options.
 */
public class ModelRequest {

    private List<ModelMessage> messages;
    private List<ToolSchema> tools;
    private ModelOptions options;

    public ModelRequest() {
    }

    public List<ModelMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ModelMessage> messages) {
        this.messages = messages;
    }

    public List<ToolSchema> getTools() {
        return tools;
    }

    public void setTools(List<ToolSchema> tools) {
        this.tools = tools;
    }

    public ModelOptions getOptions() {
        return options;
    }

    public void setOptions(ModelOptions options) {
        this.options = options;
    }
}
