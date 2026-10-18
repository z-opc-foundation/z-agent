package com.zifang.z.agent.engine.agent.llm.provider;

/**
 * LLM caller configuration for model connection.
 */
public class LlmCallerConfig {

    private String modelName;
    private String baseUrl = "http://localhost:11434";

    public LlmCallerConfig() {
    }

    public LlmCallerConfig(String modelName, String baseUrl) {
        this.modelName = modelName;
        this.baseUrl = baseUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static class Builder {
        private String modelName;
        private String baseUrl = "http://localhost:11434";

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public LlmCallerConfig build() {
            return new LlmCallerConfig(modelName, baseUrl);
        }
    }
}
