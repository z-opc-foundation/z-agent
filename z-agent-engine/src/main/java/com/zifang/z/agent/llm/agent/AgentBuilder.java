package com.zifang.z.agent.llm.agent;

import com.zifang.z.agent.llm.agent.runtime.ReactAgent;
import com.zifang.z.agent.kernel.tool.BaseTool;
import com.zifang.z.agent.llm.infrastructure.llm.context.LlmStreamRequest;
import com.zifang.z.agent.llm.infrastructure.llm.engine.LlmReactEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for constructing {@link ReactAgent} instances.
 */
public class AgentBuilder {

    private String name;
    private String systemPrompt;
    private String id;
    private String sessionId;
    private String llmCode;
    private LlmStreamRequest.StreamOptions defaultStreamOptions;
    private LlmReactEngine llmEngine;
    private List<BaseTool> tools;

    public AgentBuilder() {
        this.tools = new ArrayList<>();
    }

    public AgentBuilder name(String name) {
        this.name = name;
        return this;
    }

    public AgentBuilder systemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public AgentBuilder id(String id) {
        this.id = id;
        return this;
    }

    public AgentBuilder sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public AgentBuilder llmCode(String llmCode) {
        this.llmCode = llmCode;
        return this;
    }

    public AgentBuilder defaultStreamOptions(LlmStreamRequest.StreamOptions options) {
        this.defaultStreamOptions = options;
        return this;
    }

    public AgentBuilder llmEngine(LlmReactEngine llmEngine) {
        this.llmEngine = llmEngine;
        return this;
    }

    public AgentBuilder tools(List<BaseTool> tools) {
        this.tools = tools != null ? tools : new ArrayList<BaseTool>();
        return this;
    }

    public AgentBuilder addTool(BaseTool tool) {
        if (tool != null) {
            this.tools.add(tool);
        }
        return this;
    }

    public ReactAgent build() {
        LlmReactEngine engine = this.llmEngine != null ? this.llmEngine : new LlmReactEngine(llmCode);
        LlmStreamRequest.StreamOptions opts = this.defaultStreamOptions != null
                ? this.defaultStreamOptions
                : new LlmStreamRequest.StreamOptions();
        return new ReactAgent(name, systemPrompt, id, sessionId, llmCode, opts, engine, tools);
    }
}
