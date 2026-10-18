package com.zifang.z.agent.center.core.agent.stats;

import com.zifang.z.agent.center.core.agent.stats.dto.*;

import java.util.List;

/**
 * FEATURE013 T3.2: Agent 监控面板统计服务
 *
 * <p>所有指标均聚合自 z_agent_conversation 表，不写新数据。后续如需引入
 * z_agent_metric 做 per-LLM 调用级聚合，可在本服务里扩展新接口而不破坏现有契约。
 */
public interface AgentStatsService {

    /**
     * 顶部 KPI 总览：累计 / 今日 对话数、Token、成功率、延迟
     *
     * @param appCode 可选应用过滤；null/空 表示全量
     */
    AgentStatsOverview getOverview(String appCode);

    /**
     * 近 N 天每日趋势点（默认 7 天）
     *
     * @param appCode 可选应用过滤
     * @param days    趋势天数（包含今日）
     */
    List<AgentTrendPoint> getTrend(String appCode, int days);

    /**
     * 延迟分布直方图：固定 6 桶（&lt;500 / 500-1k / 1k-2k / 2k-5k / 5k-10k / &gt;=10k ms）
     *
     * @param appCode 可选应用过滤
     */
    List<AgentLatencyBucket> getLatencyDistribution(String appCode);

    /**
     * 按 Agent 应用聚合统计
     *
     * @param appCode 可选应用过滤
     * @param limit   TOP N
     */
    List<AgentByAppRow> getByApp(String appCode, int limit);

    /**
     * 按用户聚合统计
     *
     * @param appCode 可选应用过滤
     * @param limit   TOP N
     */
    List<AgentByUserRow> getByUser(String appCode, int limit);
}
