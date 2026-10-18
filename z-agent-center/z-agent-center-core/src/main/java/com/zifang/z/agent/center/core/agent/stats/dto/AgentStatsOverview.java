package com.zifang.z.agent.center.core.agent.stats.dto;

import java.io.Serializable;

/**
 * FEATURE013 T3.2: Agent 监控面板 - 顶部 KPI 总览卡片
 *
 * <p>聚合指标来源：
 * <ul>
 *     <li>totalConversations / successCount / failedCount / successRate — z_agent_conversation 表</li>
 *     <li>totalTokens / avgTokens — token_count 字段 SUM/AVG</li>
 *     <li>avgLatencyMs / p95LatencyMs — latency_ms 字段 AVG / PERCENTILE</li>
 *     <li>todayConversations / todayTokens — WHERE DATE(gmt_create) = CURDATE()</li>
 * </ul>
 */
public class AgentStatsOverview implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 累计对话总数（含成功/失败）
     */
    private Long totalConversations;

    /**
     * 成功对话数
     */
    private Long successCount;

    /**
     * 失败对话数
     */
    private Long failedCount;

    /**
     * 成功率（0-1）
     */
    private Double successRate;

    /**
     * 累计 Token 消耗
     */
    private Long totalTokens;

    /**
     * 平均 Token / 对话
     */
    private Double avgTokens;

    /**
     * 平均延迟（毫秒）
     */
    private Double avgLatencyMs;

    /**
     * P95 延迟（毫秒，TOP 5% 极值剔除）
     */
    private Long p95LatencyMs;

    /**
     * 今日对话数
     */
    private Long todayConversations;

    /**
     * 今日 Token 消耗
     */
    private Long todayTokens;

    /**
     * 今日失败数
     */
    private Long todayFailedCount;

    /**
     * 独立 Agent 应用数（有对话产生的 distinct instance_code）
     */
    private Long activeAppCount;

    /**
     * 独立用户数
     */
    private Long activeUserCount;

    public Long getTotalConversations() {
        return totalConversations;
    }

    public void setTotalConversations(Long totalConversations) {
        this.totalConversations = totalConversations;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Long failedCount) {
        this.failedCount = failedCount;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Double getAvgTokens() {
        return avgTokens;
    }

    public void setAvgTokens(Double avgTokens) {
        this.avgTokens = avgTokens;
    }

    public Double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(Double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public Long getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(Long p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public Long getTodayConversations() {
        return todayConversations;
    }

    public void setTodayConversations(Long todayConversations) {
        this.todayConversations = todayConversations;
    }

    public Long getTodayTokens() {
        return todayTokens;
    }

    public void setTodayTokens(Long todayTokens) {
        this.todayTokens = todayTokens;
    }

    public Long getTodayFailedCount() {
        return todayFailedCount;
    }

    public void setTodayFailedCount(Long todayFailedCount) {
        this.todayFailedCount = todayFailedCount;
    }

    public Long getActiveAppCount() {
        return activeAppCount;
    }

    public void setActiveAppCount(Long activeAppCount) {
        this.activeAppCount = activeAppCount;
    }

    public Long getActiveUserCount() {
        return activeUserCount;
    }

    public void setActiveUserCount(Long activeUserCount) {
        this.activeUserCount = activeUserCount;
    }
}
