package com.zifang.z.agent.llm.infrastructure.events;

/**
 * Content block — base class for structured content within messages.
 */
public class ContentBlock {

    protected final String type;

    public ContentBlock(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
