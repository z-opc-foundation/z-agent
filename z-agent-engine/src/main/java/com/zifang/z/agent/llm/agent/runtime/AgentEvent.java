package com.zifang.z.agent.llm.agent.runtime;

/**
 * Agent runtime event — used by z-team extensions (CompactionExtension, StepBudgetExtension).
 */
public class AgentEvent {

    private Type type;
    private int step;
    private String text;
    private Throwable error;
    // ===== Compaction event (used by CompactionExtension) =====
    private int evictedCount;
    private int archiveSize;
    private boolean compressed;

    public AgentEvent() {
    }

    public AgentEvent(Type type) {
        this.type = type;
    }

    public static AgentEvent compaction(int evictedCount, int archiveSize, boolean compressed) {
        AgentEvent e = new AgentEvent(Type.TEXT);
        e.evictedCount = evictedCount;
        e.archiveSize = archiveSize;
        e.compressed = compressed;
        return e;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public int getEvictedCount() {
        return evictedCount;
    }

    public int getArchiveSize() {
        return archiveSize;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public enum Type {
        STEP_START, TOOL_CALL, TOOL_RESULT, TEXT, DONE, ERROR
    }
}
