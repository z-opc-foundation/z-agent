package com.zifang.z.agent.engine.agent.llm.tool.core;

import java.util.Map;

/**
 * 工具接口（贴合原项目 Tool 接口，定义工具统一规范）
 */
public interface NanoBotTool {

    // 工具唯一名称（如 "read_file"）
    String getToolName();

    // 工具描述（给模型看，用于意图识别）
    String getToolDescription();

    // 执行工具，返回执行结果
    String execute(Map<String, Object> parameters);
}
