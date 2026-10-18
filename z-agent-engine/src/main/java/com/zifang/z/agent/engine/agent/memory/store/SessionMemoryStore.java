package com.zifang.z.agent.engine.agent.memory.store;

import com.zifang.z.agent.engine.agent.memory.model.MemoryEntry;

import java.util.List;

/**
 * 会话记忆存储接口。
 * <p>
 * 引擎层定义接口契约，平台层（如 z-agent-center-core）负责实现。
 *
 * <ul>
 *   <li>SHORT（短时）：基于 LinkedHashMap LRUCache，存储在堆内存中，自动淘汰</li>
 *   <li>LONG（长时）：接口定义，实现类由平台层提供（可接入 DB/Redis 等持久化存储）</li>
 * </ul>
 */
public interface SessionMemoryStore {

    /**
     * 保存一条记忆条目。
     *
     * @param entry 记忆条目，不能为 null
     */
    void save(MemoryEntry entry);

    /**
     * 查询指定会话的最新历史。
     * 返回的列表按 timestamp 升序排列（最旧的在前面）。
     *
     * @param sessionId 会话 ID
     * @param limit     最多返回的条目数，≤0 表示不限制
     * @return 历史列表
     */
    List<MemoryEntry> getHistory(String sessionId, int limit);

    /**
     * 清除指定会话的全部记忆（SHORT + LONG）。
     *
     * @param sessionId 会话 ID
     */
    void clear(String sessionId);

    /**
     * 返回指定会话的当前条目总数（SHORT + LONG 合计）。
     *
     * @param sessionId 会话 ID
     * @return 条目数量
     */
    int size(String sessionId);
}