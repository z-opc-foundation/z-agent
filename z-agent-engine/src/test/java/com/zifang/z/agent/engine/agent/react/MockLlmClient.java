package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.model.*;
import com.zifang.z.agent.engine.agent.llm.provider.LlmClient;
import com.zifang.z.agent.engine.agent.llm.provider.LlmProvider;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Programmable LlmClient test double supporting both blocking chat() and real
 * streaming chatFlux(). Pattern modeled after agentscope-java's MockModel.
 * <p>
 * Three construction styles:
 * - withText / withToolCall / withFullFlow       (one-shot scripted responses)
 * - enqueue() chained calls                      (response queue)
 * - setGenerator(fn)                             (custom per-call lambda)
 * <p>
 * Plus:
 * - withError(msg)                               (injects a runtime failure)
 * - enqueueStreamDeltas(...)                     (raw Flux<StreamDelta> scripted)
 * - record: getCallCount / getLastRequest / getLastProviderCode
 */
public class MockLlmClient extends LlmClient {

    private final List<ChatResult> queue = new ArrayList<>();
    private final List<Flux<LlmClient.StreamDelta>> streamQueue = new ArrayList<>();
    private final List<ChatCompletionsRequest> recorded = new ArrayList<>();
    private final List<String> recordedProviders = new ArrayList<>();
    private final AtomicInteger callCount = new AtomicInteger(0);
    private final AtomicInteger streamCallCount = new AtomicInteger(0);
    private ResponseGenerator generator;
    private StreamResponseGenerator streamGenerator;
    private boolean throwError;
    private String errorMessage = "mock error";
    private boolean exhaustedReturnsLast = true;
    private ChatResult exhaustedFallback;
    private Flux<LlmClient.StreamDelta> exhaustedStreamFallback;

    public MockLlmClient() {
        super();
        // Replace the default ollama/openai providers with this mock instance so that
        // LlmClient.streamChatFlux("ollama", ...) / chat("ollama", ...) routes here
        // instead of hitting the real network. This is what makes the mock
        // "intercept" every call regardless of providerCode the agent was built with.
        registerProvider(new MockProvider());
    }

    public static MockLlmClient withText(String text) {
        MockLlmClient m = new MockLlmClient();
        m.queue.add(textResult(text));
        m.streamQueue.add(singleChunkStream(text, "stop", null));
        return m;
    }

    public static MockLlmClient withToolCall(String toolName, String argsJson) {
        MockLlmClient m = new MockLlmClient();
        m.queue.add(toolCallResult(toolName, argsJson));
        m.streamQueue.add(singleChunkStream("", "tool_calls",
                Collections.singletonList(new ToolCall("call_" + UUID.randomUUID(), toolName, argsJson))));
        return m;
    }

    /**
     * think-text + tool_call + final-answer, all delivered as a multi-chunk stream.
     */
    public static MockLlmClient withFullFlow(String think, String toolName, String argsJson, String finalAnswer) {
        MockLlmClient m = new MockLlmClient();
        m.queue.add(thinkAndToolCallResult(think, toolName, argsJson));
        m.queue.add(textResult(finalAnswer));
        m.streamQueue.add(Flux.concat(
                Flux.just(LlmClient.StreamDelta.chunk("mock", think)),
                Flux.just(LlmClient.StreamDelta.terminal("mock", "tool_calls",
                        Collections.singletonList(new ToolCall("call_" + UUID.randomUUID(), toolName, argsJson)),
                        10, 20))));
        m.streamQueue.add(Flux.concat(
                Flux.just(LlmClient.StreamDelta.chunk("mock", finalAnswer)),
                Flux.just(LlmClient.StreamDelta.terminal("mock", "stop", null, 5, 10))));
        return m;
    }

    // ==================== Factory ====================

    public static ChatResult textResult(String content) {
        ChatResult r = new ChatResult();
        r.setId("mock-" + UUID.randomUUID());
        r.setContent(content);
        r.setFinishReason("stop");
        r.setPromptTokens(10);
        r.setCompletionTokens(20);
        r.setTotalTokens(30);
        return r;
    }

    public static ChatResult thinkResult(String think) {
        ChatResult r = new ChatResult();
        r.setId("mock-think-" + UUID.randomUUID());
        r.setContent(think);
        r.setFinishReason("stop");
        return r;
    }

    public static ChatResult toolCallResult(String toolName, String argsJson) {
        ToolCall tc = new ToolCall("call_" + UUID.randomUUID(), toolName, argsJson);
        ChatResult r = new ChatResult();
        r.setId("mock-tool-" + UUID.randomUUID());
        r.setToolCalls(Collections.singletonList(tc));
        r.setFinishReason("tool_calls");
        r.setContent("");
        return r;
    }

    // ==================== Build scripts after construction ====================

