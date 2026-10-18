package com.zifang.z.agent.engine.agent.react;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Stream events emitted by {@link Agent#stream(String)}.
 * <p>
 * Each event is one of seven kinds (see {@link Kind}). Subscribers can filter
 * on kind to pick only the slice they care about (e.g. only FinalDelta for
 * a pure text UI).
 */
public abstract class StreamEvent {

    private final LocalDateTime timestamp = LocalDateTime.now();

    public abstract Kind kind();

    public LocalDateTime timestamp() {
        return timestamp;
    }

    public enum Kind {
        STEP_START,         // a ReAct iteration begins
        THOUGHT_DELTA,      // raw text fragment from LLM (think / tool-call narration)
        TOOL_CALL_REQUEST,  // LLM decided to call a tool
        TOOL_RESULT,        // tool execution finished
        FINAL_DELTA,        // final-answer text fragment
        DONE,               // agent finished; terminal marker
        ERROR               // unrecoverable error
    }

    // ===== Concrete event types =====

    public static final class StepStart extends StreamEvent {
        public final int step;

        public StepStart(int step) {
            this.step = step;
        }

        @Override
        public Kind kind() {
            return Kind.STEP_START;
        }

        @Override
        public String toString() {
            return "StepStart(" + step + ")";
        }
    }

    public static final class ThoughtDelta extends StreamEvent {
        public final String text;

        public ThoughtDelta(String text) {
            this.text = text == null ? "" : text;
        }

        @Override
        public Kind kind() {
            return Kind.THOUGHT_DELTA;
        }

        @Override
        public String toString() {
            return "ThoughtDelta(" + text + ")";
        }
    }

    public static final class ToolCallRequest extends StreamEvent {
        public final String name;
        public final Map<String, Object> arguments;
        public final String argumentsJson;

        public ToolCallRequest(String name, Map<String, Object> arguments, String argumentsJson) {
            this.name = name;
            this.arguments = arguments;
            this.argumentsJson = argumentsJson == null ? "{}" : argumentsJson;
        }

        @Override
        public Kind kind() {
            return Kind.TOOL_CALL_REQUEST;
        }

        @Override
        public String toString() {
            return "ToolCallRequest(" + name + ", args=" + argumentsJson + ")";
        }
    }

    public static final class ToolResult extends StreamEvent {
        public final String name;
        public final Object result;
        public final String error;
        public final boolean success;

        public ToolResult(String name, Object result, String error, boolean success) {
            this.name = name;
            this.result = result;
            this.error = error;
            this.success = success;
        }

        @Override
        public Kind kind() {
            return Kind.TOOL_RESULT;
        }

        @Override
        public String toString() {
            return "ToolResult(" + name + ", success=" + success
                    + ", result=" + (result == null ? "null" : result.toString())
                    + (error == null ? "" : ", error=" + error) + ")";
        }
    }

    public static final class FinalDelta extends StreamEvent {
        public final String text;

        public FinalDelta(String text) {
            this.text = text == null ? "" : text;
        }

        @Override
        public Kind kind() {
            return Kind.FINAL_DELTA;
        }

        @Override
        public String toString() {
            return "FinalDelta(" + text + ")";
        }
    }

    public static final class Done extends StreamEvent {
        public final String reply;
        public final int totalSteps;
        public final Integer promptTokens;
        public final Integer completionTokens;

        public Done(String reply, int totalSteps, Integer promptTokens, Integer completionTokens) {
            this.reply = reply == null ? "" : reply;
            this.totalSteps = totalSteps;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }

        @Override
        public Kind kind() {
            return Kind.DONE;
        }

        @Override
        public String toString() {
            return "Done(steps=" + totalSteps + ", replyLen=" + reply.length() + ")";
        }
    }

    public static final class ErrorEvent extends StreamEvent {
        public final Throwable cause;

        public ErrorEvent(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public Kind kind() {
            return Kind.ERROR;
        }

        @Override
        public String toString() {
            return "Error(" + (cause == null ? "null" : cause.getMessage()) + ")";
        }
    }
}
