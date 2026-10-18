package com.zifang.z.agent.engine.agent.stream;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent 事件流中的增强事件类型。
 * 在 StreamEvent 基础上增加：
 * - METADATA_EVENT：token 计数、执行时间等元数据
 * - STREAMING_TEXT：逐 token/逐字的文本流
 */
public abstract class AgentEvent {
    private final LocalDateTime timestamp = LocalDateTime.now();

    public abstract Kind kind();

    public LocalDateTime timestamp() {
        return timestamp;
    }

    public enum Kind {
        // 继承 StreamEvent.Kind（7种）
        STEP_START,
        THOUGHT_DELTA,
        TOOL_CALL_REQUEST,
        TOOL_RESULT,
        FINAL_DELTA,
        DONE,
        ERROR,
        // 扩展
        STREAMING_TEXT,    // 逐字文本流（用于前端逐字渲染）
        METADATA_EVENT,    // token/耗时等元数据
        AGENT_STATUS       // Agent 运行状态变化
    }

    // ===== StreamingText =====
    public static final class StreamingText extends AgentEvent {
        public final String text;
        public final boolean isDelta;       // true=增量，false=完整文本
        public final String contentType;    // "thought" / "final" / "tool_result"

        public StreamingText(String text, boolean isDelta, String contentType) {
            this.text = text;
            this.isDelta = isDelta;
            this.contentType = contentType;
        }

        @Override
        public Kind kind() {
            return Kind.STREAMING_TEXT;
        }
    }

    // ===== Metadata =====
    public static final class Metadata extends AgentEvent {
        public final Map<String, Object> data;

        public Metadata(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        public Kind kind() {
            return Kind.METADATA_EVENT;
        }
    }

    // ===== AgentStatus =====
    public static final class AgentStatus extends AgentEvent {
        public final String status; // IDLE / RUNNING / WAITING_TOOL / DONE / ERROR
        public final String message;

        public AgentStatus(String status, String message) {
            this.status = status;
            this.message = message;
        }

        @Override
        public Kind kind() {
            return Kind.AGENT_STATUS;
        }
    }
}