package com.zifang.z.agent.engine.agent.llm.tool.core;

import java.util.Map;

/**
 * Tool call request for execution.
 */
public class ToolCallRequest {

    private String toolName;
    private Map<String, Object> parameters;

    public ToolCallRequest() {
    }

    public ToolCallRequest(String toolName, Map<String, Object> parameters) {
        this.toolName = toolName;
        this.parameters = parameters;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
