package com.zifang.z.agent.center.core.agent.tool;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.engine.agent.llm.tool.core.NanoBotTool;
import com.zifang.z.mcp.api.dto.McpToolDto;

import java.util.Map;

/**
 * Tool Manifest - 标准化工具清单 schema
 * <p>
 * 统一工具描述格式，支持:
 * - 内置工具 (BUILT_IN)
 * - MCP 工具 (MCP)
 * - 代码注入工具 (CODE_INJECTION)
 * - 第三方工具 (THIRD_PARTY)
 * <p>
 * 用于 Agent 运行时获取可用工具列表，贴合 LLM function calling 规范
 */
public class ToolManifest {

    private String toolName;          // 工具唯一标识
    private String type;              // 工具类型: BUILT_IN | MCP | CODE_INJECTION | THIRD_PARTY
    private String description;      // 工具描述（给模型看）
    private String category;         // 工具分类: file | network | system | database | custom
    private Map<String, Object> inputSchema;   // JSON Schema for LLM
    private Map<String, Object> outputSchema;  // 输出 schema
    private String source;           // 来源: engine | mcp | custom
    private String executeUrl;       // 执行地址 (第三方/MCP)
    private boolean enabled;         // 是否启用
    private int priority;            // 优先级（数字越小越高）
    private long createTime;
    private long updateTime;

    public ToolManifest() {
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
        this.enabled = true;
        this.priority = 100;
    }

    public ToolManifest(String toolName, String type, String description) {
        this();
        this.toolName = toolName;
        this.type = type;
        this.description = description;
    }

    // ── Builder ──
    public static Builder builder() {
        return new Builder();
    }

    // ── Convert from McpToolDto (z-mcp) ──
    public static ToolManifest fromMcp(McpToolDto dto) {
        if (dto == null) {
            return null;
        }

        String serverName = dto.getServerName();
        return builder()
                .toolName(dto.getName())
                .type("MCP")
                .description(dto.getDescription())
                .inputSchema(readSchema(dto.getInputSchemaJson()))
                .outputSchema(readSchema(dto.getOutputSchemaJson()))
                .source(serverName == null ? "mcp-center" : "mcp:" + serverName)
                .build();
    }

    /**
     * schema 来自外部 MCP server 的 tools/list，单个坏 schema 不能顶掉整张工具表，降级为 null。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> readSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.isEmpty()) {
            return null;
        }
        try {
            // schema 会随 manifest 进 OpenAI function declaration，必须摊平成 JDK Map
            Object plain = JsonUtil.toPlainValue(JsonUtil.parse(schemaJson));
            return plain instanceof Map ? (Map<String, Object>) plain : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Convert from NanoBotTool (Engine) ──
    public static ToolManifest fromNanoBotTool(NanoBotTool tool) {
        if (tool == null) {
            return null;
        }

        return builder()
                .toolName(tool.getToolName())
                .type("BUILT_IN")
                .description(tool.getToolDescription())
                .source("engine")
                .build();
    }

    // ── Getters & Setters ──
    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Map<String, Object> inputSchema) {
        this.inputSchema = inputSchema;
    }

    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(Map<String, Object> outputSchema) {
        this.outputSchema = outputSchema;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExecuteUrl() {
        return executeUrl;
    }

    public void setExecuteUrl(String executeUrl) {
        this.executeUrl = executeUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 生成 OpenAI function calling 格式
     */
    public Map<String, Object> toFunctionDeclaration() {
        java.util.Map<String, Object> func = new java.util.LinkedHashMap<>();
        func.put("name", toolName);
        func.put("description", description);
        if (inputSchema != null) {
            func.put("parameters", inputSchema);
        } else {
            // 默认 empty schema
            java.util.Map<String, Object> emptySchema = new java.util.LinkedHashMap<>();
            emptySchema.put("type", "object");
            emptySchema.put("properties", new java.util.LinkedHashMap<>());
            func.put("parameters", emptySchema);
        }
        return func;
    }

    @Override
    public String toString() {
        return "ToolManifest{" +
                "toolName='" + toolName + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", source='" + source + '\'' +
                ", enabled=" + enabled +
                '}';
    }

    public static class Builder {
        private final ToolManifest manifest = new ToolManifest();

        public Builder toolName(String toolName) {
            manifest.toolName = toolName;
            return this;
        }

        public Builder type(String type) {
            manifest.type = type;
            return this;
        }

        public Builder description(String desc) {
            manifest.description = desc;
            return this;
        }

        public Builder category(String category) {
            manifest.category = category;
            return this;
        }

        public Builder inputSchema(Map<String, Object> schema) {
            manifest.inputSchema = schema;
            return this;
        }

        public Builder outputSchema(Map<String, Object> schema) {
            manifest.outputSchema = schema;
            return this;
        }

        public Builder source(String source) {
            manifest.source = source;
            return this;
        }

        public Builder executeUrl(String url) {
            manifest.executeUrl = url;
            return this;
        }

        public Builder enabled(boolean enabled) {
            manifest.enabled = enabled;
            return this;
        }

        public Builder priority(int priority) {
            manifest.priority = priority;
            return this;
        }

        public ToolManifest build() {
            return manifest;
        }
    }
}