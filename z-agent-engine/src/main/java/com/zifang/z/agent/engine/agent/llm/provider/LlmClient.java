package com.zifang.z.agent.engine.agent.llm.provider;

import com.zifang.z.agent.engine.agent.llm.model.*;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM Client that manages multiple providers and supports all LLM parameters.
 * <p>
 * Two execution modes:
 * - chat(...)         blocking call returning a complete ChatResult
 * - streamChat(...)   blocking call returning accumulated StreamResult (kept for back-compat)
 * - streamChatFlux(...) reactive stream of {@link StreamDelta} events
 */
public class LlmClient {

    private static final Logger log = LogManager.getLogger(LlmClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Map<String, LlmProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, LlmProviderConfig> providerConfigs = new ConcurrentHashMap<>();
    private final OkHttpClient httpClient;

    public LlmClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        registerProvider(new OllamaProvider());
        registerProvider(new OpenAIProvider());
        registerProvider(new MiniMaxProvider());
        registerProvider(new SparkProvider());
    }

    /**
     * Register a provider.
     */
    public void registerProvider(LlmProvider provider) {
        providers.put(provider.getProviderCode(), provider);
        log.info("Registered LLM provider: {}", provider.getProviderCode());
    }

    /**
     * Configure a provider with base URL and API key.
     */
    public void configureProvider(String providerCode, String baseUrl, String apiKey) {
        LlmProviderConfig config = new LlmProviderConfig();
        config.setProviderCode(providerCode);
        config.setBaseUrl(baseUrl);
        config.setApiKey(apiKey);
        providerConfigs.put(providerCode, config);

        LlmProvider provider = providers.get(providerCode);
        if (provider instanceof OllamaProvider) {
            ((OllamaProvider) provider).setBaseUrl(baseUrl);
        } else if (provider instanceof OpenAIProvider) {
            ((OpenAIProvider) provider).setBaseUrl(baseUrl);
            ((OpenAIProvider) provider).setApiKey(apiKey);
        } else if (provider instanceof MiniMaxProvider) {
            ((MiniMaxProvider) provider).setBaseUrl(baseUrl);
            ((MiniMaxProvider) provider).setApiKey(apiKey);
        } else if (provider instanceof SparkProvider) {
            ((SparkProvider) provider).setBaseUrl(baseUrl);
            ((SparkProvider) provider).setApiKey(apiKey);
        }
    }

    // ===== Basic chat methods =====

    public ChatResult chat(String providerCode, String model, List<ChatMessage> messages) throws IOException {
        return chat(providerCode, ChatCompletionsRequest.builder()
                .model(model)
                .messages(messages)
                .build());
    }

    public ChatResult chat(String providerCode, String model, List<ChatMessage> messages,
                           List<OpenAITool> tools) throws IOException {
        return chat(providerCode, ChatCompletionsRequest.builder()
                .model(model)
                .messages(messages)
                .tools(tools)
                .build());
    }

