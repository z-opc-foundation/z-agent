package com.zifang.z.agent.engine.agent.multi;

import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsRequest;
import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsResponse;
import com.zifang.z.agent.engine.agent.llm.model.ChatMessage;
import com.zifang.z.agent.engine.agent.llm.model.ToolCall;
import com.zifang.z.agent.engine.agent.llm.provider.LlmClient;
import com.zifang.z.agent.engine.agent.llm.provider.LlmProvider;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用于测试: 总是返回固定的 (可选回声) 文本, 不连任何真实 LLM。
 * 每个实例的 providerCode 唯一 (默认 "stub" + 序号), 避免覆盖。
 */
public class StubLlmProvider implements LlmProvider {

    private static final java.util.concurrent.atomic.AtomicLong SEQ =
            new java.util.concurrent.atomic.AtomicLong(0);
    /**
     * 收到的请求计数器 (供测试断言)
     */
    public final AtomicInteger callCount = new AtomicInteger(0);
    private final String providerCode;
    /**
     * 锁定 response, 每次请求都返回
     */
    private volatile String cannedResponse;
    /**
     * 锁定 tool calls, 每次请求都返回
     */
    private volatile List<ToolCall> cannedToolCalls;

    public StubLlmProvider() {
        this("stub-" + SEQ.incrementAndGet());
    }

    public StubLlmProvider(String code) {
        this.providerCode = code;
    }

    public StubLlmProvider respondWith(String text) {
        this.cannedResponse = text;
        return this;
    }

    public StubLlmProvider respondWithToolCalls(List<ToolCall> tcs) {
        this.cannedToolCalls = tcs;
        return this;
    }

    @Override
    public String getProviderCode() {
        return providerCode;
    }

    @Override
    public String getDefaultBaseUrl() {
        return "stub://localhost";
    }

    private ChatCompletionsResponse buildResponse(ChatCompletionsRequest req) {
        callCount.incrementAndGet();
        ChatCompletionsResponse resp = new ChatCompletionsResponse();
        resp.setId("stub-resp-" + callCount.get());
        ChatCompletionsResponse.ChatChoice choice = new ChatCompletionsResponse.ChatChoice();
        choice.setIndex(0);
        ChatMessage msg = new ChatMessage();
        msg.setRole("assistant");
        if (cannedResponse != null) {
            msg.setContent(cannedResponse);
        } else {
            // 默认: 把最后一条 user message 回显
            String echoed = "(stub echo)";
            if (req != null && req.getMessages() != null && !req.getMessages().isEmpty()) {
                ChatMessage last = req.getMessages().get(req.getMessages().size() - 1);
                if (last != null && last.getContent() != null) {
                    String c = last.getContent();
                    if (c.length() > 200) c = c.substring(c.length() - 200);

                    echoed = c;
                }
            }
            msg.setContent(echoed);
        }
        if (cannedToolCalls != null) {
            msg.setToolCalls(new ArrayList<>(cannedToolCalls));
        }
        choice.setMessage(msg);
        choice.setFinishReason(cannedToolCalls != null ? "tool_calls" : "stop");
        resp.setChoices(java.util.Collections.singletonList(choice));
        return resp;
    }

    @Override
    public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
        return buildResponse(request);
    }

    @Override
    public Flux<LlmClient.StreamDelta> streamChatFlux(ChatCompletionsRequest request) {
        ChatCompletionsResponse r = buildResponse(request);
        String id = r.getId();
        ChatCompletionsResponse.ChatChoice c0 = r.getChoices().get(0);
        String content = c0.getMessage().getContent() == null ? "" : c0.getMessage().getContent();
        String finish = c0.getFinishReason();
        List<ToolCall> tcs = c0.getMessage().getToolCalls();
        return Flux.just(
                LlmClient.StreamDelta.chunk(id, content),
                LlmClient.StreamDelta.terminal(id, finish, tcs, null, null)
        );
    }
}
