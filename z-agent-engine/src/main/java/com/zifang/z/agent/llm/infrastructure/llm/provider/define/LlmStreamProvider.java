package com.zifang.z.agent.llm.infrastructure.llm.provider.define;

import com.zifang.z.agent.llm.infrastructure.llm.events.LlmProviderStreamEvents;

/**
 * LLM stream provider — abstracts the streaming protocol for different LLM backends.
 */
public interface LlmStreamProvider {

    /**
     * Start a stream call.
     */
    void stream(String prompt, String systemPrompt, StreamCallbacks callbacks);

    /**
     * Stop a running stream.
     */
    void stop();

    /**
     * Stream callbacks.
     */
    interface StreamCallbacks {
        void onToken(String token);

        void onComplete(String fullText);

        void onError(Throwable error);
    }

    /**
     * Listener for provider stream events.
     */
    interface LlmProviderStreamListener {
        void onEvent(LlmProviderStreamEvents event);
    }
}
