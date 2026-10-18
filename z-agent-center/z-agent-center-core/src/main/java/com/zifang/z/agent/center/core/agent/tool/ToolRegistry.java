package com.zifang.z.agent.center.core.agent.tool;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.engine.agent.llm.tool.core.NanoBotTool;
import com.zifang.z.mcp.api.dto.CallToolResult;
import com.zifang.z.mcp.api.dto.ContentBlock;
import com.zifang.z.mcp.api.dto.McpToolDto;
import com.zifang.z.mcp.core.registry.McpRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Global Tool Registry
 * <p>
 * 统一工具注册中心，支持:
 * 1. 代码注入工具注册 (Code Injection)
 * 2. MCP 工具自动发现与同步
 * 3. 内置工具 (Engine NanoBotTool)
 * 4. 第三方工具
 * <p>
 * 线程安全单例
 */
public class ToolRegistry {

    private static final Logger log = LogManager.getLogger(ToolRegistry.class);
    private static volatile ToolRegistry instance;

    // 工具缓存: toolName -> ToolManifest
    private final Map<String, ToolManifest> toolManifestMap = new ConcurrentHashMap<>();

    // 工具执行器映射: toolName -> NanoBotTool (Engine 内置工具)
    private final Map<String, NanoBotTool> engineToolMap = new ConcurrentHashMap<>();
    // 变更监听器
    private final List<Consumer<ToolManifest>> changeListeners = new ArrayList<>();
    // MCP Registry 引用 (可选，用于同步 MCP 工具)
    private McpRegistry mcpRegistry;

    private ToolRegistry() {
        // 注册默认内置工具
        registerDefaultEngineTools();
    }

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

    /**
     * 初始化并绑定 MCP Registry
     */
    public void bindMcpRegistry(McpRegistry mcpRegistry) {
        this.mcpRegistry = mcpRegistry;
        // 立即同步一次
        syncFromMcp();
    }

    /**
     * 注册代码注入工具 (Code Injection)
     */
    public void registerCodeInjectionTool(ToolManifest manifest, NanoBotTool executor) {
        if (manifest == null || manifest.getToolName() == null) {
            log.warn("Cannot register code injection tool: null manifest or toolName");
            return;
        }
        manifest.setType("CODE_INJECTION");
        manifest.setSource("code-injection");
        manifest.setUpdateTime(System.currentTimeMillis());

        toolManifestMap.put(manifest.getToolName(), manifest);
        if (executor != null) {
            engineToolMap.put(manifest.getToolName(), executor);
        }

        log.info("Registered code injection tool: {}", manifest.getToolName());
        notifyListeners(manifest);
    }

    /**
     * 注册内置工具 (Engine NanoBotTool)
     */
    public void registerEngineTool(NanoBotTool tool) {
        if (tool == null || tool.getToolName() == null) return;


        ToolManifest manifest = ToolManifest.fromNanoBotTool(tool);
        manifest.setType("BUILT_IN");
        manifest.setSource("engine");
        manifest.setUpdateTime(System.currentTimeMillis());

        toolManifestMap.put(tool.getToolName(), manifest);
        engineToolMap.put(tool.getToolName(), tool);

        log.debug("Registered engine tool: {}", tool.getToolName());
    }

    /**
     * 注册第三方工具
     */
    public void registerThirdPartyTool(ToolManifest manifest) {
        if (manifest == null || manifest.getToolName() == null) return;

        manifest.setType("THIRD_PARTY");
        manifest.setUpdateTime(System.currentTimeMillis());
        toolManifestMap.put(manifest.getToolName(), manifest);
        log.info("Registered third-party tool: {}", manifest.getToolName());
        notifyListeners(manifest);
    }

    /**
     * 从 MCP Registry 同步工具
     */
    public void syncFromMcp() {
        if (mcpRegistry == null) {
            log.debug("MCP Registry not bound, skip sync");
            return;
        }

        List<McpToolDto> mcpTools = mcpRegistry.listTools();
        int synced = 0;

        for (McpToolDto dto : mcpTools) {
            ToolManifest manifest = ToolManifest.fromMcp(dto);
            if (manifest != null) {
                // 避免覆盖已有工具（优先 code injection）
                String toolName = dto.getName();
                ToolManifest existing = toolManifestMap.get(toolName);
                if (existing == null || "BUILT_IN".equals(existing.getType())) {
                    toolManifestMap.put(toolName, manifest);
                    synced++;
                }
            }
        }

        log.info("Synced {} tools from MCP Registry (total tools: {})", synced, toolManifestMap.size());
    }

    /**
     * 获取所有工具 Manifest
     */
    public List<ToolManifest> getAllTools() {
        return new ArrayList<>(toolManifestMap.values());
    }

    /**
     * 获取所有工具 Manifest (仅启用)
     */
    public List<ToolManifest> getEnabledTools() {
        List<ToolManifest> enabled = new ArrayList<>();
        for (ToolManifest m : toolManifestMap.values()) {
            if (m.isEnabled()) {
                enabled.add(m);
            }
        }
        return enabled;
    }

    /**
     * 获取工具 Manifest
     */
    public ToolManifest getTool(String toolName) {
        return toolManifestMap.get(toolName);
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String toolName) {
        return toolManifestMap.containsKey(toolName);
    }

