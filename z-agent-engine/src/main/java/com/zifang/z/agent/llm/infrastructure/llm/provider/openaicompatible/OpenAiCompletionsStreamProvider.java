package com.zifang.z.agent.llm.infrastructure.llm.provider.openaicompatible;

import com.zifang.util.json.JsonCursor;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonArray;
import com.zifang.util.json.model.JsonObject;
import com.zifang.z.agent.llm.infrastructure.llm.provider.define.LlmStreamProvider;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OpenAI-compatible LLM stream provider — 真发 SSE 流式请求到 minimaxi.com / hermes 等 OpenAI 兼容网关。
 *
 * <p>协议：OpenAI Chat Completions streaming（POST {baseUrl}/chat/completions, Authorization: Bearer xxx, stream=true）。
 * 响应格式：{@code data: {json}\n\n} 增量块，{@code [DONE]} 表示结束。
 *
 * <p>stream() 同步阻塞直到 LLM 流结束（onClosed / onFailure / readTimeout）：
 * <ul>
 *   <li>逐 token 通过 callbacks.onToken 推回（delta.content 经 {@link UpstreamThinkingTagSplitter}
 *       切分：正文段直推，思考段加 [REASONING:] 前缀推；delta.reasoning_content 同样加 [REASONING:] 前缀推）</li>
 *   <li>正常结束通过 callbacks.onComplete(fullText) 报告</li>
 *   <li>中途错误通过 callbacks.onError 报告，并解除阻塞</li>
 * </ul>
 */
public class OpenAiCompletionsStreamProvider implements LlmStreamProvider {

    private static final Logger log = LogManager.getLogger(OpenAiCompletionsStreamProvider.class);

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 单次 stream() 阻塞上限：30 秒（防止 onFailure 不触发时永久卡死）。
     */
    private static final long STREAM_AWAIT_SECONDS = 30L;

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;

