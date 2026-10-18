package com.zifang.z.agent.engine.agent.stream;

import com.zifang.z.agent.engine.agent.observe.AgentTracer;
import com.zifang.z.agent.engine.agent.observe.NoOpAgentTracer;
import com.zifang.z.agent.engine.agent.react.ReActAgent;
import com.zifang.z.agent.engine.agent.react.StreamEvent;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支持流式输出的 ReActAgent。
 * <p>
 * 在原有 StreamEvent 基础上，额外：
 * - 订阅 llmClient.streamChatFlux() 的每个 Chunk，实时发出 STREAMING_TEXT 事件
 * - 发出 METADATA_EVENT（token 计数、耗时）
 * - 发出 AGENT_STATUS 状态变化
 */
public class StreamingReActAgent implements StreamingAgent {
    private final ReActAgent delegate;
    private final String providerCode;
    private final AgentTracer tracer;

    public StreamingReActAgent(ReActAgent delegate, String providerCode, AgentTracer tracer) {
        this.delegate = delegate;
        this.providerCode = providerCode;
        this.tracer = tracer != null ? tracer : new NoOpAgentTracer();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public Flux<AgentEvent> streamEvents(String input) {
        return Flux.create(sink -> {
            sink.next(new AgentEvent.AgentStatus("RUNNING", "Agent started"));

            delegate.stream(input)
                    .doOnNext(se -> {
                        // 转换 StreamEvent -> AgentEvent
                        AgentEvent ae = convert(se);
                        if (ae != null) {
                            sink.next(ae);
                        }

                    })
                    .doOnError(err -> {
                        sink.next(new AgentEvent.AgentStatus("ERROR", err.getMessage()));
                        sink.error(err);
                    })
                    .doOnTerminate(() -> {
                        sink.next(new AgentEvent.AgentStatus("IDLE", "Agent finished"));
                        sink.complete();
                    })
                    .subscribe();
        });
    }

    private AgentEvent convert(StreamEvent se) {
        if (se instanceof StreamEvent.ThoughtDelta) {
            return new AgentEvent.StreamingText(
                    ((StreamEvent.ThoughtDelta) se).text, true, "thought");
        }
        if (se instanceof StreamEvent.FinalDelta) {
            return new AgentEvent.StreamingText(
                    ((StreamEvent.FinalDelta) se).text, true, "final");
        }
        if (se instanceof StreamEvent.ToolResult) {
            StreamEvent.ToolResult tr = (StreamEvent.ToolResult) se;
            return new AgentEvent.StreamingText(
                    tr.result != null ? tr.result.toString() : "", true, "tool_result");
        }
        if (se instanceof StreamEvent.Done) {
            StreamEvent.Done d = (StreamEvent.Done) se;
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("total_steps", d.totalSteps);
            meta.put("reply_length", d.reply.length());
            if (d.promptTokens != null) {
                meta.put("prompt_tokens", d.promptTokens);
            }

            if (d.completionTokens != null) {
                meta.put("completion_tokens", d.completionTokens);
            }

            return new AgentEvent.Metadata(meta);
        }
        // STEP_START, TOOL_CALL_REQUEST, ERROR — 返回 null（由 delegate 直接 emit）
        return null;
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void reset() {
        delegate.reset();
    }
}