package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.common.enums.AgentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 执行器工厂
 * 根据实例的 runMode 选择对应的执行器
 * <p>
 * 支持两种模式:
 * - IN_PROCESS: ReAct 循环 (本地内存执行)
 * - CONTAINER: Hermes 远程代理 (Docker 容器)
 */
@Component
public class AgentExecutorFactory {

    private static final Logger log = LogManager.getLogger(AgentExecutorFactory.class);

    @Resource
    private AgentInstanceService agentInstanceService;

    @Resource
    private AgentContainerService containerService;

    // ReAct Agent 工厂 (内存中执行)
    private ReActAgentFactory reActAgentFactory;
    @Resource
    private LocalProcessAgentRuntime localProcessRuntime;

    @PostConstruct
    public void init() {
        // 初始化 ReAct Agent 工厂
        this.reActAgentFactory = new ReActAgentFactory();
        log.info("AgentExecutorFactory initialized");
    }

    /**
     * 根据实例编码获取执行器
     */
    public AgentExecutor getExecutor(String instanceCode) {
        AgentInstance instance = agentInstanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            throw new RuntimeException("Agent instance not found: " + instanceCode);
        }
        return getExecutorByInstance(instance);
    }

    /**
     * 根据运行模式获取执行器
     */
    public AgentExecutor getExecutorByMode(String runMode, String instanceCode) {
        if ("CONTAINER".equals(runMode)) {
            HermesProxyExecutor executor = new HermesProxyExecutor(containerService);
            // 注册已有容器
            Integer port = instanceCode != null ? getInstancePort(instanceCode) : null;
            if (port != null) {
                executor.registerContainer(instanceCode, port);
            }
            return executor;
        } else if ("IN_PROCESS".equals(runMode)) {
            return new InProcessAgentExecutor(reActAgentFactory);
        } else {
            throw new RuntimeException("Unknown run mode: " + runMode);
        }
    }

    /**
     * 获取 ReAct Agent 工厂 (用于 IN_PROCESS 模式)
     */
    public ReActAgentFactory getReActAgentFactory() {
        return reActAgentFactory;
    }

    /**
     * 获取实例对应的容器端口
     */
    private Integer getInstancePort(String instanceCode) {
        // TODO: 从 AgentInstance 获取 containerPort 字段
        // 目前通过 AgentInstanceService 查询
        AgentInstance instance = agentInstanceService.getByInstanceCode(instanceCode);
        if (instance != null && instance.getContainerPort() != null) {
            return instance.getContainerPort();
        }
        return null;
    }

    // ===== FEATURE014 多 Agent — 按 agentType 分发执行器 =====

    /**
     * 获取所有可用的执行器类型
     */
    public Map<String, String> getAvailableModes() {
        Map<String, String> modes = new HashMap<String, String>();
        modes.put("IN_PROCESS", "ReAct 循环 (本地内存执行)");
        modes.put("CONTAINER", "Hermes 远程代理 (Docker 容器)");
        return modes;
    }

    /**
     * 根据实例编码获取执行器（优先使用 agentType 字段，回退到 runMode）。
     *
     * <p>FEATURE014 起, agentType 是主键；runMode 仅作后向兼容。</p>
     */
    public AgentExecutor getExecutorByInstance(AgentInstance instance) {
        if (instance == null) {
            throw new RuntimeException("Agent instance is null");
        }
        // 1. 优先按 agentType 分发
        AgentType agentType = resolveAgentType(instance);
        switch (agentType) {
            case ZBOT:
                return new InProcessAgentExecutor(reActAgentFactory);
            case HERMES_CONTAINER:
                return buildContainerExecutor(instance);
            case HERMES_LOCAL:
            case CLAUDE_CODE:
            case OPENCODE:
            case CUSTOM:
                // 统一走 LocalProcessExecutor（Day 2/3 会拆成多个子类）
                return buildLocalProcessExecutor(instance, agentType);
            default:
                log.warn("Unknown agentType {}, fallback to ZBOT IN_PROCESS", agentType);
                return new InProcessAgentExecutor(reActAgentFactory);
        }
    }

    /**
     * 解析 agentType：优先读新字段，若为空则从旧 runMode 反推。
     */
    private AgentType resolveAgentType(AgentInstance instance) {
        if (instance.getAgentType() != null && !instance.getAgentType().trim().isEmpty()) {
            return AgentType.parse(instance.getAgentType());
        }
        // 回退旧逻辑
        return AgentType.fromLegacyRunMode(instance.getRunMode());
    }

    private HermesProxyExecutor buildContainerExecutor(AgentInstance instance) {
        HermesProxyExecutor executor = new HermesProxyExecutor(containerService);
        Integer port = instance.getContainerPort();
        if (port != null) {
            executor.registerContainer(instance.getInstanceCode(), port);
        }
        return executor;
    }

    private AgentExecutor buildLocalProcessExecutor(AgentInstance instance, AgentType agentType) {
        // Day 2/3: 按 agentType 返回对应的语义子类，便于后续接入 per-type 协议
        LocalProcessProxyExecutor executor;
        switch (agentType) {
            case CLAUDE_CODE:
                executor = new ClaudeCodeExecutor(localProcessRuntime);
                break;
            case OPENCODE:
                executor = new OpenCodeExecutor(localProcessRuntime);
                break;
            case HERMES_LOCAL:
                executor = new HermesLocalExecutor(localProcessRuntime);
                break;
            default:
                executor = new LocalProcessProxyExecutor(localProcessRuntime, agentType);
        }
        // 注册进程（如果 instance 已有端口则直接注册；否则按需 startProcess）
        Integer port = resolveLocalPort(instance, agentType);
        if (port != null) {
            executor.registerProcess(instance.getInstanceCode(), port, instance.getCallbackUrl());
        }
        return executor;
    }

    private Integer resolveLocalPort(AgentInstance instance, AgentType agentType) {
        // 如果实例已经记录了 callbackUrl，从中提取端口
        String url = instance.getCallbackUrl();
        if (url != null && !url.isEmpty()) {
            try {
                java.net.URI uri = java.net.URI.create(url);
                if (uri.getPort() > 0) {
                    return uri.getPort();
                }
            } catch (Exception ignored) {
            }
        }
        // 否则回退到 containerPort（HERMES_CONTAINER 兼容场景）
        return instance.getContainerPort();
    }

    /**
     * 列出当前已注册的所有 Agent 类型（含 displayName），供前端下拉使用。
     */
    public Map<String, String> getAvailableAgentTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        for (AgentType t : AgentType.values()) {
            types.put(t.name(), t.getDisplayName());
        }
        return types;
    }
}