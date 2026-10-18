package com.zifang.z.agent.center.core.agent.tool;

import com.zifang.z.mcp.api.dto.McpToolDto;
import com.zifang.z.mcp.core.registry.McpRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * MCP Tool 自动同步服务
 * <p>
 * 负责:
 * 1. 启动时同步 MCP 工具到 ToolRegistry
 * 2. 定时检查 MCP 工具变更并同步
 * 3. 维护 Agent 实例与工具的关联关系
 * 4. 提供工具列表查询接口给 Agent 运行时
 */
// @Service  // 暂时禁用，MCP 工具同步改由调用方显式触发
public class McpToolSyncService {

    private static final Logger log = LogManager.getLogger(McpToolSyncService.class);
    private final ToolRegistry toolRegistry;
    // 工具版本追踪
    private final java.util.Map<String, Long> toolVersionMap = new java.util.concurrent.ConcurrentHashMap<>();
    @Autowired(required = false)
    private McpRegistry mcpRegistry;
    // 同步状态
    private volatile boolean syncEnabled = false;
    private volatile long lastSyncTime = 0;
    private volatile int totalSyncedCount = 0;

    public McpToolSyncService() {
        this.toolRegistry = ToolRegistry.getInstance();
    }

    @PostConstruct
    public void init() {
        if (mcpRegistry != null) {
            toolRegistry.bindMcpRegistry(mcpRegistry);
            syncNow();
            syncEnabled = true;
            log.info("McpToolSyncService initialized with MCP Registry");
        } else {
            log.warn("McpRegistry not available, MCP tool sync disabled");
        }
    }

    /**
     * 立即执行一次同步
     */
    public synchronized void syncNow() {
        if (mcpRegistry == null) {
            log.debug("syncNow skipped: MCP Registry is null");
            return;
        }

        long start = System.currentTimeMillis();
        int added = 0;
        int updated = 0;

        List<McpToolDto> mcpTools = mcpRegistry.listTools();

        for (McpToolDto dto : mcpTools) {
            String toolName = dto.getName();
            Long oldVersion = toolVersionMap.get(toolName);
            Long newVersion = fingerprint(dto);

            ToolManifest manifest = ToolManifest.fromMcp(dto);

            if (oldVersion == null) {
                // 新工具
                toolRegistry.registerThirdPartyTool(manifest);
                added++;
            } else if (!oldVersion.equals(newVersion)) {
                // 工具已更新
                toolRegistry.registerThirdPartyTool(manifest);
                updated++;
            }

            toolVersionMap.put(toolName, newVersion);
        }

        lastSyncTime = System.currentTimeMillis();
        totalSyncedCount = mcpTools.size();

        long cost = System.currentTimeMillis() - start;
        log.info("MCP sync completed: added={}, updated={}, total={}, cost={}ms",
                added, updated, totalSyncedCount, cost);
    }

    /**
     * 定时同步 (每 30 秒)
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 60000)
    public void scheduledSync() {
        if (!syncEnabled) {
            return;
        }

        try {
            syncNow();
        } catch (Exception e) {
            log.error("Scheduled MCP sync failed: {}", e.getMessage());
        }
    }

    /**
     * 获取已同步的工具列表 (给 Agent 运行时用)
     */
    public List<ToolManifest> getSyncedTools() {
        return toolRegistry.getEnabledTools();
    }

    /**
     * 获取工具统计
     */
    public java.util.Map<String, Object> getSyncStatus() {
        java.util.Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("enabled", syncEnabled);
        status.put("lastSyncTime", lastSyncTime);
        status.put("totalSyncedTools", totalSyncedCount);
        status.put("toolRegistrySize", toolRegistry.size());
        status.put("statistics", toolRegistry.getStatistics());
        return status;
    }

    /**
     * 手动触发同步 (API 调用)
     */
    public void triggerSync() {
        log.info("Manual MCP sync triggered");
        syncNow();
    }

    /**
     * 获取特定工具是否已同步
     */
    public boolean isToolSynced(String toolName) {
        return toolRegistry.hasTool(toolName);
    }

    /**
     * 获取 MCP 工具详情
     */
    public ToolManifest getToolManifest(String toolName) {
        return toolRegistry.getTool(toolName);
    }

    /**
     * 执行工具
     */
    public String executeTool(String toolName, java.util.Map<String, Object> parameters) {
        return toolRegistry.executeTool(toolName, parameters);
    }

    /**
     * 启用/禁用同步
     */
    public void setSyncEnabled(boolean enabled) {
        this.syncEnabled = enabled;
        log.info("MCP sync enabled={}", enabled);
    }

    /**
     * 检查工具是否有更新
     */
    public java.util.List<String> checkForUpdates() {
        if (mcpRegistry == null) {
            return java.util.Collections.emptyList();
        }


        java.util.List<String> updatedTools = new java.util.ArrayList<>();
        List<McpToolDto> mcpTools = mcpRegistry.listTools();

        for (McpToolDto dto : mcpTools) {
            Long oldVersion = toolVersionMap.get(dto.getName());
            if (oldVersion != null && !oldVersion.equals(fingerprint(dto))) {
                updatedTools.add(dto.getName());
            }
        }

        return updatedTools;
    }

    /**
     * 工具内容指纹: z-mcp 的目录项没有 createTime，改动只能从会被看见的字段上判出来.
     */
    private static long fingerprint(McpToolDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append(dto.getName()).append('|').append(dto.getTitle()).append('|')
                .append(dto.getDescription()).append('|')
                .append(dto.getInputSchemaJson()).append('|').append(dto.getOutputSchemaJson());
        return sb.toString().hashCode();
    }
}