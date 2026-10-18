package com.zifang.z.agent.llm.agent.observability;

/**
 * Turn result — the outcome of a single agent turn.
 */
public class TurnResult {

    private final String text;
    private final TokenUsage usage;

    public TurnResult(String text, TokenUsage usage) {
        this.text = text;
        this.usage = usage;
    }

    public String getText() {
        return text;
    }

    public TokenUsage getUsage() {
        return usage;
    }

    public TokenUsage tokenUsage() {
        return usage;
    }
}
