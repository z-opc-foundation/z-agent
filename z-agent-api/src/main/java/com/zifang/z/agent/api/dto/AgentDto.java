package com.zifang.z.agent.api.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z-agent 平台 Agent 描述 DTO.
 *
 * <p>name: agent 唯一标识.
 * <p>kind: agent 类型 (react / plan / multi / custom / external).
 * <p>description: 用途描述(给 z-agent-admin 列表展示 + 调试用).
 * <p>tools: 可用工具名列表(给 LLM prompt 注入).
 * <p>model: 默认 LLM 模型名(从 kernel.llm 6 provider 中选).
 * <p>systemPrompt: 系统提示(可选, 不传则用 agent 默认).
 * <p>capabilities: 能力描述(给路由 / 权限系统读).
 */
public final class AgentDto {

    private final String name;
    private final String kind;
    private final String description;
    private final List<String> tools;
    private final String model;
    private final String systemPrompt;
    private final Map<String, String> capabilities;
    private final Map<String, Object> metadata;

    private AgentDto(Builder b) {
        this.name = b.name;
        this.kind = b.kind == null ? "react" : b.kind;
        this.description = b.description == null ? "" : b.description;
        this.tools = b.tools == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(b.tools));
        this.model = b.model;
        this.systemPrompt = b.systemPrompt;
        this.capabilities = b.capabilities == null ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, String>(b.capabilities));
        this.metadata = b.metadata == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(b.metadata));
    }

    public String getName() { return name; }
    public String getKind() { return kind; }
    public String getDescription() { return description; }
    public List<String> getTools() { return tools; }
    public String getModel() { return model; }
    public String getSystemPrompt() { return systemPrompt; }
    public Map<String, String> getCapabilities() { return capabilities; }
    public Map<String, Object> getMetadata() { return metadata; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String name;
        private String kind;
        private String description;
        private List<String> tools;
        private String model;
        private String systemPrompt;
        private Map<String, String> capabilities;
        private Map<String, Object> metadata;

        public Builder name(String v) { this.name = v; return this; }
        public Builder kind(String v) { this.kind = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder tools(List<String> v) { this.tools = v; return this; }
        public Builder model(String v) { this.model = v; return this; }
        public Builder systemPrompt(String v) { this.systemPrompt = v; return this; }
        public Builder capabilities(Map<String, String> v) { this.capabilities = v; return this; }
        public Builder metadata(Map<String, Object> v) { this.metadata = v; return this; }

        public AgentDto build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name required");
            }
            return new AgentDto(this);
        }
    }
}