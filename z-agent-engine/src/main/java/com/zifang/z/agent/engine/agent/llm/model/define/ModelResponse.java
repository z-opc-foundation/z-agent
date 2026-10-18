package com.zifang.z.agent.engine.agent.llm.model.define;

/**
 * Model response: incremental chunk delivered by a streaming {@link Model}.
 * <p>
 * 当前定义仅承载 stream() 输出的 content 片段；后续若需要 finishReason / usage / toolCalls
 * 在此扩展，兼容现有调用方（getter/setter 都是新加字段）。
 */
public class ModelResponse {

    private String content;

    public ModelResponse() {
    }

    public ModelResponse(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}