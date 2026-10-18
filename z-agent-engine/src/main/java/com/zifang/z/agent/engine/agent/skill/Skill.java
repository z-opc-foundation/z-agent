package com.zifang.z.agent.engine.agent.skill;

import com.zifang.util.json.annotation.JsonDeserialize;
import com.zifang.util.json.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Skill definition for agent capabilities.
 * Supports YAML frontmatter metadata (Hermes-compatible format).
 * <p>
 * Frontmatter fields:
 * - name: skill identifier
 * - description: what this skill does
 * - trigger: regex pattern to auto-trigger this skill
 * - version: skill version
 * - author: creator
 * - tags: categorization tags
 * - enabled: whether skill is active
 * - examples: usage examples in markdown
 */
public class Skill {

    // === Frontmatter fields (metadata) ===

    private String name;
    private String description;
    private String trigger;           // regex pattern, e.g. "java|spring|maven"
    private String version = "1.0.0";
    private String author;
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();
    private List<String> tags = new ArrayList<>();
    private String examples;          // markdown usage examples
    private boolean enabled = true;

    // === Existing fields ===

    private Map<String, Object> parameters;
    private List<String> required;

    /**
     * 2026-10-04 修：字段原来叫 {@code isComposite}，那是个 JavaBeans 的坑 ——
     * boolean 字段带 {@code is} 前缀，而访问器是 {@code isComposite()}/{@code setComposite()}，
     * 于是 Jackson 认到的属性名是 {@code composite}，跟字段名对不上。
     *
     * <p>后果不是"名字难看"，是<b>磁盘上的技能文件一个都标不成 composite</b>：
     * {@code z-agent/skills/} 下 31 个 json 与 feature020.md 里写的都是
     * {@code "is_composite": false}，而 {@link SkillLoader} 的反序列化<b>忽略未知字段</b>
     * ⇒ 那个键被<b>静默丢弃</b>，不报错、不告警，isComposite 恒为 false。
     * 实测（2026-10-04）：31 个文件全部"加载成功"，无一例外 isComposite=false。
     *
     * <p>而 {@code isComposite()} 是<b>门控真实行为</b>的：AgentEngine:45 据它决定要不要把
     * 技能拆解成原子子技能，:88 又据它过滤出原子技能清单。也就是说
     * "composite 技能 ⇒ LLM 拆解" 这条能力，配文件根本点不着。
     *
     * <p>⇒ 字段名去掉 is 前缀（与 bean 属性名一致，去掉坑），JSON 名用
     * {@code @JsonSerialize/@JsonDeserialize(name=...)} 显式钉成 {@code is_composite} —— 对齐磁盘文件、feature 文档
     * 与 SkillTest 里那条一直没跑通的契约断言。
     */
    @JsonSerialize(name = "is_composite")
    @JsonDeserialize(name = "is_composite")
    private boolean composite;

    // === Content (markdown body) ===
    private String content;           // the markdown body after frontmatter

    public Skill() {
    }

    // Builder pattern for construction
    public static Builder builder() {
        return new Builder();
    }

    // === Frontmatter getters/setters ===

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getExamples() {
        return examples;
    }

    public void setExamples(String examples) {
        this.examples = examples;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // === Existing getters/setters ===

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required;
    }

    public boolean isComposite() {
        return composite;
    }

    public void setComposite(boolean composite) {
        this.composite = composite;
    }

    // === Content getter/setter ===

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // === Utility methods ===

    /**
     * Check if this skill matches the given message.
     * Uses trigger regex if set, otherwise matches description keywords.
     */
    public boolean matches(String message) {
        if (!enabled) {
            return false;
        }

        if (trigger == null || trigger.isEmpty()) {
            // Fallback: match on description keywords
            if (description == null) {
                return false;
            }

            String lowerMsg = message.toLowerCase();
            String lowerDesc = description.toLowerCase();
            // Match if any word in trigger appears in message
            for (String tag : tags) {
                if (lowerMsg.contains(tag.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        // Regex match on trigger
        return message.matches("(?i).*(" + trigger + ").*");
    }

    /**
     * Build full system prompt by combining content with metadata.
     */
    public String toSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        if (description != null) {
            sb.append("## ").append(name).append("\n");
            sb.append(description).append("\n\n");
        }
        if (content != null) {
            sb.append(content);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Skill{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", enabled=" + enabled +
                ", trigger='" + trigger + '\'' +
                '}';
    }

    // === Builder ===

    public static class Builder {
        private final Skill skill = new Skill();

        public Builder name(String name) {
            skill.setName(name);
            return this;
        }

        public Builder description(String desc) {
            skill.setDescription(desc);
            return this;
        }

        public Builder trigger(String trigger) {
            skill.setTrigger(trigger);
            return this;
        }

        public Builder version(String version) {
            skill.setVersion(version);
            return this;
        }

        public Builder author(String author) {
            skill.setAuthor(author);
            return this;
        }

        public Builder tags(List<String> tags) {
            skill.setTags(tags);
            return this;
        }

        public Builder examples(String examples) {
            skill.setExamples(examples);
            return this;
        }

        public Builder enabled(boolean enabled) {
            skill.setEnabled(enabled);
            return this;
        }

        public Builder content(String content) {
            skill.setContent(content);
            return this;
        }

        public Builder parameters(Map<String, Object> params) {
            skill.setParameters(params);
            return this;
        }

        public Builder required(List<String> required) {
            skill.setRequired(required);
            return this;
        }

        public Builder composite(boolean composite) {
            skill.setComposite(composite);
            return this;
        }

        public Skill build() {
            if (skill.getName() == null || skill.getName().isEmpty()) {
                throw new IllegalStateException("Skill name is required");
            }
            return skill;
        }
    }
}