package com.zifang.z.agent.llm.gateway;

import com.zifang.z.agent.llm.infrastructure.llm.context.LlmStreamRequest;

/**
 * Utility class for ZAgent LLM operations.
 */
public class ZAgentLlmUtil {

    /**
     * Build a default LlmStreamRequest with the given LLM code.
     */
    public static LlmStreamRequest defaultRequest(String llmCode) {
        return new LlmStreamRequest(llmCode);
    }

    /**
     * Build an LlmStreamRequest with custom options.
     */
    public static LlmStreamRequest request(String llmCode, double temperature, int maxTokens) {
        LlmStreamRequest.StreamOptions opts = new LlmStreamRequest.StreamOptions();
        opts.setTemperature(temperature);
        opts.setMaxTokens(maxTokens);
        return new LlmStreamRequest(llmCode, opts);
    }

    /**
     * Simple string formatting utility.
     */
    public static String formatModelKey(String provider, String model) {
        return provider + ":" + model;
    }
}
