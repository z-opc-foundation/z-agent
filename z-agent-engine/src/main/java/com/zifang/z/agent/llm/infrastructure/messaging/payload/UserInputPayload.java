package com.zifang.z.agent.llm.infrastructure.messaging.payload;

import com.zifang.z.agent.llm.infrastructure.events.ContentBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User input payload — encapsulates a user's input message for agent processing.
 * Supports both text-only and content-block (multi-modal) payloads.
 */
public class UserInputPayload {

    private final String content;
    private final String sessionId;
    private final String userId;
    private final List<ContentBlock> blocks;

    private UserInputPayload(String content, String sessionId, String userId, List<ContentBlock> blocks) {
        this.content = content;
        this.sessionId = sessionId;
        this.userId = userId;
        this.blocks = blocks != null ? new ArrayList<>(blocks) : new ArrayList<>();
    }

    public static UserInputPayload empty() {
        return new UserInputPayload("", "", "", Collections.<ContentBlock>emptyList());
    }

    public static UserInputPayload of(List<ContentBlock> blocks) {
        return new UserInputPayload("", "", "", blocks);
    }

    public String getContent() {
        return content;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public List<ContentBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    /**
     * Phase 7 扩展：转换为 Map 表示（兼容 0815 业务调用）。
     * <p>返回 4 字段映射：content、sessionId、userId、blocks，供旧版 ReactAgent.chat() 接收。</p>
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("content", content);
        map.put("sessionId", sessionId);
        map.put("userId", userId);
        map.put("blocks", blocks);
        return map;
    }
}
