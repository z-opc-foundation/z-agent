package com.zifang.z.agent.engine.agent.llm.provider;

import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsRequest;
import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsResponse;
import com.zifang.z.agent.engine.agent.llm.model.ChatMessage;
import com.zifang.z.agent.engine.agent.llm.model.ToolCall;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

/**
 * LLM Provider interface.
 * <p>
 * Implementations must provide:
 * - chat(): non-streaming call
 * - streamChatFlux(): real streaming (one {@link LlmClient.StreamDelta} per server chunk)
 * <p>
 * Defaults are wired so an existing provider that only overrides {@code chat()}
 * still works through both {@code streamChat()} (legacy blocking) and
 * {@code streamChatFlux()} (returns the response as a single chunk).
 */
public interface LlmProvider {

    String getProviderCode();

    String getDefaultBaseUrl();

    ChatCompletionsResponse chat(ChatCompletionsRequest request) throws IOException;

    /**
     * Real streaming. Default wraps {@link #chat(ChatCompletionsRequest)} into a single-chunk
     * Flux so providers that haven't implemented true streaming still work.
     * Override to emit per-chunk deltas.
     */
    default Flux<LlmClient.StreamDelta> streamChatFlux(ChatCompletionsRequest request) {
        ChatCompletionsResponse resp;
        try {
            resp = chat(request);
        } catch (IOException e) {
            return Flux.error(e);
        }
        if (resp == null) {
            return Flux.empty();
        }
        String id = resp.getId();
        ChatCompletionsResponse.ChatChoice c0 = (resp.getChoices() == null || resp.getChoices().isEmpty())
                ? null : resp.getChoices().get(0);
        String content = c0 != null && c0.getMessage() != null
                ? (c0.getMessage().getContent() == null ? "" : c0.getMessage().getContent()) : "";
        String finish = c0 != null ? c0.getFinishReason() : "stop";
        List<ToolCall> tcs =
                c0 != null && c0.getMessage() != null ? c0.getMessage().getToolCalls() : null;
        Integer pt = resp.getUsage() != null ? resp.getUsage().getPromptTokens() : null;
        Integer ct = resp.getUsage() != null ? resp.getUsage().getCompletionTokens() : null;
        return Flux.just(
                LlmClient.StreamDelta.chunk(id, content),
                LlmClient.StreamDelta.terminal(id, finish, tcs, pt, ct)
        );
    }

    /**
     * Blocking stream (legacy). Default collects {@link #streamChatFlux} into a
     * {@link LlmClient.StreamResult}.
     */
    default LlmClient.StreamResult streamChat(ChatCompletionsRequest request) throws IOException {
        LlmClient.StreamResult acc = new LlmClient.StreamResult();
        try {
            streamChatFlux(request).doOnNext(d -> {
                if (d.getId() != null && acc.getId() == null) {
                    acc.setId(d.getId());
                }

                if (d.getContent() != null) {
                    acc.appendContent(d.getContent());
                }

                if (d.getFinishReason() != null) {
                    acc.setFinishReason(d.getFinishReason());
                }

                if (d.getToolCalls() != null) {
                    acc.setToolCalls(d.getToolCalls());
                }

                if (d.getPromptTokens() != null) {
                    acc.setPromptTokens(d.getPromptTokens());
                }

                if (d.getCompletionTokens() != null) {
                    acc.setCompletionTokens(d.getCompletionTokens());
                }

            }).blockLast();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }

            throw e;
        }
        acc.setDone(true);
        return acc;
    }

    default ChatCompletionsRequest buildRequest(String model, List<ChatMessage> messages) {
        return ChatCompletionsRequest.builder()
                .model(model)
                .messages(messages)
                .build();
    }

    default boolean isAvailable() {
        return true;
    }
}
