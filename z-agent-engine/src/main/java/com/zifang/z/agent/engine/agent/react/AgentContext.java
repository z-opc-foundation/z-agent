package com.zifang.z.agent.engine.agent.react;

import java.util.HashMap;
import java.util.Map;

/**
 * AgentContext — 父子代理共享的上下文
 * <p>
 * 用于在父/子 agent 之间传递：
 * - InMemoryMemory（共享记忆）
 * - SharedData（kv store）
 * - Config（共享配置）
 * - Parent reference（用于结果冒泡）
 */
public class AgentContext {

    private final InMemoryMemory memory;
    private final Map<String, Object> sharedData;
    private final Map<String, String> config;
    private final SubAgent parent;
    private final long createdAt;

    public AgentContext(InMemoryMemory memory, SubAgent parent) {
        this.memory = memory != null ? memory : new InMemoryMemory();
        this.sharedData = new HashMap<>();
        this.config = new HashMap<>();
        this.parent = parent;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * 复制上下文（用于创建子代理）
     */
    public AgentContext fork(SubAgent childParent) {
        AgentContext fork = new AgentContext(this.memory, childParent);
        fork.sharedData.putAll(this.sharedData);
        fork.config.putAll(this.config);
        return fork;
    }

    // ========== Memory ==========
    public InMemoryMemory getMemory() {
        return memory;
    }

    public void addToMemory(String role, String content) {
        memory.add(Msg.user(content));  // 简化：统一作为 user message 记录
    }

    // ========== SharedData ==========
    public void set(String key, Object value) {
        sharedData.put(key, value);
    }

    public Object get(String key) {
        return sharedData.get(key);
    }

    public Object remove(String key) {
        return sharedData.remove(key);
    }

    public boolean has(String key) {
        return sharedData.containsKey(key);
    }

    // ========== Config ==========
    public void setConfig(String key, String value) {
        config.put(key, value);
    }

    public String getConfig(String key) {
        return config.get(key);
    }

    public String getConfig(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    // ========== Parent ==========
    public SubAgent getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    // ========== Meta ==========
    public long getAgeSeconds() {
        return (System.currentTimeMillis() - createdAt) / 1000;
    }

    @Override
    public String toString() {
        return "AgentContext(mem=" + memory.size()
                + ", data=" + sharedData.size()
                + ", parent=" + (parent != null ? parent.getName() : "root") + ")";
    }
}