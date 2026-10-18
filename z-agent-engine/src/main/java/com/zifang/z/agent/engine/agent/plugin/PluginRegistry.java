package com.zifang.z.agent.engine.agent.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 全局插件注册中心。
 * 负责插件发现、注册、按优先级排序。
 */
public class PluginRegistry {
    private static final PluginRegistry INSTANCE = new PluginRegistry();

    private final List<AgentPlugin> plugins = new CopyOnWriteArrayList<>();

    private PluginRegistry() {
    }

    public static PluginRegistry getInstance() {
        return INSTANCE;
    }

    public void register(AgentPlugin plugin) {
        if (plugin == null) {
            return;
        }
        plugins.add(plugin);
    }

    public void unregister(String pluginName) {
        plugins.removeIf(p -> pluginName.equals(p.name()));
    }

    public List<AgentPlugin> getPlugins() {
        return new ArrayList<>(plugins);
    }

    /**
     * 获取所有已注册的生命周期钩子（扁平化）
     */
    public List<AgentLifecycleHook> getLifecycleHooks() {
        List<AgentLifecycleHook> hooks = new ArrayList<>();
        for (AgentPlugin p : plugins) {
            AgentLifecycleHook h = p.lifecycleHook();
            if (h != null) {
                hooks.add(h);
            }

        }
        return hooks;
    }

    public void clear() {
        plugins.clear();
    }
}