    public ChatResult chat(String providerCode, ChatCompletionsRequest request) throws IOException {
        LlmProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerCode);
        }
        ChatCompletionsResponse response = provider.chat(request);
        return toChatResult(response);
    }

    // ===== Stream chat (blocking, legacy) =====

    /**
     * Blocking stream call. Collects all deltas into one StreamResult.
     * Kept for back-compat; prefer {@link #streamChatFlux(String, ChatCompletionsRequest)}.
     */
    public StreamResult streamChat(String providerCode, String model, List<ChatMessage> messages) throws IOException {
        return streamChat(providerCode, ChatCompletionsRequest.builder()
                .model(model)
                .messages(messages)
                .stream(true)
                .build());
    }

    public StreamResult streamChat(String providerCode, ChatCompletionsRequest request) throws IOException {
        LlmProvider provider = providers.get(providerCode);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerCode);
        }
        return provider.streamChat(request);
    }

    // ===== Stream chat (reactive, real streaming) =====

    /**
     * Real streaming API. Emits one {@link StreamDelta} per server chunk.
     * Backpressure-aware via {@link Flux#create}.
     */
    public Flux<StreamDelta> streamChatFlux(String providerCode, String model, List<ChatMessage> messages) {
        return streamChatFlux(providerCode, ChatCompletionsRequest.builder()
                .model(model)
                .messages(messages)
                .stream(true)
                .build());
    }

    public Flux<StreamDelta> streamChatFlux(String providerCode, ChatCompletionsRequest request) {
        LlmProvider provider = providers.get(providerCode);
        if (provider == null) {
            return Flux.error(new IllegalArgumentException("Unknown provider: " + providerCode));
        }
        return provider.streamChatFlux(request);
    }

    private ChatResult toChatResult(ChatCompletionsResponse response) {
        ChatResult result = new ChatResult();
        result.setId(response.getId());
        result.setContent(response.getFirstContent());
        result.setToolCalls(response.getToolCalls());
        result.setFinishReason(response.getFinishReason());
        if (response.getUsage() != null) {
            result.setPromptTokens(response.getUsage().getPromptTokens());
            result.setCompletionTokens(response.getUsage().getCompletionTokens());
            result.setTotalTokens(response.getUsage().getTotalTokens());
        }
        return result;
    }

    public Set<String> getAvailableProviders() {
        return providers.keySet();
    }

    private static class LlmProviderConfig {
        private String providerCode;
        private String baseUrl;
        private String apiKey;

        public String getProviderCode() {
            return providerCode;
        }

        public void setProviderCode(String providerCode) {
            this.providerCode = providerCode;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    /**
     * Chat result with tool call support.
     */
    public static class ChatResult {
        private String id;
        private String content;
        private List<ToolCall> toolCalls;
        private String finishReason;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<ToolCall> getToolCalls() {
            return toolCalls;
        }

        public void setToolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    /**
     * Single streaming delta. Emitted per server chunk in real streaming mode.
     * <p>
     * Semantics:
     * - First delta of a stream: {@code index=0, content="", toolCalls=null, done=false}.
     * - Intermediate deltas: {@code content} is the new text fragment of this chunk;
     * {@code content} does NOT contain previously emitted text. Accumulate externally if needed.
     * - Terminal delta: {@code done=true, finishReason} set (e.g. "stop" / "tool_calls" / "length").
     */
    public static class StreamDelta {
        private final String id;
        private final String content;
        private final String finishReason;
        private final boolean done;
        private final List<ToolCall> toolCalls;
        private final Integer promptTokens;
        private final Integer completionTokens;

        public StreamDelta(String id, String content, String finishReason, boolean done,
                           List<ToolCall> toolCalls, Integer promptTokens, Integer completionTokens) {
            this.id = id;
            this.content = content;
            this.finishReason = finishReason;
            this.done = done;
            this.toolCalls = toolCalls;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }

        public static StreamDelta chunk(String id, String textChunk) {
            return new StreamDelta(id, textChunk, null, false, null, null, null);
        }

        public static StreamDelta terminal(String id, String finishReason, List<ToolCall> toolCalls,
                                           Integer promptTokens, Integer completionTokens) {
            return new StreamDelta(id, null, finishReason, true, toolCalls, promptTokens, completionTokens);
        }

        public String getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public boolean isDone() {
            return done;
        }

        public List<ToolCall> getToolCalls() {
            return toolCalls;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    /**
     * Legacy blocking stream result. Prefer {@link StreamDelta} for real streaming.
     * Kept for back-compat with existing tests / call sites.
     */
    public static class StreamResult {
        private String id;
        private String content;
        private String finishReason;
        private boolean done;
        private List<ToolCall> toolCalls;
        private Integer promptTokens;
        private Integer completionTokens;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void appendContent(String delta) {
            if (this.content == null) {
                this.content = "";
            }

            this.content += delta;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }

        public List<ToolCall> getToolCalls() {
            return toolCalls;
        }

        public void setToolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }
    }
}
