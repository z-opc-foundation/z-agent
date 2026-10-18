package com.zifang.z.agent.llm.agent.observability;

/**
 * Token usage for a turn.
 *
 * <p>Phase 7 扩展：0815 业务侧统计需要 reasoning tokens 字段，
 * 该字段在使用推理模型（qwen / deepseek）的 OpenAI 兼容 provider 中上报。</p>
 */
public class TokenUsage {

    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;
    private int reasoningTokens;

    public TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public static TokenUsage empty() {
        return new TokenUsage(0, 0, 0);
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public int getReasoningTokens() {
        return reasoningTokens;
    }

    public void setReasoningTokens(int reasoningTokens) {
        this.reasoningTokens = reasoningTokens;
    }

    /**
     * Phase 7 扩展：TokenUsage → TurnUsage 转换（兼容 0815 调用）。
     */
    public TurnUsage toTurnUsage() {
        return new TurnUsage(promptTokens, completionTokens);
    }
}
