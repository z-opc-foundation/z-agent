package com.zifang.z.agent.engine.agent.memory.model;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 记忆条目。
 * 核心数据模型，对应一次对话内容。
 */
public class MemoryEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 条目身份。2026-10-04 新增。
     *
     * <p>原来 {@link InMemorySessionMemoryStore#saveShort} 拿
     * {@code sessionId + "_" + timestamp} 当 map 的 key，而 {@code timestamp} 是
     * {@link System#currentTimeMillis()} —— <b>毫秒级，不是唯一标识</b>。
     * 同一毫秒里追加的两条记忆 key 完全相同，后一条把前一条<b>整个覆盖掉</b>。
     *
     * <p>这不是测试造出来的场景，生产里天天发生：ReActAgent 一轮里连续 append
     * 用户消息 / 工具调用 / 工具结果，全在同一个毫秒内。实测后果：
     * <ul>
     *   <li>{@code store.size(sessionId)} 7 条只剩 1 条</li>
     *   <li>{@code shouldDecompose()} 永远为 false ⇒ "记忆够了就让 LLM 拆解"这条能力是死的</li>
     *   <li>{@code buildMemoryPrompt()} 只喂给 LLM 最后一条 ⇒ 它自称的
     *       {@code [对话历史]} 几乎是空的</li>
     * </ul>
     *
     * <p>为什么不能直接把 timestamp 换成 nanoTime 当 key：那会让"同一个对象被重新
     * save 一次"变成两条记录（原地更新语义没了）。条目要有一个<b>跟内容无关、
     * 随对象走</b>的身份才成。
     */
    private static final AtomicLong SEQ = new AtomicLong();

    private String id;
    private String sessionId;
    private String content;
    private MemoryType type;
    private long timestamp;
    private Map<String, Object> metadata;

    public MemoryEntry() {
        this.timestamp = System.currentTimeMillis();
        this.id = nextId();
    }

    public MemoryEntry(String sessionId, String content, MemoryType type, Map<String, Object> metadata) {
        this.sessionId = sessionId;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.metadata = metadata;
        this.id = nextId();
    }

    /**
     * 进程内单调序号 + 纳秒尾巴。尾巴是为了跨进程重启后不与上一轮留下的 id 撞上
     * （条目可能被序列化后持久化再读回来）。
     */
    private static String nextId() {
        return "e" + SEQ.incrementAndGet() + "-" + System.nanoTime();
    }

    /**
     * 拿到条目身份。老数据（serialVersionUID 仍是 1L，字段是后加的）反序列化出来
     * {@code id} 会是 null，这里补一个 —— 宁可用一个一次性的 id，也不要让调用方
     * 拿到 null 去拼 key 又撞一次。
     */
    public String getId() {
        if (id == null) {
            id = nextId();
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter / Setter

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MemoryType getType() {
        return type;
    }

    public void setType(MemoryType type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Builder pattern

    @Override
    public String toString() {
        return "MemoryEntry{" +
                "sessionId='" + sessionId + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", content='" + content + '\'' +
                '}';
    }

    public enum MemoryType {
        /**
         * 短时记忆：基于 LRU 自动淘汰
         */
        SHORT,
        /**
         * 长时记忆：持久化存储，不过期
         */
        LONG
    }

    public static class Builder {
        private String sessionId;
        private String content;
        private MemoryType type;
        private Map<String, Object> metadata;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder type(MemoryType type) {
            this.type = type;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public MemoryEntry build() {
            return new MemoryEntry(sessionId, content, type, metadata);
        }
    }
}