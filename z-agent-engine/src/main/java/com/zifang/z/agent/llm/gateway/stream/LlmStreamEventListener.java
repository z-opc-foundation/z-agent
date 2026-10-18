package com.zifang.z.agent.llm.gateway.stream;

/**
 * Stream event listener for LLM SSE connections.
 */
public interface LlmStreamEventListener {

    void onEvent(LlmStreamEvent event);

    void onComplete();

    void onError(Throwable error);
}