    /**
     * 执行工具 (优先 Engine 内置工具)
     */
    public String executeTool(String toolName, Map<String, Object> parameters) {
        // 1. 优先使用 Engine 内置工具
        NanoBotTool engineTool = engineToolMap.get(toolName);
        if (engineTool != null) {
            try {
                return engineTool.execute(parameters);
            } catch (Exception e) {
                log.error("Engine tool '{}' execution failed: {}", toolName, e.getMessage());
                return "Tool '" + toolName + "' execution failed: " + e.getMessage();
            }
        }

        // 2. MCP 工具交给 z-mcp Registry 执行 (外部 server 的工具由 ExternalServerManager 挂进目录)
        if (mcpRegistry != null && mcpRegistry.hasTool(toolName)) {
            try {
                Object result = mcpRegistry.call(toolName, parameters);
                return result instanceof CallToolResult
                        ? renderMcpResult((CallToolResult) result)
                        : String.valueOf(result);
            } catch (Exception e) {
                log.error("Failed to execute MCP tool '{}': {}", toolName, e.getMessage());
                return "Error executing MCP tool: " + e.getMessage();
            }
        }

        // 3. 未找到工具
        List<String> available = new ArrayList<>(toolManifestMap.keySet());
        return "Error: Tool '" + toolName + "' not found. Available tools: " + available;
    }

    /**
     * 把 MCP 调用结果收敛成给模型看的文本: 优先正文 content blocks，没有正文就退回线格式.
     */
    private String renderMcpResult(CallToolResult result) {
        StringBuilder text = new StringBuilder();
        for (ContentBlock block : result.getContent()) {
            if (block instanceof ContentBlock.Text) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(((ContentBlock.Text) block).getText());
            }
        }

        String body;
        if (text.length() > 0) {
            body = text.toString();
        } else {
            try {
                body = JsonUtil.toJson(result.toWire());
            } catch (Exception e) {
                body = "result could not be rendered: " + e.getMessage();
            }
        }
        return result.isError() ? "Error: " + body : body;
    }

    /**
     * 注销工具
     */
    public boolean unregisterTool(String toolName) {
        ToolManifest removed = toolManifestMap.remove(toolName);
        engineToolMap.remove(toolName);
        if (removed != null) {
            log.info("Unregistered tool: {}", toolName);
            return true;
        }
        return false;
    }

    /**
     * 启用/禁用工具
     */
    public void setEnabled(String toolName, boolean enabled) {
        ToolManifest manifest = toolManifestMap.get(toolName);
        if (manifest != null) {
            manifest.setEnabled(enabled);
            manifest.setUpdateTime(System.currentTimeMillis());
            log.info("Tool '{}' enabled={}", toolName, enabled);
        }
    }

    /**
     * 获取工具数量
     */
    public int size() {
        return toolManifestMap.size();
    }

    /**
     * 清空所有工具
     */
    public void clear() {
        toolManifestMap.clear();
        engineToolMap.clear();
        log.info("Tool registry cleared");
    }

    /**
     * 添加变更监听器
     */
    public void addChangeListener(Consumer<ToolManifest> listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    /**
     * 移除变更监听器
     */
    public void removeChangeListener(Consumer<ToolManifest> listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners(ToolManifest manifest) {
        for (Consumer<ToolManifest> listener : changeListeners) {
            try {
                listener.accept(manifest);
            } catch (Exception e) {
                log.error("Listener notification failed: {}", e.getMessage());
            }
        }
    }

    /**
     * 注册默认 Engine 工具
     */
    private void registerDefaultEngineTools() {
        // read_file tool
        engineToolMap.put("read_file", new NanoBotTool() {
            @Override
            public String getToolName() {
                return "read_file";
            }

            @Override
            public String getToolDescription() {
                return "读取工作目录下的文件，参数：path（文件路径）";
            }

            @Override
            public String execute(Map<String, Object> parameters) {
                String path = parameters == null ? null : (String) parameters.get("path");
                if (path == null || path.isEmpty()) {
                    return "Error: missing required parameter 'path'";
                }
                return "File content for: " + path + " (simulated)";
            }
        });

        // exec tool
        engineToolMap.put("exec", new NanoBotTool() {
            @Override
            public String getToolName() {
                return "exec";
            }

            @Override
            public String getToolDescription() {
                return "执行系统命令，参数：command（命令内容）";
            }

            @Override
            public String execute(Map<String, Object> parameters) {
                String command = parameters == null ? null : (String) parameters.get("command");
                if (command == null || command.isEmpty()) {
                    return "Error: missing required parameter 'command'";
                }
                return "Executed: " + command + " (simulated)";
            }
        });

        // 注册到 manifest
        for (NanoBotTool tool : engineToolMap.values()) {
            toolManifestMap.put(tool.getToolName(), ToolManifest.fromNanoBotTool(tool));
        }

        log.info("Registered {} default engine tools", engineToolMap.size());
    }

    /**
     * 获取 OpenAI function calling 格式的工具声明列表
     */
    public List<Map<String, Object>> getFunctionDeclarations() {
        List<Map<String, Object>> functions = new ArrayList<>();
        for (ToolManifest manifest : getEnabledTools()) {
            functions.add(manifest.toFunctionDeclaration());
        }
        return functions;
    }

    /**
     * 获取工具统计信息
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("total", toolManifestMap.size());
        stats.put("engine", (int) engineToolMap.size());
        stats.put("mcp", countByType("MCP"));
        stats.put("codeInjection", countByType("CODE_INJECTION"));
        stats.put("thirdParty", countByType("THIRD_PARTY"));
        return stats;
    }

    private int countByType(String type) {
        int count = 0;
        for (ToolManifest m : toolManifestMap.values()) {
            if (type.equals(m.getType())) {
                count++;
            }
        }
        return count;
    }
}