package com.zifang.z.agent.api.dto;

/**
 * token 用量 DTO (供 z-agent-admin 视图渲染).
 */
public final class TokenUsageDto {

    private final long promptTokens;
    private final long completionTokens;
    private final long totalTokens;

    public TokenUsageDto(long promptTokens, long completionTokens, long totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public long getPromptTokens() { return promptTokens; }
    public long getCompletionTokens() { return completionTokens; }
    public long getTotalTokens() { return totalTokens; }

    public static TokenUsageDto empty() {
        return new TokenUsageDto(0L, 0L, 0L);
    }
}