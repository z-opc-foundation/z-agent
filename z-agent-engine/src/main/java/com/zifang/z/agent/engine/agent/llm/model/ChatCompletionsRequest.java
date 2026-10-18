package com.zifang.z.agent.engine.agent.llm.model;

import com.zifang.util.json.annotation.JsonDeserialize;
import com.zifang.util.json.annotation.JsonSerialize;

import java.util.List;
import java.util.Map;

/**
 * Request payload for the Chat Completions HTTP API.
 * Compatible with OpenAI's Chat Completions API format.
 * Supports all major LLM parameters for comprehensive testing.
 */
public class ChatCompletionsRequest {

    // ===== Core Parameters =====
    private String model;
    private List<ChatMessage> messages;

    // ===== Sampling Parameters =====
    private Double temperature;
    @JsonSerialize(name = "top_p")
    @JsonDeserialize(name = "top_p")
    private Double topP;
    @JsonSerialize(name = "max_tokens")
    @JsonDeserialize(name = "max_tokens")
    private Integer maxTokens;
    @JsonSerialize(name = "max_completion_tokens")
    @JsonDeserialize(name = "max_completion_tokens")
    private Integer maxCompletionTokens;
    @JsonSerialize(name = "top_k")
    @JsonDeserialize(name = "top_k")
    private Integer topK;
    @JsonSerialize(name = "repeat_penalty")
    @JsonDeserialize(name = "repeat_penalty")
    private Double repeatPenalty;

    // ===== Diversity & Penalty Parameters =====
    @JsonSerialize(name = "frequency_penalty")
    @JsonDeserialize(name = "frequency_penalty")
    private Double frequencyPenalty;
    @JsonSerialize(name = "presence_penalty")
    @JsonDeserialize(name = "presence_penalty")
    private Double presencePenalty;

    // ===== Output Control =====
    private List<String> stop;
    private Integer n;
    private Boolean logprobs;
    private Boolean echo;
    private Long seed;
    private String user;
    private Boolean store;
    private Map<Integer, Float> logitBias;
    private Map<String, String> metadata;
    private Boolean parallelToolCalls;
    private String presenceMetadata;

    // ===== Streaming & Tools =====
    private Boolean stream;
    private List<OpenAITool> tools;
    @JsonSerialize(name = "tool_choice")
    @JsonDeserialize(name = "tool_choice")
    private Object toolChoice;
    @JsonSerialize(name = "response_format")
    @JsonDeserialize(name = "response_format")
    private ResponseFormat responseFormat;
    private Map<String, Object> prediction;

    // ===== System & Context (Ollama specific) =====
    private Integer numCtx;
    private Integer num_gpu;
    private Boolean keep_alive;

    // ===== Extra Parameters =====
    private Map<String, Object> extraParams;

    public ChatCompletionsRequest() {
    }

    public ChatCompletionsRequest(String model, List<ChatMessage> messages) {
        this.model = model;
        this.messages = messages;
    }

    // ===== Builder =====
    public static Builder builder() {
        return new Builder();
    }

    // ===== Getters and Setters =====
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
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

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
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

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    public Boolean getLogprobs() {
        return logprobs;
    }

    public void setLogprobs(Boolean logprobs) {
        this.logprobs = logprobs;
    }

    public Boolean getEcho() {
        return echo;
    }

