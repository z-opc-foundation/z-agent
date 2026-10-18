package com.zifang.z.agent.llm.agent.turn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single agent turn — the processing of one user input through the ReAct loop.
 */
public class TurnRun {

    private final String id;
    private final List<?> messages;
    private final long startedAt;
    private long finishedAt;

    public TurnRun(String id, List<?> messages) {
        this.id = id;
        this.messages = messages != null ? new ArrayList<>(messages) : Collections.emptyList();
        this.startedAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public List<?> getMessages() {
        return messages;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public void markFinished() {
        this.finishedAt = System.currentTimeMillis();
    }
}