    public static ChatResult thinkAndToolCallResult(String think, String toolName, String argsJson) {
        ToolCall tc = new ToolCall("call_" + UUID.randomUUID(), toolName, argsJson);
        ChatResult r = new ChatResult();
        r.setId("mock-think-tool-" + UUID.randomUUID());
        r.setContent(think);
        r.setToolCalls(Collections.singletonList(tc));
        r.setFinishReason("tool_calls");
        return r;
    }

    public static ChatResult nullResult() {
        return null;
    }

    public static Flux<LlmClient.StreamDelta> singleChunkStream(String content, String finishReason, List<ToolCall> toolCalls) {
        return Flux.concat(
                Flux.just(LlmClient.StreamDelta.chunk("mock", content == null ? "" : content)),
                Flux.just(LlmClient.StreamDelta.terminal("mock", finishReason, toolCalls, 1, 2)));
    }

    /**
     * Build a multi-chunk stream from a list of text fragments, then a terminal stop.
     */
    public static Flux<LlmClient.StreamDelta> wordByWordStream(String... words) {
        Flux<LlmClient.StreamDelta> chunks = Flux.fromArray(words)
                .map(w -> LlmClient.StreamDelta.chunk("mock", w));
        Flux<LlmClient.StreamDelta> terminal = Flux.just(LlmClient.StreamDelta.terminal("mock", "stop", null, 1, 1));
        return Flux.concat(chunks, terminal);
    }

    public MockLlmClient enqueueText(String text) {
        queue.add(textResult(text));
        streamQueue.add(singleChunkStream(text, "stop", null));
        return this;
    }

    public MockLlmClient enqueueToolCall(String toolName, String argsJson) {
        queue.add(toolCallResult(toolName, argsJson));
        streamQueue.add(singleChunkStream("", "tool_calls",
                Collections.singletonList(new ToolCall("call_" + UUID.randomUUID(), toolName, argsJson))));
        return this;
    }

    public MockLlmClient enqueueFinal(String text) {
        queue.add(textResult(text));
        streamQueue.add(singleChunkStream(text, "stop", null));
        return this;
    }

    public MockLlmClient enqueue(ChatResult r) {
        queue.add(r);
        // 对应 stream 形式：text 或 tool_call
        if (r == null) {
            // chat() 路径返回 null（模拟"LLM 拿不到结果"）；
            // 对应流式路径发一个 error Flux，让 ReActAgent 走 ErrorEvent 分支。
            streamQueue.add(Flux.error(new IOException("MockLlmClient: scripted null ChatResult")));
        } else if (r.hasToolCalls()) {
            streamQueue.add(singleChunkStream(r.getContent() == null ? "" : r.getContent(), "tool_calls", r.getToolCalls()));
        } else {
            streamQueue.add(singleChunkStream(r.getContent() == null ? "" : r.getContent(), "stop", null));
        }
        return this;
    }

    /**
     * Enqueue a stream of deltas for the next streamChatFlux() call.
     */
    public MockLlmClient enqueueStreamDeltas(LlmClient.StreamDelta... deltas) {
        streamQueue.add(Flux.fromArray(deltas));
        return this;
    }

    public MockLlmClient enqueueStreamDeltas(List<LlmClient.StreamDelta> deltas) {
        streamQueue.add(Flux.fromIterable(deltas));
        return this;
    }

    public MockLlmClient setGenerator(ResponseGenerator g) {
        this.generator = g;
        return this;
    }

    // ==================== Response builders ====================

    public MockLlmClient setStreamGenerator(StreamResponseGenerator g) {
        this.streamGenerator = g;
        return this;
    }

    public MockLlmClient withError(String msg) {
        this.throwError = true;
        this.errorMessage = msg;
        return this;
    }

    public MockLlmClient withExhaustedFallback(ChatResult r) {
        this.exhaustedReturnsLast = false;
        this.exhaustedFallback = r;
        return this;
    }

    public MockLlmClient withExhaustedStreamFallback(Flux<LlmClient.StreamDelta> f) {
        this.exhaustedReturnsLast = false;
        this.exhaustedStreamFallback = f;
        return this;
    }

    public int getCallCount() {
        return recorded.size();
    }

    public int getStreamCallCount() {
        return streamCallCount.get();
    }

    public ChatCompletionsRequest getLastRequest() {
        return recorded.isEmpty() ? null : recorded.get(recorded.size() - 1);
    }

    // ==================== Records / asserts ====================

    public List<ChatMessage> getLastMessages() {
        ChatCompletionsRequest r = getLastRequest();
        return r == null ? Collections.emptyList() : r.getMessages();
    }

    public List<OpenAITool> getLastTools() {
        ChatCompletionsRequest r = getLastRequest();
        return r == null ? Collections.emptyList() : r.getTools();
    }

    public List<ChatCompletionsRequest> getAllRequests() {
        return Collections.unmodifiableList(recorded);
    }

