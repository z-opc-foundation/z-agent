package com.zifang.z.agent.engine.agent.llm.provider;

import com.zifang.util.json.JsonCursor;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsRequest;
import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsResponse;
import com.zifang.z.agent.engine.agent.llm.model.ToolCall;
import okhttp3.*;
import okio.BufferedSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MiniMax provider implementation (OpenAI-compatible API).
 * <p>
 * Base URL: https://api.minimax.chat/v1
 * Model: MiniMax-Text-01 / MiniMax-Text-02
 */
public class MiniMaxProvider implements LlmProvider {

    private static final Logger log = LogManager.getLogger(MiniMaxProvider.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_BASE_URL = "https://api.minimax.chat/v1";

    private final OkHttpClient httpClient;
    private String baseUrl;
    private String apiKey;

    public MiniMaxProvider() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.baseUrl = DEFAULT_BASE_URL;
    }

    public MiniMaxProvider(String baseUrl, String apiKey) {
        this();
        if (StringUtils.isNotBlank(baseUrl)) {
            this.baseUrl = baseUrl;
        }
        this.apiKey = apiKey;
    }

    public void setBaseUrl(String baseUrl) {
        if (StringUtils.isNotBlank(baseUrl)) {
            this.baseUrl = baseUrl;
        }
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderCode() {
        return "minimax";
    }

    @Override
    public String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    public ChatCompletionsResponse chat(ChatCompletionsRequest request) throws IOException {
        String jsonStr = JsonUtil.toJsonExcludingNulls(request);
        log.info("MiniMax NON-STREAM REQUEST: {}", jsonStr);
        RequestBody body = RequestBody.create(jsonStr, JSON);
        log.info("MiniMax NON-STREAM messages count: {}", request.getMessages().size());
        Request.Builder builder = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(body)
                .addHeader("Content-Type", "application/json");

        if (StringUtils.isNotBlank(apiKey)) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            String responseBody = response.body().string();
            log.info("MiniMax NON-STREAM RESPONSE: {}", responseBody);
            if (!response.isSuccessful()) {
                log.error("MiniMax error response: {}", responseBody);
                throw new IOException("MiniMax request failed: " + response.code() + " " + response.message() + " body: " + responseBody);
            }
            return JsonUtil.fromJson(responseBody, ChatCompletionsResponse.class);
        }
    }

    @Override
    public Flux<LlmClient.StreamDelta> streamChatFlux(ChatCompletionsRequest request) {
        return Flux.create(sink -> {
            request.setStream(true);
            String minimaxJsonStr = JsonUtil.toJsonExcludingNulls(request);
            log.info("MiniMax request body: {}", minimaxJsonStr);
            RequestBody body = RequestBody.create(minimaxJsonStr, JSON);
            Request.Builder builder = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .post(body)
                    .addHeader("Content-Type", "application/json");

            if (StringUtils.isNotBlank(apiKey)) {
                builder.addHeader("Authorization", "Bearer " + apiKey);
            }

            Call call = httpClient.newCall(builder.build());
            sink.onCancel(() -> {
                try {
                    call.cancel();
                } catch (Exception ignore) {
                }
            });

            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    sink.error(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (Response r = response) {
                        if (!r.isSuccessful()) {
                            String bodyStr = r.body() != null ? r.body().string() : "null";
                            log.warn("MiniMax stream failed: {} code={}, body={}", r.code(), r.message(), bodyStr);
                            sink.error(new IOException("MiniMax stream failed: " + r.code()));
                            return;
                        }
                        try (BufferedSource source = r.body().source()) {
                            String line;
                            String streamId = null;
                            StringBuilder contentBuf = new StringBuilder();
                            String finishReason = null;
                            List<ToolCall> toolCalls = null;
                            Integer promptTokens = null;
                            Integer completionTokens = null;

                            while ((line = source.readUtf8Line()) != null) {
                                if (sink.isCancelled()) {
                                    return;
                                }

                                if (!line.startsWith("data: ")) {
                                    continue;
                                }

                                String jsonData = line.substring(6).trim();
                                if ("[DONE]".equals(jsonData)) {
                                    break;
                                }

                                try {
                                    JsonCursor node = JsonCursor.of(JsonUtil.parse(jsonData));
                                    if (streamId == null) {
                                        String id = JsonCursorSupport.textOrNull(node.path("id"));
                                        if (id != null && !id.isEmpty()) {
                                            streamId = id;
                                        }
                                    }
                                    JsonCursor choices = node.path("choices");
                                    if (choices.isArray() && choices.size() > 0) {
                                        JsonCursor choice0 = choices.at(0);
                                        JsonCursor delta = choice0.path("delta");
                                        String chunk = JsonCursorSupport.textOrNull(delta.path("content"));
                                        if (chunk != null && !chunk.isEmpty()) {
                                            contentBuf.append(chunk);
                                        }
                                        String reason = JsonCursorSupport.textOrNull(choice0.path("finish_reason"));
                                        if (reason != null && !reason.isEmpty() && !"null".equals(reason)) {
                                            finishReason = reason;
                                        }
                                        JsonCursor tcs = delta.path("tool_calls");
                                        if (tcs.isArray() && tcs.size() > 0) {
                                            if (toolCalls == null) {
                                                toolCalls = new ArrayList<>();
                                            }

                                            for (JsonCursor t : tcs.elements()) {
                                                String tcid = JsonCursorSupport.textOrNull(t.path("id"));
                                                JsonCursor fn = t.path("function");
                                                String tname = JsonCursorSupport.textOrNull(fn.path("name"));
                                                String targs = JsonCursorSupport.textOrNull(fn.path("arguments"));
                                                toolCalls.add(new ToolCall(tcid, tname, targs == null ? "{}" : targs));
                                            }
                                        }
                                    }
                                    JsonCursor usageNode = node.path("usage");
                                    if (!usageNode.isMissing() && usageNode.size() > 0) {
                                        if (promptTokens == null) {
                                            promptTokens = usageNode.path("prompt_tokens").asInt(0);
                                        }
                                        completionTokens = usageNode.path("completion_tokens").asInt(0);
                                    }
                                } catch (Exception ex) {
                                    log.debug("Skip stream chunk: {}", ex.getMessage());
                                }
                            }
                            // Emit accumulated content as a final chunk if any
                            if (contentBuf.length() > 0) {
                                sink.next(LlmClient.StreamDelta.chunk(streamId, contentBuf.toString()));
                            }
                            log.debug("MiniMax terminal: finishReason={}, toolCalls={}", finishReason, toolCalls);
                            sink.next(LlmClient.StreamDelta.terminal(
                                    streamId,
                                    finishReason == null ? "stop" : finishReason,
                                    toolCalls,
                                    promptTokens,
                                    completionTokens));
                        }
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    }
                }
            });
        });
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.isNotBlank(apiKey);
    }
}