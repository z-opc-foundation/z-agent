package com.zifang.z.agent.engine.agent.multi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent 之间的通信消息。
 * 支持：
 * - REQUEST: Worker 向 Supervisor 请求指导
 * - RESPONSE: Supervisor 向 Worker 回复
 * - NOTIFICATION: 单向通知（如任务完成广播）
 * - DELEGATION: Supervisor 将子任务委托给 Worker
 */
public class AgentMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private String messageId;
    private String fromAgent;
    private String toAgent;       // null 表示广播
    private MessageType type;
    private String content;       // 消息内容
    private Map<String, Object> payload; // 结构化数据
    private long timestamp;
    private String conversationId; // 同一会话链的 ID
    private String inReplyTo;      // 这条消息回复的是哪条 (DELEGATION messageId)

    public AgentMessage() {
        this.payload = new HashMap<>();
    }

    public static AgentMessage create(String from, String to, MessageType type, String content) {
        AgentMessage m = new AgentMessage();
        m.messageId = "msg-" + System.nanoTime();
        m.fromAgent = from;
        m.toAgent = to;
        m.type = type;
        m.content = content;
        m.timestamp = System.currentTimeMillis();
        m.payload = new HashMap<>();
        m.conversationId = "conv-" + System.currentTimeMillis();
        return m;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFromAgent() {
        return fromAgent;
    }

    public void setFromAgent(String fromAgent) {
        this.fromAgent = fromAgent;
    }

    public String getToAgent() {
        return toAgent;
    }

    public void setToAgent(String toAgent) {
        this.toAgent = toAgent;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    // ===== Builder =====

    @Override
    public String toString() {
        return "AgentMessage{" +
                "messageId='" + messageId + '\'' +
                ", fromAgent='" + fromAgent + '\'' +
                ", toAgent='" + toAgent + '\'' +
                ", type=" + type +
                ", content='" + content + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", inReplyTo='" + inReplyTo + '\'' +
                '}';
    }

    public enum MessageType {
        REQUEST,
        RESPONSE,
        NOTIFICATION,
        DELEGATION,
        ERROR
    }

    public static class Builder {
        private final AgentMessage message = new AgentMessage();

        public Builder messageId(String messageId) {
            message.messageId = messageId;
            return this;
        }

        public Builder fromAgent(String fromAgent) {
            message.fromAgent = fromAgent;
            return this;
        }

        public Builder toAgent(String toAgent) {
            message.toAgent = toAgent;
            return this;
        }

        public Builder type(MessageType type) {
            message.type = type;
            return this;
        }

        public Builder content(String content) {
            message.content = content;
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            message.payload = payload;
            return this;
        }

        public Builder timestamp(long timestamp) {
            message.timestamp = timestamp;
            return this;
        }

        public Builder conversationId(String conversationId) {
            message.conversationId = conversationId;
            return this;
        }

        public Builder inReplyTo(String inReplyTo) {
            message.inReplyTo = inReplyTo;
            return this;
        }

        public AgentMessage build() {
            return message;
        }
    }
}