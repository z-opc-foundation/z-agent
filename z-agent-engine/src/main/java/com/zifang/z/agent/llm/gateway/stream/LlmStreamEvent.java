package com.zifang.z.agent.llm.gateway.stream;

/**
 * Represents a single SSE event from the LLM stream.
 */
public class LlmStreamEvent {

    private final String event;
    private final String data;

    public LlmStreamEvent(String event, String data) {
        this.event = event;
        this.data = data;
    }

    public String getEvent() {
        return event;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "LlmStreamEvent{event='" + event + "', data='" + (data != null && data.length() > 50 ? data.substring(0, 50) + "..." : data) + "'}";
    }
}
