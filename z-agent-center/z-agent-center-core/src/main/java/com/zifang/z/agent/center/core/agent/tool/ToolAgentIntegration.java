package com.zifang.z.agent.center.core.agent.tool;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool Agent Integration - Agent 实例与工具的关联管理
 * <p>
 * 负责:
 * 1. 管理 Agent 实例关联的工具列表
 * 2. 为 Agent 运行时提供可用工具清单
 * 3. 支持工具动态注入到 Agent
 * 4. 工具调用路由到 ToolRegistry 执行
 */
@Component
public class ToolAgentIntegration {

    private static final Logger log = LogManager.getLogger(ToolAgentIntegration.class);
    private final ToolRegistry toolRegistry;
    // Agent 实例 -> 工具列表 映射
    private final Map<String, Set<String>> agentToolsMap = new ConcurrentHashMap<>();
    // 工具 -> Agent 实例 映射 (反向索引)
    private final Map<String, Set<String>> toolAgentsMap = new ConcurrentHashMap<>();
    @Autowired
    private AgentInstanceService agentInstanceService;

    public ToolAgentIntegration() {
        this.toolRegistry = ToolRegistry.getInstance();
    }

    @PostConstruct
    public void init() {
        // 注册工具变更监听器
        toolRegistry.addChangeListener(this::onToolChanged);
        log.info("ToolAgentIntegration initialized");
    }

    /**
     * 为 Agent 实例绑定工具列表
     */
    public void bindToolsToAgent(String instanceCode, List<String> toolNames) {
        if (instanceCode == null || toolNames == null) {
            return;
        }

        // 清理旧绑定
        Set<String> oldTools = agentToolsMap.remove(instanceCode);
        if (oldTools != null) {
            for (String toolName : oldTools) {
                Set<String> agents = toolAgentsMap.get(toolName);
                if (agents != null) {
                    agents.remove(instanceCode);
                }

            }
        }

        // 建立新绑定
        Set<String> newTools = new HashSet<>(toolNames);
        agentToolsMap.put(instanceCode, newTools);

        for (String toolName : newTools) {
            toolAgentsMap.computeIfAbsent(toolName, k -> new HashSet<>()).add(instanceCode);
        }

        log.info("Bound {} tools to agent instance: {}", toolNames.size(), instanceCode);
    }

    /**
     * 为 Agent 实例添加单个工具
     */
    public void addToolToAgent(String instanceCode, String toolName) {
        if (instanceCode == null || toolName == null) {
            return;
        }

        Set<String> tools = agentToolsMap.computeIfAbsent(instanceCode, k -> new HashSet<>());
        tools.add(toolName);
        toolAgentsMap.computeIfAbsent(toolName, k -> new HashSet<>()).add(instanceCode);

        log.debug("Added tool '{}' to agent instance '{}'", toolName, instanceCode);
    }

    /**
     * 为 Agent 实例移除单个工具
     */
    public void removeToolFromAgent(String instanceCode, String toolName) {
        if (instanceCode == null || toolName == null) {
            return;
        }

        Set<String> tools = agentToolsMap.get(instanceCode);
        if (tools != null) {
            tools.remove(toolName);
        }

        Set<String> agents = toolAgentsMap.get(toolName);
        if (agents != null) {
            agents.remove(instanceCode);
        }

        log.debug("Removed tool '{}' from agent instance '{}'", toolName, instanceCode);
    }

    /**
     * 获取 Agent 实例关联的所有工具 Manifest
     */
    public List<ToolManifest> getToolsForAgent(String instanceCode) {
        Set<String> toolNames = agentToolsMap.get(instanceCode);
        if (toolNames == null || toolNames.isEmpty()) {
            // 返回默认工具
            return toolRegistry.getEnabledTools();
        }

        List<ToolManifest> result = new ArrayList<>();
        for (String toolName : toolNames) {
            ToolManifest manifest = toolRegistry.getTool(toolName);
            if (manifest != null && manifest.isEnabled()) {
                result.add(manifest);
            }
        }
        return result;
    }

    /**
     * 获取 Agent 可用的工具名称列表
     */
    public List<String> getToolNamesForAgent(String instanceCode) {
        Set<String> toolNames = agentToolsMap.get(instanceCode);
        if (toolNames == null || toolNames.isEmpty()) {
            // 返回所有可用工具名
            List<ToolManifest> allTools = toolRegistry.getEnabledTools();
            List<String> names = new ArrayList<>(allTools.size());
            for (ToolManifest m : allTools) {
                names.add(m.getToolName());
            }
            return names;
        }
        return new ArrayList<>(toolNames);
    }

