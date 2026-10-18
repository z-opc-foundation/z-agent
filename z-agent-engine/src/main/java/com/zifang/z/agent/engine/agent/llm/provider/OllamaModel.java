package com.zifang.z.agent.engine.agent.llm.provider;

import com.zifang.util.json.JsonCursor;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.engine.agent.llm.model.define.*;
import com.zifang.z.agent.engine.agent.llm.tool.core.ToolSchema;
import okhttp3.*;
import okio.BufferedSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ollama 本地模型实现（OpenAI 风格对外 Model 接口）。
 *
 * <p>对齐 z-agent-engine 内 OllamaProvider / OpenAIProvider 的 OkHttp + JSON 风格：
 * <ul>
 *     <li>{@link #chat(List)} 同步调用 {@code POST /api/chat}</li>
 *     <li>{@link #generate(List)} 同步调用 {@code POST /api/generate}</li>
 *     <li>{@link #stream(List, List, ModelOptions)} 异步调用 {@code POST /api/chat}（NDJSON 流）</li>
 * </ul>
 *
 * <p>注：{@link Model#stream(ModelRequest)} 的入参不展开，仅透传到上述 list 重载；
 * 该入口为占位，等 {@code ModelRequest} 字段稳定后补齐。
 */
public class OllamaModel implements Model {

    private static final Logger log = LogManager.getLogger(OllamaModel.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    private final OkHttpClient httpClient;
    private String baseUrl;
    private String modelName;

    public OllamaModel() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.baseUrl = DEFAULT_BASE_URL;
    }

    public OllamaModel(String baseUrl, String modelName) {
        this();
        if (StringUtils.isNotBlank(baseUrl)) {
            this.baseUrl = baseUrl;
        }
        if (StringUtils.isNotBlank(modelName)) {
            this.modelName = modelName;
        }
    }

    public void init(LlmCallerConfig config) {
        if (config == null) {
            return;
        }
        if (StringUtils.isNotBlank(config.getBaseUrl())) {
            this.baseUrl = config.getBaseUrl();
        }
        if (StringUtils.isNotBlank(config.getModelName())) {
            this.modelName = config.getModelName();
        }
    }

    public void setBaseUrl(String baseUrl) {
        if (StringUtils.isNotBlank(baseUrl)) {
            this.baseUrl = baseUrl;
        }
    }

    public void setModelName(String modelName) {
        if (StringUtils.isNotBlank(modelName)) {
            this.modelName = modelName;
        }
    }

    @Override
    public String identity() {
        return "OllamaModel(" + baseUrl + "/" + modelName + ")";
    }

    // ────────────────────────── stream 占位 ──────────────────────────

    @Override
    public Flux<ModelResponse> stream(ModelRequest modelRequest) {
        // 透传到 list 重载；ModelRequest 字段未来稳定后再补解析
        List<ModelMessage> messages = modelRequest == null
                ? java.util.Collections.<ModelMessage>emptyList()
                : modelRequest.getMessages();
        List<ToolSchema> tools = modelRequest == null
                ? java.util.Collections.<ToolSchema>emptyList()
                : modelRequest.getTools();
        ModelOptions options = modelRequest == null ? new ModelOptions() : modelRequest.getOptions();
        return stream(messages, tools, options);
    }

    @Override
    public Flux<ModelResponse> stream(List<ModelMessage> messages, List<ToolSchema> tools, ModelOptions options) {
        return Flux.create(sink -> streamNdjson(messages, tools, options, sink));
    }

    /**
     * Ollama 原生 NDJSON 流（非 SSE）：每行一个 JSON 对象，{@code done:true} 终止。
     * 这里把 content 片段逐步推送，最后一个对象标记流结束。
     */
    private void streamNdjson(List<ModelMessage> messages,
                              List<ToolSchema> tools,
                              ModelOptions options,
                              FluxSink<ModelResponse> sink) {
        OllamaChatRequest req;
        try {
            req = toChatRequest(messages, tools, options);
        } catch (IOException e) {
            sink.error(e);
            return;
        }
        req.setStream(true);

        RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(req), JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .post(body)
                .build();

        Call call = httpClient.newCall(httpRequest);
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
                        String err = r.body() == null ? "" : r.body().string();
                        sink.error(new IOException("Ollama stream failed: " + r.code()
                                + " body=" + err));
                        return;
                    }
                    try (BufferedSource source = r.body().source()) {
                        StringBuilder contentBuf = new StringBuilder();
                        String line;
                        while ((line = source.readUtf8Line()) != null) {
                            if (sink.isCancelled()) {
                                return;
                            }

                            try {
                                JsonCursor node = JsonCursor.of(JsonUtil.parse(line));
                                JsonCursor msg = node.path("message");
                                String chunk = msg.path("content").asText();
                                if (!chunk.isEmpty()) {
                                    contentBuf.append(chunk);
                                    ModelResponse mr = new ModelResponse();
                                    mr.setContent(chunk);
                                    sink.next(mr);
                                }
                                if (node.path("done").asBoolean(false)) {
                                    break;
                                }
                            } catch (Exception ex) {
                                log.debug("Skip ollama stream chunk: {}", ex.getMessage());
                            }
                        }
                        if (contentBuf.length() == 0) {
                            // 即便没拿到任何 chunk，也得发一个终止信号避免下游挂住
                            sink.next(new ModelResponse());
                        }
                        sink.complete();
                    }
                } catch (Exception e) {
                    sink.error(e);
                }
            }
        });
    }

    // ────────────────────────── sync /api/chat ──────────────────────────

    @Override
    public String chat(List<ModelMessage> modelMessages) {
        OllamaChatRequest req;
        try {
            req = toChatRequest(modelMessages, java.util.Collections.<ToolSchema>emptyList(), null);
        } catch (IOException e) {
            throw new RuntimeException("build ollama chat request failed", e);
        }
        req.setStream(false);

        RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(req), JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            String respBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new RuntimeException("Ollama chat failed: " + response.code()
                        + " body=" + respBody);
            }
            JsonCursor root = JsonCursor.of(JsonUtil.parse(respBody));
            JsonCursor msg = root.path("message");
            String content = msg.path("content").asText();
            if (content.isEmpty()) {
                throw new RuntimeException("Ollama chat returned empty content: " + respBody);
            }
            return content;
        } catch (IOException e) {
            throw new RuntimeException("call ollama chat failed", e);
        }
    }

    // ────────────────────────── sync /api/generate ──────────────────────────

    @Override
    public String generate(List<ModelMessage> modelMessages) {
        String prompt = adapter(modelMessages);

        OllamaGenerateRequest req = new OllamaGenerateRequest();
        req.setModel(modelName);
        req.setPrompt(prompt);
        req.setStream(false);
        req.setOptions(new OllamaOptions(0.1, 4096));

        RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(req), JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/generate")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            String respBody = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new RuntimeException("Ollama generate failed: " + response.code()
                        + " body=" + respBody);
            }
            JsonCursor root = JsonCursor.of(JsonUtil.parse(respBody));
            return root.path("response").asText();
        } catch (IOException e) {
            throw new RuntimeException("call ollama generate failed", e);
        }
    }

    // ────────────────────────── request DTO ──────────────────────────

    private OllamaChatRequest toChatRequest(List<ModelMessage> messages,
                                            List<ToolSchema> tools,
                                            ModelOptions options) throws IOException {
        OllamaChatRequest req = new OllamaChatRequest();
        req.setModel(modelName);

        List<OllamaMessage> omsgs = new ArrayList<>();
        for (ModelMessage m : messages) {
            OllamaMessage om = new OllamaMessage();
            om.setRole(m.getRole());
            om.setContent(m.getContent());
            omsgs.add(om);
        }
        req.setMessages(omsgs);

        if (options != null) {
            req.setOptions(new OllamaOptions(options));
        }
        return req;
    }

    private String adapter(List<ModelMessage> modelMessages) {
        StringBuilder sb = new StringBuilder();
        for (ModelMessage m : modelMessages) {
            sb.append(m.getRole() == null ? "" : m.getRole())
                    .append(":")
                    .append(m.getContent() == null ? "" : m.getContent())
                    .append('\n');
        }
        return sb.toString();
    }

    // ────────────────────────── inner DTO ──────────────────────────

    private static class OllamaChatRequest {
        private String model;
        private List<OllamaMessage> messages;
        private boolean stream;
        private OllamaOptions options;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<OllamaMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<OllamaMessage> messages) {
            this.messages = messages;
        }

        public boolean isStream() {
            return stream;
        }

        public void setStream(boolean stream) {
            this.stream = stream;
        }

        public OllamaOptions getOptions() {
            return options;
        }

        public void setOptions(OllamaOptions options) {
            this.options = options;
        }
    }

    private static class OllamaGenerateRequest {
        private String model;
        private String prompt;
        private boolean stream;
        private OllamaOptions options;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public boolean isStream() {
            return stream;
        }

        public void setStream(boolean stream) {
            this.stream = stream;
        }

        public OllamaOptions getOptions() {
            return options;
        }

        public void setOptions(OllamaOptions options) {
            this.options = options;
        }
    }

    private static class OllamaMessage {
        private String role;
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    private static class OllamaOptions {
        private Double temperature;
        private Integer numPredict;

        public OllamaOptions() {
        }

        public OllamaOptions(Double temperature, Integer numPredict) {
            this.temperature = temperature;
            this.numPredict = numPredict;
        }

        public OllamaOptions(ModelOptions options) {
            this(options.getTemperature(), options.getMaxTokens());
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getNumPredict() {
            return numPredict;
        }

        public void setNumPredict(Integer numPredict) {
            this.numPredict = numPredict;
        }
    }
}