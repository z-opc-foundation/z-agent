package com.zifang.z.agent.engine.agent.llm.tool.core;

import java.util.Map;

/**
 * 工具注册中心（单例，管理所有工具，贴合原项目 ToolRegistry）
 */
public class ToolRegistry {

    // 单例实例（JDK1.8 懒汉式，线程安全）
    private static volatile ToolRegistry instance;
    // 工具缓存（key：工具名，value：工具实例）
    private Map<String, NanoBotTool> toolMap;

    // 私有构造器，初始化默认工具（贴合原项目默认工具集）
    private ToolRegistry() {
        this.toolMap = new java.util.concurrent.ConcurrentHashMap<>();
        // 注册默认工具（示例：文件读取、命令执行，可扩展）
        this.toolMap.put("read_file", new ReadFileTool());
        this.toolMap.put("exec", new ExecTool());
    }

    // 单例获取方法
    public static ToolRegistry getInstance() {
        if (instance == null) {
            synchronized (ToolRegistry.class) {
                if (instance == null) {
                    instance = new ToolRegistry();
                }
            }
        }
        return instance;
    }

    // 注册工具
    public void registerTool(NanoBotTool tool) {
        if (tool != null && org.apache.commons.lang3.StringUtils.isNotBlank(tool.getToolName())) {
            toolMap.put(tool.getToolName(), tool);
        }
    }

    // 执行工具
    public String executeTool(String toolName, Map<String, Object> parameters) {
        NanoBotTool tool = toolMap.get(toolName);
        if (tool == null) {
            return "错误：未找到工具 [" + toolName + "]，可用工具：" + toolMap.keySet();
        }
        try {
            return tool.execute(parameters);
        } catch (Exception e) {
            return "工具 [" + toolName + "] 执行失败：" + e.getMessage();
        }
    }

    // 获取所有工具描述（用于拼接模型 prompt，告知模型可用工具）
    public String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用工具列表：\n");
        toolMap.values().forEach(tool -> {
            sb.append("- ").append(tool.getToolName()).append("：").append(tool.getToolDescription()).append("\n");
        });
        sb.append("工具调用格式（必须严格遵循）：{\"tool\":\"工具名\",\"parameters\":{\"参数名\":\"参数值\"}}");
        return sb.toString();
    }

    // 示例工具1：文件读取工具（贴合原项目 read_file）
    static class ReadFileTool implements NanoBotTool {
        @Override
        public String getToolName() {
            return "read_file";
        }

        @Override
        public String getToolDescription() {
            return "读取工作目录下的文件，参数：path（文件路径，相对工作目录）";
        }

        @Override
        public String execute(Map<String, Object> parameters) {
            String path = parameters.get("path") == null ? "" : parameters.get("path").toString();
            if (org.apache.commons.lang3.StringUtils.isBlank(path)) {
                return "参数错误：请提供文件路径（path）";
            }
            // 简化实现：实际需结合工作目录，处理文件读取逻辑（贴合原项目）
            return "文件 [" + path + "] 读取成功，内容：（模拟内容）Hello from nanobot file";
        }
    }

    // 示例工具2：命令执行工具（贴合原项目 exec）
    static class ExecTool implements NanoBotTool {
        @Override
        public String getToolName() {
            return "exec";
        }

        @Override
        public String getToolDescription() {
            return "执行系统命令，参数：command（命令内容），working_dir（可选，工作目录）";
        }

        @Override
        public String execute(Map<String, Object> parameters) {
            String command = parameters.get("command") == null ? "" : parameters.get("command").toString();
            if (org.apache.commons.lang3.StringUtils.isBlank(command)) {
                return "参数错误：请提供命令内容（command）";
            }
            // 简化实现：实际需处理命令执行、安全校验（贴合原项目安全逻辑）
            return "命令 [" + command + "] 执行成功，输出：（模拟输出）command executed successfully";
        }
    }
}
