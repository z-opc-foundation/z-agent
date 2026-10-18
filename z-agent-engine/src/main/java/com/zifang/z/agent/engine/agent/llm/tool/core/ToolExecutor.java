package com.zifang.z.agent.engine.agent.llm.tool.core;

import java.util.Map;

/**
 * Functional interface for tool execution.
 */
@FunctionalInterface
public interface ToolExecutor {
    String execute(Map<String, Object> args);
}