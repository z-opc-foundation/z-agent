package com.zifang.z.agent.engine.agent.llm.provider;

import com.zifang.util.json.JsonCursor;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.engine.agent.llm.model.*;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ollama provider implementation.
 */
public class OllamaProvider implements LlmProvider {

    private static final Logger log = LogManager.getLogger(OllamaProvider.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    private final OkHttpClient httpClient;
    private String baseUrl;

    public OllamaProvider() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.baseUrl = DEFAULT_BASE_URL;
    }

    public OllamaProvider(String baseUrl) {
        this();
        if (StringUtils.isNotBlank(baseUrl)) {
            this.baseUrl = baseUrl;
        }
    }

    @Override
    public String getProviderCode() {
        return "ollama";
    }

    @Override
    public String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    @Override
    public ChatCompletionsResponse chat(ChatCompletionsRequest request) throws IOException {
        OllamaChatRequest ollamaRequest = toOllamaRequest(request);

        RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(ollamaRequest), JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama request failed: " + response);
            }
            String responseBody = response.body().string();
            return fromOllamaResponse(responseBody, request.getModel());
        }
    }

    /**
     * Real streaming: each NDJSON line becomes one {@link LlmClient.StreamDelta}.
     * Ollama's "done" line carries usage stats; tool_calls come on terminal line.
     */
    @Override
    public Flux<LlmClient.StreamDelta> streamChatFlux(ChatCompletionsRequest request) {
        return Flux.create(sink -> {
            OllamaChatRequest ollamaRequest;
            try {
                ollamaRequest = toOllamaRequest(request);
            } catch (IOException e) {
                sink.error(e);
                return;
            }
            ollamaRequest.setStream(true);

            RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(ollamaRequest), JSON);
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
                public void onResponse(Call call, Response response) {
                    try (Response r = response) {
                        if (!r.isSuccessful()) {
                            sink.error(new IOException("Ollama stream failed: " + r));
                            return;
                        }
                        MediaType contentType = r.body().contentType();
                        if (contentType != null && contentType.toString().contains("text/event-stream")) {
                            streamSse(sink, r);
                        } else {
                            // Non-streaming response: single-chunk fallback
                            String responseBody = r.body().string();
                            ChatCompletionsResponse chatResp = fromOllamaResponse(responseBody, request.getModel());
                            String content = chatResp.getFirstContent() == null ? "" : chatResp.getFirstContent();
                            sink.next(LlmClient.StreamDelta.chunk(chatResp.getId(), content));
                            sink.next(LlmClient.StreamDelta.terminal(
                                    chatResp.getId(),
                                    chatResp.getFinishReason() == null ? "stop" : chatResp.getFinishReason(),
                                    chatResp.getToolCalls(),
                                    chatResp.getUsage() != null ? chatResp.getUsage().getPromptTokens() : null,
                                    chatResp.getUsage() != null ? chatResp.getUsage().getCompletionTokens() : null));
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

    private void streamSse(FluxSink<LlmClient.StreamDelta> sink, Response r) throws IOException {
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
                        streamId = "chatcmpl-" + System.currentTimeMillis();
                    }
                    JsonCursor messageNode = node.path("message");
                    String content = messageNode.path("content").asText();
                    if (!content.isEmpty()) {
                        contentBuf.append(content);
                        sink.next(LlmClient.StreamDelta.chunk(streamId, content));
                    }
                    JsonCursor tcs = messageNode.path("tool_calls");
                    if (tcs.isArray() && tcs.size() > 0) {
                        if (toolCalls == null) {
                            toolCalls = new ArrayList<>();
                        }

                        for (JsonCursor t : tcs.elements()) {
                            String tcid = JsonCursorSupport.textOrNull(t.path("id"));
                            JsonCursor fn = t.path("function");
                            String tname = JsonCursorSupport.textOrNull(fn.path("name"));
                            JsonCursor argsNode = fn.path("arguments");
                            String targs = JsonCursorSupport.isTextual(argsNode) ? argsNode.asText()
                                    : (argsNode.isMissing() || argsNode.isNull() ? "{}"
                                    : JsonUtil.toJsonExcludingNulls(argsNode.value()));
                            toolCalls.add(new ToolCall(tcid, tname, targs));
                        }
                    }
                    if (node.path("done").asBoolean(false)) {
                        finishReason = "stop";
                        JsonCursor statsNode = node.path("prompt_eval_count");
                        if (!statsNode.isMissing()) {
                            promptTokens = statsNode.asInt(0);
                        }
                        JsonCursor evalCountNode = node.path("eval_count");
                        if (!evalCountNode.isMissing()) {
                            completionTokens = evalCountNode.asInt(0);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Skip ollama stream chunk: {}", e.getMessage());
                }
            }
            sink.next(LlmClient.StreamDelta.terminal(
                    streamId,
                    finishReason == null ? "stop" : finishReason,
                    toolCalls,
                    promptTokens,
                    completionTokens));
        }
    }

    private OllamaChatRequest toOllamaRequest(ChatCompletionsRequest request) throws IOException {
        OllamaChatRequest ollamaRequest = new OllamaChatRequest();
        ollamaRequest.setModel(request.getModel());

        List<OllamaMessage> ollamaMessages = new java.util.ArrayList<>();
        for (ChatMessage msg : request.getMessages()) {
            OllamaMessage ollamaMsg = new OllamaMessage();
            ollamaMsg.setRole(msg.getRole());
            ollamaMsg.setContent(msg.getContent());
            // Forward assistant tool_calls back to Ollama so it can continue a tool-calling conversation.
            if (msg.getRole() != null && "assistant".equals(msg.getRole())
                    && msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                ollamaMsg.setToolCalls(toOllamaToolCalls(msg.getToolCalls()));
            }
            ollamaMessages.add(ollamaMsg);
        }
        ollamaRequest.setMessages(ollamaMessages);

        if (request.getTemperature() != null) {
            ollamaRequest.setOptions(new OllamaOptions(request));
        }

        // Translate OpenAI-style tool definitions into Ollama format.
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            ollamaRequest.setTools(toOllamaTools(request.getTools()));
        }

        return ollamaRequest;
    }

    private List<OllamaTool> toOllamaTools(List<OpenAITool> openAiTools) {
        List<OllamaTool> out = new java.util.ArrayList<>();
        for (OpenAITool t : openAiTools) {
            OllamaTool ot = new OllamaTool();
            ot.setType("function");
            OllamaToolFunction fn = new OllamaToolFunction();
            String name = null;
            String description = null;
            Map<String, Object> parametersMap = null;
            Object fnObj = t.getFunction();
            if (fnObj instanceof OpenAITool.OpenAIToolFunction) {
                OpenAITool.OpenAIToolFunction f =
                        (OpenAITool.OpenAIToolFunction) fnObj;
                name = f.getName();
                description = f.getDescription();
                parametersMap = f.getParameters();
            } else if (fnObj instanceof OpenAITool.FunctionDef) {
                OpenAITool.FunctionDef f =
                        (OpenAITool.FunctionDef) fnObj;
                name = f.getName();
                description = f.getDescription();
                parametersMap = f.getParameters();
            }
            fn.setName(name);
            fn.setDescription(description);
            // Ollama expects parameters as a JSON object.
            fn.setParameters(parametersMap);
            ot.setFunction(fn);
            out.add(ot);
        }
        return out;
    }

    private Object parseJsonObject(String json) {
        if (json == null || json.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        try {
            return JsonUtil.parse(json);
        } catch (RuntimeException e) {
            log.warn("Failed to parse tool parameters as JSON object, fallback to empty: {}", e.getMessage());
            return java.util.Collections.emptyMap();
        }
    }

    private List<OllamaToolCall> toOllamaToolCalls(List<ToolCall> toolCalls) throws IOException {
        List<OllamaToolCall> out = new java.util.ArrayList<>();
        for (ToolCall tc : toolCalls) {
            OllamaToolCall otc = new OllamaToolCall();
            otc.setId(tc.getId());
            OllamaToolCallFunction f = new OllamaToolCallFunction();
            f.setIndex(otc.getId() == null ? 0 : Math.abs(otc.getId().hashCode()) % 1000);
            if (tc.getFunction() != null) {
                f.setName(tc.getFunction().getName());
                String args = tc.getFunction().getArguments();
                f.setArguments(args == null || args.isEmpty() ? new java.util.HashMap<>()
                        : JsonUtil.parse(args));
            }
            otc.setFunction(f);
            out.add(otc);
        }
        return out;
    }

    private ChatCompletionsResponse fromOllamaResponse(String responseBody, String model) throws IOException {
        JsonCursor root = JsonCursor.of(JsonUtil.parse(responseBody));
        ChatCompletionsResponse resp = new ChatCompletionsResponse();
        resp.setId("chatcmpl-" + System.currentTimeMillis());
        resp.setObject("chat.completion");
        resp.setCreated(System.currentTimeMillis() / 1000);
        resp.setModel(model);

        ChatCompletionsResponse.ChatChoice choice = new ChatCompletionsResponse.ChatChoice();
        choice.setIndex(0);

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setRole("assistant");

        JsonCursor messageNode = root.path("message");
        String content = messageNode.path("content").asText();
        assistantMsg.setContent(content);

        // Ollama returns message.tool_calls as an array; translate to OpenAI-shaped ToolCall.
        List<ToolCall> toolCalls = parseOllamaToolCalls(messageNode.path("tool_calls"));
        if (!toolCalls.isEmpty()) {
            assistantMsg.setToolCalls(toolCalls);
            choice.setFinishReason("tool_calls");
        } else {
            choice.setFinishReason(root.path("done").asBoolean(false) ? "stop" : "tool_calls");
        }

        choice.setMessage(assistantMsg);
        resp.setChoices(java.util.Collections.singletonList(choice));

        ChatCompletionsResponse.Usage usage = new ChatCompletionsResponse.Usage();
        usage.setPromptTokens(root.path("prompt_eval_count").asInt(0));
        usage.setCompletionTokens(root.path("eval_count").asInt(0));
        usage.setTotalTokens(usage.getPromptTokens() + usage.getCompletionTokens());
        resp.setUsage(usage);

        return resp;
    }

    private List<ToolCall> parseOllamaToolCalls(JsonCursor toolCallsNode) {
        List<ToolCall> out = new java.util.ArrayList<>();
        if (toolCallsNode == null || !toolCallsNode.isArray()) {
            return out;
        }
        for (JsonCursor tc : toolCallsNode.elements()) {
            String id = JsonCursorSupport.textOrNull(tc.path("id"));
            JsonCursor fn = tc.path("function");
            String name = JsonCursorSupport.textOrNull(fn.path("name"));
            JsonCursor argsNode = fn.path("arguments");
            String argsJson;
            if (argsNode == null || argsNode.isNull()) {
                argsJson = "{}";
            } else if (JsonCursorSupport.isTextual(argsNode)) {
                // OpenAI-style: arguments is a JSON-encoded string.
                argsJson = argsNode.asText();
            } else {
                // Ollama native: arguments is a JSON object. Serialize back to string.
                argsJson = JsonUtil.toJsonExcludingNulls(argsNode.value());
            }
            out.add(new ToolCall(id, name, argsJson));
        }
        return out;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private static class OllamaChatRequest {
        private String model;
        private List<OllamaMessage> messages;
        private boolean stream;
        private OllamaOptions options;
        private List<OllamaTool> tools;
        private Object format; // optional, e.g. "json"

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

        public List<OllamaTool> getTools() {
            return tools;
        }

        public void setTools(List<OllamaTool> tools) {
            this.tools = tools;
        }

        public Object getFormat() {
            return format;
        }

        public void setFormat(Object format) {
            this.format = format;
        }
    }

    private static class OllamaMessage {
        private String role;
        private String content;
        private List<OllamaToolCall> toolCalls;

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

        public List<OllamaToolCall> getToolCalls() {
            return toolCalls;
        }

        public void setToolCalls(List<OllamaToolCall> toolCalls) {
            this.toolCalls = toolCalls;
        }
    }

    private static class OllamaTool {
        private String type = "function";
        private OllamaToolFunction function;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public OllamaToolFunction getFunction() {
            return function;
        }

        public void setFunction(OllamaToolFunction function) {
            this.function = function;
        }
    }

    private static class OllamaToolFunction {
        private String name;
        private String description;
        // Ollama wants parameters as a JSON object; we use Object so Jackson serializes a tree.
        private Object parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Object getParameters() {
            return parameters;
        }

        public void setParameters(Object parameters) {
            this.parameters = parameters;
        }
    }

    private static class OllamaToolCall {
        private String id;
        private OllamaToolCallFunction function;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public OllamaToolCallFunction getFunction() {
            return function;
        }

        public void setFunction(OllamaToolCallFunction function) {
            this.function = function;
        }
    }

    private static class OllamaToolCallFunction {
        private int index;
        private String name;
        private Object arguments;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getArguments() {
            return arguments;
        }

        public void setArguments(Object arguments) {
            this.arguments = arguments;
        }
    }

    private static class OllamaOptions {
        private Double temperature;
        private Double topP;
        private Integer topK;
        private Double repeatPenalty;
        private Long seed;
        private Integer numCtx;
        private Integer maxTokens;
        private List<String> stop;

        public OllamaOptions() {
        }

        public OllamaOptions(ChatCompletionsRequest request) {
            this.temperature = request.getTemperature();
            this.topP = request.getTopP();
            this.topK = request.getTopK();
            this.repeatPenalty = request.getRepeatPenalty();
            this.seed = request.getSeed();
            this.numCtx = request.getNumCtx();
            this.maxTokens = request.getMaxTokens();
            this.stop = request.getStop();
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Double getTopP() {
            return topP;
        }

        public void setTopP(Double topP) {
            this.topP = topP;
        }

        public Integer getTopK() {
            return topK;
        }

        public void setTopK(Integer topK) {
            this.topK = topK;
        }

        public Double getRepeatPenalty() {
            return repeatPenalty;
        }

        public void setRepeatPenalty(Double repeatPenalty) {
            this.repeatPenalty = repeatPenalty;
        }

        public Long getSeed() {
            return seed;
        }

        public void setSeed(Long seed) {
            this.seed = seed;
        }

        public Integer getNumCtx() {
            return numCtx;
        }

        public void setNumCtx(Integer numCtx) {
            this.numCtx = numCtx;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public List<String> getStop() {
            return stop;
        }

        public void setStop(List<String> stop) {
            this.stop = stop;
        }
    }
}
