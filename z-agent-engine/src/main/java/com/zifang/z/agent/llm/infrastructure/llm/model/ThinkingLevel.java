package com.zifang.z.agent.llm.infrastructure.llm.model;

/**
 * Thinking level — controls the LLM's reasoning depth.
 */
public enum ThinkingLevel {

    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String value;

    ThinkingLevel(String value) {
        this.value = value;
    }

    public static ThinkingLevel fromValue(String value) {
        if (value == null) {
            return MEDIUM;
        }
        for (ThinkingLevel level : values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        return MEDIUM;
    }

    public String getValue() {
        return value;
    }
}