    public void setEcho(Boolean echo) {
        this.echo = echo;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public List<OpenAITool> getTools() {
        return tools;
    }

    public void setTools(List<OpenAITool> tools) {
        this.tools = tools;
    }

    public Object getToolChoice() {
        return toolChoice;
    }

    public void setToolChoice(Object toolChoice) {
        this.toolChoice = toolChoice;
    }

    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Boolean getStore() {
        return store;
    }

    public void setStore(Boolean store) {
        this.store = store;
    }

    public Map<Integer, Float> getLogitBias() {
        return logitBias;
    }

    public void setLogitBias(Map<Integer, Float> logitBias) {
        this.logitBias = logitBias;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Boolean getParallelToolCalls() {
        return parallelToolCalls;
    }

    public void setParallelToolCalls(Boolean parallelToolCalls) {
        this.parallelToolCalls = parallelToolCalls;
    }

    public String getPresenceMetadata() {
        return presenceMetadata;
    }

    public void setPresenceMetadata(String presenceMetadata) {
        this.presenceMetadata = presenceMetadata;
    }

    public Map<String, Object> getPrediction() {
        return prediction;
    }

    public void setPrediction(Map<String, Object> prediction) {
        this.prediction = prediction;
    }

    public Integer getNumCtx() {
        return numCtx;
    }

    public void setNumCtx(Integer numCtx) {
        this.numCtx = numCtx;
    }

    public Integer getNum_gpu() {
        return num_gpu;
    }

    public void setNum_gpu(Integer num_gpu) {
        this.num_gpu = num_gpu;
    }

    public Boolean getKeep_alive() {
        return keep_alive;
    }

    public void setKeep_alive(Boolean keep_alive) {
        this.keep_alive = keep_alive;
    }

    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(Map<String, Object> extraParams) {
        this.extraParams = extraParams;
    }

    public static class Builder {
        private String model;
        private List<ChatMessage> messages;
        private Double temperature;
        private Double topP;
        private Integer maxTokens;
        private Integer maxCompletionTokens;
        private Integer topK;
        private Double repeatPenalty;
        private Double frequencyPenalty;
        private Double presencePenalty;
        private List<String> stop;
        private Integer n;
        private Boolean logprobs;
        private Boolean echo;
        private Long seed;
        private String user;
        private Boolean store;
        private Map<Integer, Float> logitBias;
        private Map<String, String> metadata;
        private Boolean parallelToolCalls;
        private String presenceMetadata;
        private Boolean stream;
        private List<OpenAITool> tools;
        private Object toolChoice;
        private ResponseFormat responseFormat;
        private Map<String, Object> prediction;
        private Integer numCtx;
        private Integer num_gpu;
        private Boolean keep_alive;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public Builder topK(Integer topK) {
            this.topK = topK;
            return this;
        }

        public Builder repeatPenalty(Double repeatPenalty) {
            this.repeatPenalty = repeatPenalty;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder logprobs(Boolean logprobs) {
            this.logprobs = logprobs;
            return this;
        }

        public Builder echo(Boolean echo) {
            this.echo = echo;
            return this;
        }

        public Builder seed(Long seed) {
            this.seed = seed;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder store(Boolean store) {
            this.store = store;
            return this;
        }

        public Builder logitBias(Map<Integer, Float> logitBias) {
            this.logitBias = logitBias;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder parallelToolCalls(Boolean parallelToolCalls) {
            this.parallelToolCalls = parallelToolCalls;
            return this;
        }

        public Builder presenceMetadata(String presenceMetadata) {
            this.presenceMetadata = presenceMetadata;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder tools(List<OpenAITool> tools) {
            this.tools = tools;
            return this;
        }

        public Builder toolChoice(Object toolChoice) {
            this.toolChoice = toolChoice;
            return this;
        }

        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder prediction(Map<String, Object> prediction) {
            this.prediction = prediction;
            return this;
        }

        public Builder numCtx(Integer numCtx) {
            this.numCtx = numCtx;
            return this;
        }

        public Builder num_gpu(Integer num_gpu) {
            this.num_gpu = num_gpu;
            return this;
        }

        public Builder keep_alive(Boolean keep_alive) {
            this.keep_alive = keep_alive;
            return this;
        }

        public ChatCompletionsRequest build() {
            ChatCompletionsRequest req = new ChatCompletionsRequest();
            req.model = this.model;
            req.messages = this.messages;
            req.temperature = this.temperature;
            req.topP = this.topP;
            req.maxTokens = this.maxTokens;
            req.maxCompletionTokens = this.maxCompletionTokens;
            req.topK = this.topK;
            req.repeatPenalty = this.repeatPenalty;
            req.frequencyPenalty = this.frequencyPenalty;
            req.presencePenalty = this.presencePenalty;
            req.stop = this.stop;
            req.n = this.n;
            req.logprobs = this.logprobs;
            req.echo = this.echo;
            req.seed = this.seed;
            req.user = this.user;
            req.store = this.store;
            req.logitBias = this.logitBias;
            req.metadata = this.metadata;
            req.parallelToolCalls = this.parallelToolCalls;
            req.presenceMetadata = this.presenceMetadata;
            req.stream = this.stream;
            req.tools = this.tools;
            req.toolChoice = this.toolChoice;
            req.responseFormat = this.responseFormat;
            req.prediction = this.prediction;
            req.numCtx = this.numCtx;
            req.num_gpu = this.num_gpu;
            req.keep_alive = this.keep_alive;
            return req;
        }
    }

    // ===== Inner Classes =====

    public static class ResponseFormat {
        private String type;

        public ResponseFormat() {
        }

        public ResponseFormat(String type) {
            this.type = type;
        }

        public static ResponseFormat of(String type) {
            return new ResponseFormat(type);
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
