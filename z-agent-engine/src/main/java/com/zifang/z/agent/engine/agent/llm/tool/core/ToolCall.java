package com.zifang.z.agent.engine.agent.llm.tool.core;

import java.util.Map;

/**
 * Model returned tool call instruction.
 */
public class ToolCall {

    private String toolName;
    private Map<String, Object> args;

    public ToolCall() {
    }

    public ToolCall(String toolName, Map<String, Object> args) {
        this.toolName = toolName;
        this.args = args;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public void setArgs(Map<String, Object> args) {
        this.args = args;
    }
}
