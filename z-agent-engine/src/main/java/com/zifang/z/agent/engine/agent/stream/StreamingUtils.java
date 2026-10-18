package com.zifang.z.agent.engine.agent.stream;

import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式工具类。
 * 提供 SSE（Server-Sent Events）格式转换等实用功能。
 */
public class StreamingUtils {

    /**
     * 将 Flux<AgentEvent> 转换为 SSE 格式的字符串流。
     * 每个事件格式：data: {"kind":"...", "data":...}\n\n
     */
    public static Flux<String> toSSE(Flux<AgentEvent> events) {
        return events.map(e -> {
            String kind = e.kind().name().toLowerCase();
            String data = eventToJson(e);
            return "data: {\"kind\":\"" + kind + "\",\"data\":" + data + "}\n\n";
        });
    }

    /**
     * 从 AgentEvent 中提取文本内容（用于普通文本流）
     */
    public static Flux<String> toTextStream(Flux<AgentEvent> events) {
        return events
                .filter(e -> e instanceof AgentEvent.StreamingText)
                .map(e -> ((AgentEvent.StreamingText) e).text);
    }

    /**
     * 收集所有 STREAMING_TEXT 事件，汇总为完整文本后发出。
     */
    public static Flux<String> collectText(Flux<AgentEvent> events) {
        AtomicReference<StringBuilder> buf = new AtomicReference<>(new StringBuilder());
        return Flux.create(sink -> {
            events.subscribe(
                    e -> {
                        if (e instanceof AgentEvent.StreamingText) {
                            buf.get().append(((AgentEvent.StreamingText) e).text);
                        }
                        if (e instanceof AgentEvent.Metadata) {
                            // Metadata event signals end of streaming, output collected text
                            String collected = buf.getAndSet(new StringBuilder()).toString();
                            if (!collected.isEmpty()) {
                                sink.next(collected);
                            }
                            sink.complete();
                        }
                    },
                    sink::error,
                    sink::complete
            );
        });
    }

    private static String eventToJson(AgentEvent e) {
        if (e instanceof AgentEvent.StreamingText) {
            AgentEvent.StreamingText s = (AgentEvent.StreamingText) e;
            return "{\"text\":\"" + escapeJson(s.text) + "\",\"contentType\":\"" + s.contentType + "\"}";
        }
        if (e instanceof AgentEvent.Metadata) {
            return "{}"; // 简化，实际应序列化 data
        }
        if (e instanceof AgentEvent.AgentStatus) {
            AgentEvent.AgentStatus s = (AgentEvent.AgentStatus) e;
            return "{\"status\":\"" + s.status + "\",\"message\":\"" + escapeJson(s.message) + "\"}";
        }
        return "{}";
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }

        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}