package com.zifang.z.agent.engine.agent.llm.model;

import com.zifang.util.json.annotation.JsonDeserialize;
import com.zifang.util.json.annotation.JsonSerialize;

import java.util.List;

/**
 * Response payload for the Chat Completions HTTP API.
 * Compatible with OpenAI's Chat Completions API format.
 */
public class ChatCompletionsResponse {

    private String id;
    private String object = "chat.completion";
    private long created;
    private String model;
    private List<ChatChoice> choices;
    private Usage usage;

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

    public List<ChatChoice> getChoices() {
        return choices;
    }

    public void setChoices(List<ChatChoice> choices) {
        this.choices = choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public String getFirstContent() {
        if (choices != null && !choices.isEmpty()) {
            ChatChoice choice = choices.get(0);
            if (choice != null && choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return null;
    }

    public List<ToolCall> getToolCalls() {
        if (choices != null && !choices.isEmpty()) {
            ChatChoice choice = choices.get(0);
            if (choice != null && choice.getMessage() != null) {
                return choice.getMessage().getToolCalls();
            }
        }
        return null;
    }

    public String getFinishReason() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getFinishReason();
        }
        return null;
    }

    public static class ChatChoice {
        private int index;
        private ChatMessage message;
        @JsonSerialize(name = "finish_reason")
        @JsonDeserialize(name = "finish_reason")
        private String finishReason;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public ChatMessage getMessage() {
            return message;
        }

        public void setMessage(ChatMessage message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    public static class Usage {
        @JsonSerialize(name = "prompt_tokens")
        @JsonDeserialize(name = "prompt_tokens")
        private Integer promptTokens;

        @JsonSerialize(name = "completion_tokens")
        @JsonDeserialize(name = "completion_tokens")
        private Integer completionTokens;

        @JsonSerialize(name = "total_tokens")
        @JsonDeserialize(name = "total_tokens")
        private Integer totalTokens;

        public Usage() {
        }

        public Usage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
