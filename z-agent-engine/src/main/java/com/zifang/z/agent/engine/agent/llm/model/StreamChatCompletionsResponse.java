package com.zifang.z.agent.engine.agent.llm.model;

import com.zifang.util.json.annotation.JsonDeserialize;
import com.zifang.util.json.annotation.JsonSerialize;

import java.util.List;

/**
 * Streaming response for Chat Completions API (SSE format).
 */
public class StreamChatCompletionsResponse {

    private String id;
    private String object = "chat.completion.chunk";
    private long created;
    private String model;
    private List<Delta> choices;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Delta> getChoices() {
        return choices;
    }

    public void setChoices(List<Delta> choices) {
        this.choices = choices;
    }

    public String getFirstDeltaContent() {
        if (choices != null && !choices.isEmpty()) {
            Delta delta = choices.get(0);
            if (delta != null && delta.getDelta() != null) {
                return delta.getDelta().getContent();
            }
        }
        return null;
    }

    public String getFirstFinishReason() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getFinishReason();
        }
        return null;
    }

    public boolean isLastChunk() {
        if (choices != null && !choices.isEmpty()) {
            Delta delta = choices.get(0);
            if (delta != null) {
                return delta.getFinishReason() != null;
            }
        }
        return false;
    }

    public static class Delta {
        private int index;
        private Content delta;
        private String finishReason;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public Content getDelta() {
            return delta;
        }

        public void setDelta(Content delta) {
            this.delta = delta;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    public static class Content {
        private String role;
        private String content;
        private String name;

        @JsonSerialize(name = "tool_calls")
        @JsonDeserialize(name = "tool_calls")
        private List<ToolCallData> toolCalls;

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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<ToolCallData> getToolCalls() {
            return toolCalls;
        }

        public void setToolCalls(List<ToolCallData> toolCalls) {
            this.toolCalls = toolCalls;
        }
    }

    public static class ToolCallData {
        private Integer index;
        private String id;
        private String type;
        private Function function;

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Function getFunction() {
            return function;
        }

        public void setFunction(Function function) {
            this.function = function;
        }
    }

    public static class Function {
        private String name;
        private String arguments;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }
    }
}