    public List<String> getAllProviderCodes() {
        return Collections.unmodifiableList(recordedProviders);
    }

    public String getLastProviderCode() {
        return recordedProviders.isEmpty() ? null : recordedProviders.get(recordedProviders.size() - 1);
    }

    public void resetRecorder() {
        recorded.clear();
        recordedProviders.clear();
        callCount.set(0);
        streamCallCount.set(0);
    }

    /**
     * Mock intercepts chat() so any call (regardless of providerCode) goes through
     * the scripted response queue. This means the agent can be wired with
     * .provider("ollama") and the mock still answers.
     */
    @Override
    public ChatResult chat(String providerCode, ChatCompletionsRequest request) throws IOException {
        recorded.add(request);
        recordedProviders.add(providerCode);
        int idx = callCount.getAndIncrement();

        if (throwError) {
            throw new IOException("MockLlmClient injected error: " + errorMessage);
        }
        if (generator != null) {
            return generator.generate(request.getMessages(),
                    request.getTools() == null ? Collections.emptyList() : request.getTools(),
                    idx);
        }
        if (idx < queue.size()) {
            return queue.get(idx);
        }
        if (exhaustedReturnsLast && !queue.isEmpty()) {
            return queue.get(queue.size() - 1);
        }
        return exhaustedFallback;
    }

    /**
     * Mock intercepts streamChatFlux() likewise. Always returns the mock's
     * scripted stream (or generator), bypassing the real Ollama/OpenAI providers.
     */
    @Override
    public Flux<LlmClient.StreamDelta> streamChatFlux(String providerCode, ChatCompletionsRequest request) {
        recorded.add(request);
        recordedProviders.add(providerCode);
        int idx = streamCallCount.getAndIncrement();

        if (throwError) {
            return Flux.error(new IOException("MockLlmClient injected stream error: " + errorMessage));
        }
        if (streamGenerator != null) {
            return streamGenerator.generate(request.getMessages(),
                    request.getTools() == null ? Collections.emptyList() : request.getTools(),
                    idx);
        }
        if (idx < streamQueue.size()) {
            return streamQueue.get(idx);
        }
        if (exhaustedReturnsLast && !streamQueue.isEmpty()) {
            return streamQueue.get(streamQueue.size() - 1);
        }
        if (exhaustedStreamFallback != null) {
            return exhaustedStreamFallback;
        }

        return Flux.empty();
    }

    public interface ResponseGenerator {
        ChatResult generate(List<ChatMessage> messages, List<OpenAITool> tools, int callIndex);
    }

    // ==================== Internal: overrides ====================

    public interface StreamResponseGenerator {
        Flux<LlmClient.StreamDelta> generate(List<ChatMessage> messages, List<OpenAITool> tools, int callIndex);
    }

    /**
     * Adapter: LlmProvider facade in front of the enclosing MockLlmClient state.
     */
    private final class MockProvider implements LlmProvider {
        @Override
        public String getProviderCode() {
            return "ollama";
        }

        @Override
        public String getDefaultBaseUrl() {
            return "mock://" + System.identityHashCode(this);
        }

        @Override
        public ChatCompletionsResponse
        chat(ChatCompletionsRequest request) throws java.io.IOException {
            // Reuse the same scripted path as a real LlmClient.chat("ollama", req) would,
            // then wrap the ChatResult into a ChatCompletionsResponse for the provider API.
            LlmClient.ChatResult cr = MockLlmClient.super.chat("ollama", request);
            ChatCompletionsResponse resp =
                    new ChatCompletionsResponse();
            resp.setId(cr.getId());
            ChatCompletionsResponse.ChatChoice c =
                    new ChatCompletionsResponse.ChatChoice();
            c.setIndex(0);
            c.setFinishReason(cr.getFinishReason() == null ? "stop" : cr.getFinishReason());
            ChatMessage m =
                    new ChatMessage();
            m.setRole("assistant");
            m.setContent(cr.getContent() == null ? "" : cr.getContent());
            m.setToolCalls(cr.getToolCalls());
            c.setMessage(m);
            resp.setChoices(java.util.Collections.singletonList(c));
            if (cr.getPromptTokens() != null || cr.getCompletionTokens() != null) {
                ChatCompletionsResponse.Usage u =
                        new ChatCompletionsResponse.Usage(
                                cr.getPromptTokens(), cr.getCompletionTokens(),
                                cr.getTotalTokens());
                resp.setUsage(u);
            }
            return resp;
        }

        @Override
        public reactor.core.publisher.Flux<LlmClient.StreamDelta>
        streamChatFlux(ChatCompletionsRequest request) {
            // Delegate to the enclosing mock's overridden streamChatFlux()
            return MockLlmClient.this.streamChatFlux("ollama", request);
        }
    }
}
