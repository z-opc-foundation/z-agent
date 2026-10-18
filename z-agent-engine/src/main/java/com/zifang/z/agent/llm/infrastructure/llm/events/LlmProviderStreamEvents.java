package com.zifang.z.agent.llm.infrastructure.llm.events;

/**
 * Base class for all LLM provider stream events.
 */
public class LlmProviderStreamEvents {

    public static class StartStepEvent extends LlmProviderStreamEvents {
    }

    public static class FinishStepEvent extends LlmProviderStreamEvents {
        private String finishReason;
        private com.zifang.z.agent.llm.infrastructure.llm.usage.TokenUsage usage;

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public com.zifang.z.agent.llm.infrastructure.llm.usage.TokenUsage getUsage() {
            return usage;
        }

        public void setUsage(com.zifang.z.agent.llm.infrastructure.llm.usage.TokenUsage usage) {
            this.usage = usage;
        }
    }

    public static class TextDeltaEvent extends LlmProviderStreamEvents {
        private final String delta;

        public TextDeltaEvent(String delta) {
            this.delta = delta;
        }

        public String getDelta() {
            return delta;
        }
    }

    public static class ReasoningDeltaEvent extends LlmProviderStreamEvents {
        private final String delta;

        public ReasoningDeltaEvent(String delta) {
            this.delta = delta;
        }

        public String getDelta() {
            return delta;
        }
    }

    public static class ToolCallEvent extends LlmProviderStreamEvents {
        private String id;
        private String toolName;
        private String arguments;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }

    public static class ErrorEvent extends LlmProviderStreamEvents {
        private final String error;

        public ErrorEvent(String error) {
            this.error = error;
        }

        public ErrorEvent(Throwable t) {
            this.error = t != null ? t.getMessage() : "Unknown error";
        }

        public String getError() {
            return error;
        }
    }
}
