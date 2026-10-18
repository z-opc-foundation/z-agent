package com.zifang.z.agent.llm.builtins.extensions.compaction;

/**
 * Compaction event — fired when message compaction occurs.
 */
public class CompactionEvent {

    private final String phase;
    private final String source;
    private final int evictedCount;
    private final int remainingVisible;
    private final boolean usedLlm;
    private final int summaryChars;
    private final String error;

    public CompactionEvent(String phase, String source, int evictedCount, int remainingVisible,
                           boolean usedLlm, int summaryChars, String error) {
        this.phase = phase;
        this.source = source;
        this.evictedCount = evictedCount;
        this.remainingVisible = remainingVisible;
        this.usedLlm = usedLlm;
        this.summaryChars = summaryChars;
        this.error = error;
    }

    public String getPhase() {
        return phase;
    }

    public String getSource() {
        return source;
    }

    public int getEvictedCount() {
        return evictedCount;
    }

    public int getRemainingVisible() {
        return remainingVisible;
    }

    public boolean isUsedLlm() {
        return usedLlm;
    }

    public int getSummaryChars() {
        return summaryChars;
    }

    public String getError() {
        return error;
    }
}
