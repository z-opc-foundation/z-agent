package com.zifang.z.agent.engine.agent.plugin;

/**
 * Agent 插件接口。
 * 一个插件可以同时实现 AgentLifecycleHook（生命周期钩子）
 * 和其他扩展点（如自定义 Tool、Custom Planner 等）。
 */
public interface AgentPlugin {
    /**
     * 插件名称
     */
    String name();

    /**
     * 插件版本
     */
    String version();

    /**
     * 初始化时调用（传入插件配置）
     */
    default void init(java.util.Map<String, Object> config) {
    }

    /**
     * 获取此插件注册的钩子（可返回空列表）
     */
    default AgentLifecycleHook lifecycleHook() {
        return null;
    }
}