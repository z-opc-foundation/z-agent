package com.zifang.z.agent.llm.gateway;

import com.zifang.z.agent.llm.infrastructure.llm.context.LlmStreamRequest;

/**
 * Service interface for ZAgent LLM operations.
 */
public interface ZAgentLlmService {

    /**
     * Send a chat request and get a synchronous response.
     */
    String chat(String messages, LlmStreamRequest request);

    /**
     * Send a streaming chat request.
     */
    void stream(String messages, LlmStreamRequest request, StreamListener listener);

    /**
     * Get available model codes.
     */
    java.util.List<String> listModelCodes();

    /**
     * Stream listener.
     */
    interface StreamListener {
        void onToken(String token);

        void onComplete(String fullText);

        void onError(Throwable error);
    }
}
