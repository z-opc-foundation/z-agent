package com.zifang.z.agent.engine.agent.multi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 声明一个 Worker 能做的事情。
 * 用于 supervisor 的 routing 决策 (按 tags / 名称 / 描述匹配)。
 *
 * <p>设计原则:
 * <ul>
 *   <li>{@code tags} 是 routing 用的稳定键 (如 "config", "task", "ctc")</li>
 *   <li>{@code description} 给 LLM 看, 让 supervisor 知道何时选这个 worker</li>
 *   <li>{@code examples} 是可选的样例 query, 帮助 LLM 更好地判断</li>
 * </ul>
 */
public class WorkerCapability {

    private final String name;
    private final String description;
    private final List<String> tags;
    private final List<String> examples;

    private WorkerCapability(Builder b) {
        this.name = Objects.requireNonNull(b.name, "name");
        this.description = b.description == null ? "" : b.description;
        this.tags = b.tags == null ? new ArrayList<>() : new ArrayList<>(b.tags);
        this.examples = b.examples == null ? new ArrayList<>() : new ArrayList<>(b.examples);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WorkerCapability of(String name, String description, String... tags) {
        return new Builder().name(name).description(description).tags(Arrays.asList(tags)).build();
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public List<String> tags() {
        return tags;
    }

    public List<String> examples() {
        return examples;
    }

    /**
     * 该 capability 是否与给定 tag 集合匹配 (任一 tag 命中即匹配)。
     */
    public boolean matchesAny(List<String> queryTags) {
        if (queryTags == null || queryTags.isEmpty()) {
            return false;
        }
        for (String t : queryTags) {
            if (tags.contains(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把 capability 渲染成一段人类/LLM 可读的描述, 用于 supervisor 的 system prompt。
     */
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(name);
        if (!description.isEmpty()) {
            sb.append(": ").append(description);
        }
        if (!tags.isEmpty()) {
            sb.append(" [tags: ").append(String.join(",", tags)).append("]");
        }
        if (!examples.isEmpty()) {
            sb.append(" e.g. ");
            sb.append(String.join(" | ", examples));
        }
        return sb.toString();
    }

    public static class Builder {
        private String name;
        private String description;
        private List<String> tags;
        private List<String> examples;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder examples(List<String> examples) {
            this.examples = examples;
            return this;
        }

        public WorkerCapability build() {
            return new WorkerCapability(this);
        }
    }
}