    public OpenAiCompletionsStreamProvider(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(STREAM_AWAIT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        // 排查 key 截断/污染：打印 apiKey 来源长度 + 前 12 后 8 字符（避免泄露完整 key）
        if (apiKey != null) {
            int len = apiKey.length();
            String prefix = len > 12 ? apiKey.substring(0, 12) : apiKey;
            String suffix = len > 8 ? apiKey.substring(len - 8) : "";
            log.info("[OpenAI-Compat] init baseUrl={} model={} apiKey.len={} prefix={} suffix={}",
                    baseUrl, model, len, prefix, suffix);
        } else {
            log.warn("[OpenAI-Compat] init baseUrl={} model={} apiKey=NULL", baseUrl, model);
        }
    }

    private static String safeBodySnippet(Response resp) {
        try {
            if (resp == null || resp.body() == null) {
                return "";
            }

            return resp.peekBody(512).string();
        } catch (Exception ignore) {
            return "";
        }
    }

    @Override
    public void stream(String prompt, String systemPrompt, StreamCallbacks callbacks) {
        String url = buildChatCompletionsUrl();
        String body = buildRequestBody(prompt, systemPrompt);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .post(RequestBody.create(body, JSON))
                .build();

        StringBuilder accumulated = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failureRef = new AtomicReference<>();

        // ⚠ 每条流一个实例（splitter 有状态：跨增量维持「思考态」与被切开的标签残片）
        UpstreamThinkingTagSplitter thinkingSplitter = new UpstreamThinkingTagSplitter();

        EventSources.createFactory(httpClient).newEventSource(request,
                new EventSourceListener() {
                    @Override
                    public void onOpen(EventSource eventSource, Response response) {
                        int status = response != null ? response.code() : -1;
                        log.info("[OpenAI-Compat] SSE opened url={} model={} status={}",
                                url, model, status);
                        // OkHttp EventSource 不会把 4xx/5xx 视为 SSE 错误（只调 onOpen、不调 onFailure），
                        // 中间协议错误必须在这里主动 cancel + countDown done，否则会卡到 readTimeout=180s。
                        if (status >= 400) {
                            String bodySnippet = safeBodySnippet(response);
                            log.warn("[OpenAI-Compat] non-2xx status on open, cancel immediately: HTTP {} body={}",
                                    status, bodySnippet);
                            String errMsg = "HTTP " + status + " body=" + bodySnippet;
                            failureRef.set(new RuntimeException(errMsg));
                            if (callbacks != null) {
                                callbacks.onError(new RuntimeException(errMsg));
                            }
                            eventSource.cancel();
                            done.countDown();
                        }
                    }

                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        if (data == null || data.isEmpty()) {
                            return;
                        }
                        if ("[DONE]".equals(data.trim())) {
                            return;
                        }
                        try {
                            JsonCursor root = JsonCursor.of(JsonUtil.parse(data));
                            JsonCursor choices = root.path("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonCursor delta = choices.at(0).path("delta");
                                JsonCursor contentNode = delta.path("content");
                                if (!contentNode.isMissing() && contentNode.value() instanceof String) {
                                    String token = contentNode.asText();
                                    if (!token.isEmpty()) {
                                        // ⚠ 上游可能把思考过程以**裸标签**塞进正文
                                        //   （实测 MiniMax-M2.7：content = "<think>…</think>\n\n收到"）
                                        //   ⇒ 思考被当正文推给前端 ⇒ 主群直接显示思考过程。
                                        // 这里用 splitter 把思考分流走既有 [REASONING:] 协议，
                                        // 与「上游提供独立 reasoning_content 字段」走同一条路（不新增协议）。
                                        UpstreamThinkingTagSplitter.Split split = thinkingSplitter.split(token);
                                        if (split.hasText()) {
                                            accumulated.append(split.getText());
                                            if (callbacks != null) {
                                                callbacks.onToken(split.getText());
                                            }
                                        }
                                        if (split.hasReasoning() && callbacks != null) {
                                            callbacks.onToken("[REASONING:]" + split.getReasoning());
                                        }
                                    }
                                }
                                // reasoning_content（minimaxi/M2.7 等模型）：加 [REASONING:] 前缀推回，
                                // 由上层 LlmReactEngine / FtStreamMarkerTextSupport 路由到 onThought。
                                JsonCursor reasoningNode = delta.path("reasoning_content");
                                if (!reasoningNode.isMissing() && reasoningNode.value() instanceof String) {
                                    String rt = reasoningNode.asText();
                                    if (!rt.isEmpty()) {
                                        if (callbacks != null) {
                                            callbacks.onToken("[REASONING:]" + rt);
                                        }
                                    }
                                }
                            }
                        } catch (Exception parseErr) {
                            log.warn("[OpenAI-Compat] failed to parse SSE chunk: {} data={}",
                                    parseErr.toString(), data.length() > 200 ? data.substring(0, 200) + "..." : data);
                        }
                    }

                    @Override
                    public void onClosed(EventSource eventSource) {
                        log.info("[OpenAI-Compat] SSE closed, accumulatedLen={}", accumulated.length());
                        // 收尾：把被切开的标签残片交还（否则尾部残片会静默丢失）
                        UpstreamThinkingTagSplitter.Split tail = thinkingSplitter.flush();
                        if (tail.hasText()) {
                            accumulated.append(tail.getText());
                            if (callbacks != null) {
                                callbacks.onToken(tail.getText());
                            }
                        }
                        if (tail.hasReasoning() && callbacks != null) {
                            callbacks.onToken("[REASONING:]" + tail.getReasoning());
                        }
                        if (failureRef.get() == null && callbacks != null) {
                            callbacks.onComplete(accumulated.toString());
                        }
                        done.countDown();
                    }

                    @Override
                    public void onFailure(EventSource eventSource, Throwable t, Response response) {
                        String msg = t != null ? t.toString()
                                : (response != null ? "HTTP " + response.code() + " body="
                                + safeBodySnippet(response) : "unknown error");
                        log.warn("[OpenAI-Compat] SSE failure: {}", msg);
                        Throwable err = t != null ? t : new RuntimeException(msg);
                        failureRef.set(err);
                        if (callbacks != null) {
                            callbacks.onError(err);
                        }
                        done.countDown();
                    }
                });

        // 同步等待 SSE 关闭 / 失败 / 超时
        try {
            if (!done.await(STREAM_AWAIT_SECONDS + 30, TimeUnit.SECONDS)) {
                log.warn("[OpenAI-Compat] stream() timed out waiting for SSE close");
                if (callbacks != null) {
                    callbacks.onError(new RuntimeException("stream await timed out after "
                            + (STREAM_AWAIT_SECONDS + 30) + "s"));
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            if (callbacks != null) {
                callbacks.onError(ie);
            }
        }
    }

    @Override
    public void stop() {
        // 当前实现未持久化 EventSource 引用；调用方一般通过 Thread.interrupt 取消。
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    private String buildChatCompletionsUrl() {
        String u = baseUrl == null ? "" : baseUrl.trim();
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        if (u.endsWith("/chat/completions")) {
            return u;
        }
        return u + "/chat/completions";
    }

    private String buildRequestBody(String prompt, String systemPrompt) {
        JsonObject root = new JsonObject();
        root.put("model", model);
        root.put("stream", true);
        root.put("temperature", 0.7);

        JsonArray messages = new JsonArray();
        root.put("messages", messages);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject sys = new JsonObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            messages.add(sys);
        }
        JsonObject user = new JsonObject();
        user.put("role", "user");
        user.put("content", prompt == null ? "" : prompt);
        messages.add(user);

        return root.toString();
    }
}
