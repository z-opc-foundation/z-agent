package com.zifang.z.agent.api.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 单次运行请求 DTO.
 *
 * <p>agentName: 要调用的 agent 名(从 AgentRegistry 查找).
 * <p>input: 用户输入(单轮对话) — 可为单字符串或多模 parts 列表.
 * <p>history: 历史消息(多轮对话).
 * <p>toolNames: 临时追加 tool 名(覆盖 agent 默认).
 * <p>config: 运行参数(maxSteps / temperature / model 覆盖).
 */
public final class RunRequest {

    private final String agentName;
    private final String input;
    private final List<Map<String, Object>> history;
    private final List<String> toolNames;
    private final Map<String, Object> config;

    private RunRequest(Builder b) {
        this.agentName = b.agentName;
        this.input = b.input == null ? "" : b.input;
        this.history = b.history == null ? Collections.<Map<String, Object>>emptyList()
                : Collections.unmodifiableList(new ArrayList<Map<String, Object>>(b.history));
        this.toolNames = b.toolNames == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(b.toolNames));
        this.config = b.config == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(b.config));
    }

    public String getAgentName() { return agentName; }
    public String getInput() { return input; }
    public List<Map<String, Object>> getHistory() { return history; }
    public List<String> getToolNames() { return toolNames; }
    public Map<String, Object> getConfig() { return config; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String agentName;
        private String input;
        private List<Map<String, Object>> history;
        private List<String> toolNames;
        private Map<String, Object> config;

        public Builder agentName(String v) { this.agentName = v; return this; }
        public Builder input(String v) { this.input = v; return this; }
        public Builder history(List<Map<String, Object>> v) { this.history = v; return this; }
        public Builder toolNames(List<String> v) { this.toolNames = v; return this; }
        public Builder config(Map<String, Object> v) { this.config = v; return this; }

        public RunRequest build() {
            if (agentName == null || agentName.isEmpty()) {
                throw new IllegalArgumentException("agentName required");
            }
            return new RunRequest(this);
        }
    }
}