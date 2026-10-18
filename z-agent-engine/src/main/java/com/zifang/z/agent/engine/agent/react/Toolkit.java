package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.tool.core.Tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Toolkit for ReAct agent to manage and execute tools.
 * Provides tool registration, lookup, and execution infrastructure.
 */
public class Toolkit {

    private final Map<String, Tool> tools;
    private final Map<String, ToolExecutor> toolExecutors;

    public Toolkit() {
        this.tools = new HashMap<>();
        this.toolExecutors = new HashMap<>();
    }

    /**
     * Register a tool with its executor.
     */
    public void registerTool(Tool tool, ToolExecutor executor) {
        if (tool == null || tool.getName() == null) {
            return;
        }
        tools.put(tool.getName(), tool);
        if (executor != null) {
            toolExecutors.put(tool.getName(), executor);
        }
    }

    /**
     * Register a tool with default executor.
     */
    public void registerTool(Tool tool) {
        registerTool(tool, null);
    }

    /**
     * Get tool by name.
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }

    /**
     * Get executor for a tool.
     */
    public ToolExecutor getExecutor(String toolName) {
        return toolExecutors.get(toolName);
    }

    /**
     * Check if tool exists.
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * Get all registered tool names.
     */
    public List<String> getToolNames() {
        return new ArrayList<>(tools.keySet());
    }

    /**
     * Get all tools.
     */
    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * Generate tools description for system prompt.
     */
    public String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("你有一个工具集可以使用:\n");
        for (Tool tool : tools.values()) {
            sb.append(tool.toDesc());
        }
        return sb.toString();
    }

    /**
     * Execute a tool by name with arguments.
     */
    public ToolResult execute(String toolName, Map<String, Object> args) {
        ToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            return ToolResult.error("Tool executor not found: " + toolName);
        }
        try {
            Object result = executor.execute(toolName, args);
            // Executor may return a ToolResult directly (e.g., needsConfirmation, error)
            if (result instanceof ToolResult) {
                return (ToolResult) result;
            }
            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.error(e.getMessage());
        }
    }

    /**
     * Remove a tool.
     */
    public void removeTool(String name) {
        tools.remove(name);
        toolExecutors.remove(name);
    }

    /**
     * Clear all tools.
     */
    public void clear() {
        tools.clear();
        toolExecutors.clear();
    }

    /**
     * Get tool count.
     */
    public int size() {
        return tools.size();
    }

    /**
     * Tool executor interface.
     */
    public interface ToolExecutor {
        Object execute(String toolName, Map<String, Object> args) throws Exception;
    }

    /**
     * Tool execution result.
     */
    public static class ToolResult {
        private final boolean success;
        private final Object result;
        private final String error;
        private final boolean needsConfirmation;
        private final String confirmationMessage;

        private ToolResult(boolean success, Object result, String error,
                           boolean needsConfirmation, String confirmationMessage) {
            this.success = success;
            this.result = result;
            this.error = error;
            this.needsConfirmation = needsConfirmation;
            this.confirmationMessage = confirmationMessage;
        }

        public static ToolResult success(Object result) {
            return new ToolResult(true, result, null, false, null);
        }

        public static ToolResult error(String error) {
            return new ToolResult(false, null, error, false, null);
        }

        /**
         * Creates a result that requires user confirmation before execution.
         * The agent loop will be paused and control returned to the caller.
         */
        public static ToolResult needsConfirmation(String message) {
            return new ToolResult(false, null, null, true, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public Object getResult() {
            return result;
        }

        public String getError() {
            return error;
        }

        /**
         * Returns true if this result requires user confirmation before proceeding.
         */
        public boolean isNeedsConfirmation() {
            return needsConfirmation;
        }

        /**
         * Returns the confirmation message describing the pending action.
         */
        public String getConfirmationMessage() {
            return confirmationMessage;
        }

        @Override
        public String toString() {
            if (needsConfirmation) {
                return "WAIT_CONFIRM:" + confirmationMessage;
            }
            if (!success) {
                return "ERROR:" + error;
            }
            return "OK:" + (result == null ? "" : result.toString());
        }

        public String getResultAsString() {
            if (needsConfirmation) {
                return "WAIT_CONFIRM:" + confirmationMessage;
            }
            if (result == null) {
                return error != null ? error : "";
            }
            return result.toString();
        }
    }

    /**
     * Builder for creating toolkit with tools.
     */
    public static class Builder {
        private final Toolkit toolkit;

        public Builder() {
            toolkit = new Toolkit();
        }

        public Builder addTool(Tool tool, ToolExecutor executor) {
            toolkit.registerTool(tool, executor);
            return this;
        }

        public Builder addTool(Tool tool) {
            toolkit.registerTool(tool);
            return this;
        }

        public Toolkit build() {
            return toolkit;
        }
    }
}