package com.zifang.z.agent.llm.infrastructure.llm.context;

/**
 * LLM stream request — carries the LLM code and stream options.
 */
public class LlmStreamRequest {

    private final String llmCode;
    private final StreamOptions options;

    public LlmStreamRequest(String llmCode) {
        this(llmCode, null);
    }

    public LlmStreamRequest(String llmCode, StreamOptions options) {
        this.llmCode = llmCode;
        this.options = options != null ? options : new StreamOptions();
    }

    public String getLlmCode() {
        return llmCode;
    }

    public StreamOptions getOptions() {
        return options;
    }

    /**
     * Stream options.
     */
    public static class StreamOptions {
        private boolean stream = true;
        private double temperature = 0.7;
        private int maxTokens = 4096;

        public boolean isStream() {
            return stream;
        }

        public void setStream(boolean stream) {
            this.stream = stream;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
}
