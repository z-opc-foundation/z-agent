package com.zifang.z.agent.center.core.agent.memory.service;

import com.zifang.z.agent.center.core.agent.memory.entity.AgentMemory;

import java.util.List;

/**
 * FEATURE013 T2.3: Agent 长期记忆 Service.
 *
 * <p>职责:
 * <ul>
 *     <li>write — 写入一条记忆 (fact / preference / event)</li>
 *     <li>search — 检索与 query 相关的记忆</li>
 *     <li>decay — 周期衰减所有记忆的 decay_score</li>
 *     <li>forget — 软删某条记忆</li>
 *     <li>injectForPrompt — 把检索结果格式化为 system prompt 片段</li>
 * </ul>
 */
public interface AgentMemoryService {

    /**
     * 写入一条记忆.
     *
     * @param userId     用户
     * @param appCode    Agent 应用编码 (null = 全局用户记忆)
     * @param memoryType FACT / PREFERENCE / EVENT
     * @param content    内容
     * @param importance 0-1
     * @param keywords   空格分隔关键词 (用于检索)
     */
    AgentMemory write(String userId, String appCode, String memoryType,
                      String content, Float importance, String keywords);

    /**
     * 按 user + app + query 检索 Top-N 相关记忆 (命中后自动 touch).
     */
    List<AgentMemory> searchRelevant(String userId, String appCode, String query, int limit);

    /**
     * 列出某用户某应用的所有记忆 (管理后台用).
     */
    List<AgentMemory> listByUser(String userId, String appCode, int limit);

    /**
     * 周期衰减 (factor < 1, 例 0.95 表示每次衰减 5%).
     *
     * @return 受影响行数
     */
    int decay(float factor);

    /**
     * 软删一条记忆.
     */
    boolean forget(Long id);

    /**
     * 检索 + 格式化为 system prompt 片段. 调用方直接 append 到 systemPrompt 末尾.
     * 没找到记忆时返回空字符串.
     */
    String injectForPrompt(String userId, String appCode, String query);
}
