package com.zifang.z.agent.llm.agent.observability;

/**
 * Records token usage for a single turn。
 *
 * <p>Phase 7 扩展：0815 业务侧（{@code FtAgentChatService} 等）使用 {@code TokenUsage}
 * 字段名（promptTokens / completionTokens / reasoningTokens），本类同时提供这两个
 * 名字，使 ft 与 core 互不依赖、各自消费符合语义。</p>
 */
public class TurnUsage {

    private final int inputTokens;
    private final int outputTokens;
    private int reasoningTokens;

    public TurnUsage(int inputTokens, int outputTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    /**
     * 0815 业务 TokenUsage 字段名兼容。
     */
    public int getPromptTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    /**
     * 0815 业务 TokenUsage 字段名兼容。
     */
    public int getCompletionTokens() {
        return outputTokens;
    }

    public int getTotalTokens() {
        return inputTokens + outputTokens + reasoningTokens;
    }

    public int getReasoningTokens() {
        return reasoningTokens;
    }

    public void setReasoningTokens(int reasoningTokens) {
        this.reasoningTokens = reasoningTokens;
    }
}
