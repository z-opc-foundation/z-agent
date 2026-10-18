package com.zifang.z.agent.engine.agent.memory;

import com.zifang.z.agent.engine.agent.memory.model.MemoryEntry;
import com.zifang.z.agent.engine.agent.memory.store.SessionMemoryStore;

import java.util.List;
import java.util.Map;

/**
 * 管理 Agent 对话记忆的核心组件。
 *
 * <ul>
 *   <li>SHORT（短时记忆）：ReAct 每轮自动写入，自动 LRU 淘汰</li>
 *   <li>LONG（长时记忆）：需要时显式调用 {@link #saveToLongTerm(String, String, Map)} 写入</li>
 * </ul>
 * <p>
 * 线程安全：{@link SessionMemoryStore} 实现类应保证线程安全（本引擎默认使用 {@link com.zifang.z.agent.engine.agent.memory.store.InMemorySessionMemoryStore}）。
 */
public class MemoryManager {

    private final SessionMemoryStore shortTermStore;
    private final SessionMemoryStore longTermStore; // 可为 null（仅使用短时记忆时）

    public MemoryManager(SessionMemoryStore shortTermStore, SessionMemoryStore longTermStore) {
        this.shortTermStore = shortTermStore;
        this.longTermStore = longTermStore;
    }

    /**
     * 追加一条记忆。
     * 如果 type == SHORT，写入短时存储；如果 longTermStore != null 且 type == LONG，写入长时存储。
     *
     * @param sessionId 会话 ID
     * @param content   内容
     * @param type      SHORT 或 LONG
     * @param metadata  元数据（可为 null）
     */
    public void append(String sessionId, String content, MemoryEntry.MemoryType type, Map<String, Object> metadata) {
        MemoryEntry entry = new MemoryEntry(sessionId, content, type, metadata);
        if (type == MemoryEntry.MemoryType.SHORT) {
            shortTermStore.save(entry);
        } else if (longTermStore != null) {
            longTermStore.save(entry);
        }
    }

    /**
     * 获取指定会话的最新 N 条历史（合并 SHORT + LONG，按时间升序返回）。
     *
     * @param sessionId 会话 ID
     * @param recentN   ≤0 表示不限制，返回全部
     * @return 历史列表
     */
    public List<MemoryEntry> getConversationHistory(String sessionId, int recentN) {
        return shortTermStore.getHistory(sessionId, recentN);
    }

    /**
     * 显式将内容保存为长时记忆。
     *
     * @param sessionId 会话 ID
     * @param content   内容
     * @param metadata  元数据（可为 null）
     */
    public void saveToLongTerm(String sessionId, String content, Map<String, Object> metadata) {
        if (longTermStore != null) {
            longTermStore.save(new MemoryEntry(sessionId, content, MemoryEntry.MemoryType.LONG, metadata));
        }
    }

    /**
     * 清除指定会话的全部记忆（SHORT + LONG）。
     *
     * @param sessionId 会话 ID
     */
    public void clearSession(String sessionId) {
        shortTermStore.clear(sessionId);
        if (longTermStore != null) {
            longTermStore.clear(sessionId);
        }
    }

    /**
     * 判断是否需要规划（短时记忆条目数超过阈值）。
     * 用于 ReAct Agent 在记忆膨胀时触发任务分解。
     *
     * @param sessionId 会话 ID
     * @param threshold 阈值
     * @return true 表示短时记忆超过阈值
     */
    public boolean shouldDecompose(String sessionId, int threshold) {
        return shortTermStore.size(sessionId) > threshold;
    }

    /**
     * 构建给 LLM 的 memory prompt 片段。
     * 返回格式：
     * <pre>
     * [对话历史]
     * [SHORT]msg1 content
     * [LONG]long term content
     * </pre>
     *
     * @param sessionId 会话 ID
     * @param recentN   ≤0 表示不限制
     * @return prompt 片段，空字符串表示无历史
     */
    public String buildMemoryPrompt(String sessionId, int recentN) {
        List<MemoryEntry> history = getConversationHistory(sessionId, recentN);
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n[对话历史]\n");
        for (MemoryEntry e : history) {
            sb.append("[").append(e.getType()).append("]").append(e.getContent()).append("\n");
        }
        return sb.toString();
    }

    public SessionMemoryStore getShortTermStore() {
        return shortTermStore;
    }

    public SessionMemoryStore getLongTermStore() {
        return longTermStore;
    }
}