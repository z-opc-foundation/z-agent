package com.zifang.z.agent.llm.infrastructure.llm.context;

/**
 * Cancel signal — allows callers to cancel an in-flight LLM call.
 */
public class CancelSignal {

    private volatile boolean cancelled;

    public CancelSignal() {
        this.cancelled = false;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isSet() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public void reset() {
        this.cancelled = false;
    }
}
