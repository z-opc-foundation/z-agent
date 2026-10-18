package com.zifang.z.agent.llm.infrastructure.llm.model;

/**
 * LLM model descriptor。
 *
 * <p>Phase 7 扩展：在 0815 业务侧需要的字段（modelName、endpoint、apiKey、timeoutMs、maxTokens、
 * reasoning、compat、thinkingLevelMap 等）均通过 Builder 与 setter 暴露，保持向后兼容 0815
 * 调用模式。同时保留 0615 旧构造器签名以兼容最小化用例。</p>
 */
public class LlmModel {

    private final String code;
    private String name;
    private String provider;
    private String modelName;
    private String endpoint;
    private String apiKey;
    private long timeoutMs = 120_000L;
    private Integer maxTokens;
    private boolean reasoning;

    public LlmModel(String code, String name, String provider) {
        this.code = code;
        this.name = name;
        this.provider = provider;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public boolean isReasoning() {
        return reasoning;
    }

    public void setReasoning(boolean reasoning) {
        this.reasoning = reasoning;
    }

    /**
     * LlmModel 链式构造器（Phase 7 扩展）。所有字段设为默认值，
     * 调用方按需覆写。
     */
    public static class Builder {
        private String code;
        private String name;
        private String provider;
        private String modelName;
        private String endpoint;
        private String apiKey;
        private long timeoutMs = 120_000L;
        private Integer maxTokens;
        private boolean reasoning;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder reasoning(boolean reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public LlmModel build() {
            LlmModel model = new LlmModel(code, name, provider);
            model.setModelName(modelName);
            model.setEndpoint(endpoint);
            model.setApiKey(apiKey);
            model.setTimeoutMs(timeoutMs);
            model.setMaxTokens(maxTokens);
            model.setReasoning(reasoning);
            return model;
        }
    }
}
