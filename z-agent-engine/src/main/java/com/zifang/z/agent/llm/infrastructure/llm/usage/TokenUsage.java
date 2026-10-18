package com.zifang.z.agent.llm.infrastructure.llm.usage;

/**
 * Token usage tracking.
 */
public class TokenUsage {

    private int inputTokenCount;
    private int outputTokenCount;
    private int totalTokenCount;

    public TokenUsage() {
    }

    public TokenUsage(int inputTokenCount, int outputTokenCount) {
        this.inputTokenCount = inputTokenCount;
        this.outputTokenCount = outputTokenCount;
        this.totalTokenCount = inputTokenCount + outputTokenCount;
    }

    public TokenUsage(int inputTokenCount, int outputTokenCount, int totalTokenCount, int ignored) {
        this.inputTokenCount = inputTokenCount;
        this.outputTokenCount = outputTokenCount;
        this.totalTokenCount = totalTokenCount;
    }

    public static TokenUsage empty() {
        return new TokenUsage(0, 0);
    }

    public int getInputTokenCount() {
        return inputTokenCount;
    }

    public void setInputTokenCount(int inputTokenCount) {
        this.inputTokenCount = inputTokenCount;
    }

    public int getOutputTokenCount() {
        return outputTokenCount;
    }

    public void setOutputTokenCount(int outputTokenCount) {
        this.outputTokenCount = outputTokenCount;
    }

    public int getTotalTokenCount() {
        return totalTokenCount;
    }

    public void setTotalTokenCount(int totalTokenCount) {
        this.totalTokenCount = totalTokenCount;
    }
}
