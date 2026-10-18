package com.zifang.z.agent.llm.gateway;

import com.zifang.z.agent.llm.infrastructure.llm.context.LlmStreamRequest;

import java.util.Collections;
import java.util.List;

/**
 * OpenAI-compatible implementation of ZAgentLlmService.
 */
public class OpenAICompatibleZAgentLlmService implements ZAgentLlmService {

    private final ZAgentLlmProperties properties;

    public OpenAICompatibleZAgentLlmService(ZAgentLlmProperties properties) {
        this.properties = properties;
    }

    @Override
    public String chat(String messages, LlmStreamRequest request) {
        // Stub: in production, would call OpenAI-compatible API
        return "[OpenAI-compatible LLM Response] " + messages;
    }

    @Override
    public void stream(String messages, LlmStreamRequest request, StreamListener listener) {
        // Stub
        if (listener != null) {
            listener.onToken("[OpenAI-compatible LLM Stream] ");
            listener.onComplete("[OpenAI-compatible LLM Stream] complete");
        }
    }

    @Override
    public List<String> listModelCodes() {
        return Collections.singletonList(properties.getDefaultModel());
    }
}
