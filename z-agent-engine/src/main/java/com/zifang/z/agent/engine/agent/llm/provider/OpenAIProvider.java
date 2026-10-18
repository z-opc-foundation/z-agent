package com.zifang.z.agent.engine.agent.llm.provider;

import com.zifang.util.json.JsonCursor;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsRequest;
import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsResponse;
import com.zifang.z.agent.engine.agent.llm.model.ToolCall;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI provider implementation with real streaming via SSE.
 */
public class OpenAIProvider implements LlmProvider {

    private static final Logger log = LogManager.getLogger(OpenAIProvider.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    private final OkHttpClient httpClient;
    private String baseUrl;
    private String apiKey;

    public OpenAIProvider() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.baseUrl = DEFAULT_BASE_URL;
    }

    public OpenAIProvider(String baseUrl, String apiKey) {
        this();
        if (StringUtils.isNotBlank(baseUrl)) {
            this.baseUrl = baseUrl;
        }
        this.apiKey = apiKey;
    }

    @Override
    public String getProviderCode() {
        return "openai";
    }

    @Override
    public String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    public ChatCompletionsResponse chat(ChatCompletionsRequest request) throws IOException {
        RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(request), JSON);
        Request.Builder builder = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .post(body)
                .addHeader("Content-Type", "application/json");

        if (StringUtils.isNotBlank(apiKey)) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI request failed: " + response);
            }
            String responseBody = response.body().string();
            return JsonUtil.fromJson(responseBody, ChatCompletionsResponse.class);
        }
    }

    /**
     * Real streaming: each SSE line becomes one {@link LlmClient.StreamDelta}.
     * The terminal delta carries finish_reason + any tool_calls + usage.
     */
    @Override
    public Flux<LlmClient.StreamDelta> streamChatFlux(ChatCompletionsRequest request) {
        return Flux.create(sink -> {
            request.setStream(true);
            RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(request), JSON);
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
                public void onResponse(Call call, Response response) {
                    try (Response r = response) {
                        if (!r.isSuccessful()) {
                            sink.error(new IOException("OpenAI stream failed: " + r));
                            return;
                        }
                        try (okio.BufferedSource source = r.body().source()) {
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

                                String jsonData = line.substring(6);
                                if ("[DONE]".equals(jsonData.trim())) {
                                    break;
                                }

                                try {
                                    JsonCursor node = JsonCursor.of(JsonUtil.parse(jsonData));
                                    if (streamId == null) {
                                        String id = JsonCursorSupport.textOrNull(node.path("id"));
                                        if (id != null) {
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
                                            sink.next(LlmClient.StreamDelta.chunk(streamId, chunk));
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
                                                toolCalls.add(new ToolCall(tcid, tname, targs == null ? "" : targs));
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

                @Override
                public void onFailure(Call call, IOException e) {
                    sink.error(e);
                }
            });
        });
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.isNotBlank(apiKey);
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