    /**
     * 检查 Agent 是否有特定工具权限
     */
    public boolean agentHasTool(String instanceCode, String toolName) {
        Set<String> toolNames = agentToolsMap.get(instanceCode);
        if (toolNames == null || toolNames.isEmpty()) {
            // 无绑定，返回全局工具
            return toolRegistry.hasTool(toolName);
        }
        return toolNames.contains(toolName);
    }

    /**
     * 执行工具 (Agent 运行时调用)
     */
    public String executeToolForAgent(String instanceCode, String toolName, Map<String, Object> parameters) {
        if (!agentHasTool(instanceCode, toolName)) {
            return "Error: Agent '" + instanceCode + "' does not have permission to use tool '" + toolName + "'";
        }
        return toolRegistry.executeTool(toolName, parameters);
    }

    /**
     * 获取 Agent 绑定的工具数量
     */
    public int getToolCountForAgent(String instanceCode) {
        Set<String> toolNames = agentToolsMap.get(instanceCode);
        return toolNames != null ? toolNames.size() : 0;
    }

    /**
     * 移除 Agent 的所有工具绑定
     */
    public void unbindAllTools(String instanceCode) {
        Set<String> oldTools = agentToolsMap.remove(instanceCode);
        if (oldTools != null) {
            for (String toolName : oldTools) {
                Set<String> agents = toolAgentsMap.get(toolName);
                if (agents != null) {
                    agents.remove(instanceCode);
                }

            }
        }
        log.info("Unbound all tools from agent instance: {}", instanceCode);
    }

    /**
     * 获取使用某工具的所有 Agent 实例
     */
    public List<String> getAgentsUsingTool(String toolName) {
        Set<String> agents = toolAgentsMap.get(toolName);
        return agents != null ? new ArrayList<>(agents) : Collections.emptyList();
    }

    /**
     * 获取工具统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAgents", agentToolsMap.size());
        stats.put("totalTools", toolRegistry.size());
        stats.put("toolUsageCount", toolAgentsMap.size());

        // 每个 agent 的工具数
        Map<String, Integer> agentToolCounts = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : agentToolsMap.entrySet()) {
            agentToolCounts.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("agentToolCounts", agentToolCounts);

        return stats;
    }

    /**
     * 工具变更回调
     */
    private void onToolChanged(ToolManifest manifest) {
        // 当工具变更时，通知所有使用该工具的 Agent
        Set<String> agents = toolAgentsMap.get(manifest.getToolName());
        if (agents != null && !agents.isEmpty()) {
            log.info("Tool '{}' changed, notifying {} agents", manifest.getToolName(), agents.size());
            // 这里可以触发 Agent 重新加载工具列表等后续处理
        }
    }

    /**
     * 从数据库加载 Agent 实例的工具配置
     */
    public void loadAgentToolBindings() {
        if (agentInstanceService == null) {
            log.warn("AgentInstanceService not available, skip loading bindings");
            return;
        }

        try {
            // 获取所有 Agent 实例 (通过 owner 查询，null 表示所有)
            List<AgentInstance> instances = agentInstanceService.listByOwner(null);
            if (instances == null) {
                instances = Collections.emptyList();
            }
            for (AgentInstance instance : instances) {
                // 工具配置存储在 instance 的配置字段中 (JSON 格式)
                String toolConfig = instance.getConfig();
                if (toolConfig != null && !toolConfig.isEmpty()) {
                    try {
                        // tools 下游要 instanceof List 后 (List<String>) 强转，嵌套容器必须摊平
                        Object plain = JsonUtil.toPlainValue(JsonUtil.parse(toolConfig));
                        if (!(plain instanceof Map)) {
                            continue;
                        }
                        Object toolsObj = ((Map<String, Object>) plain).get("tools");
                        if (toolsObj instanceof List) {
                            List<String> tools = (List<String>) toolsObj;
                            bindToolsToAgent(instance.getInstanceCode(), tools);
                        }
                    } catch (Exception e) {
                        log.debug("Failed to parse tool config for instance {}: {}",
                                instance.getInstanceCode(), e.getMessage());
                    }
                }
            }
            log.info("Loaded tool bindings for {} agent instances", agentToolsMap.size());
        } catch (Exception e) {
            log.error("Failed to load agent tool bindings: {}", e.getMessage());
        }
    }

    /**
     * 获取 OpenAI function calling 格式的工具声明 (给特定 Agent)
     */
    public List<Map<String, Object>> getFunctionDeclarationsForAgent(String instanceCode) {
        List<ToolManifest> tools = getToolsForAgent(instanceCode);
        List<Map<String, Object>> functions = new ArrayList<>(tools.size());
        for (ToolManifest m : tools) {
            functions.add(m.toFunctionDeclaration());
        }
        return functions;
    }
}