package com.zifang.z.agent.center.core.agent.stats.dto;

import java.io.Serializable;

/**
 * FEATURE013 T3.2: 每日趋势点（横轴日期 + 纵轴指标）
 *
 * <p>用于折线图 / 柱状图。例如 7 天 Token 趋势，每个点 = 一天。
 */
public class AgentTrendPoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期字符串 yyyy-MM-dd
     */
    private String date;

    /**
     * 当日对话数
     */
    private Long dailyConversations;

    /**
     * 当日成功数
     */
    private Long dailySuccess;

    /**
     * 当日失败数
     */
    private Long dailyFailed;

    /**
     * 当日 Token 消耗
     */
    private Long dailyTokens;

    /**
     * 当日平均延迟（毫秒）
     */
    private Double avgLatencyMs;

    public AgentTrendPoint() {
    }

    public AgentTrendPoint(String date, Long dailyConversations, Long dailyTokens) {
        this.date = date;
        this.dailyConversations = dailyConversations;
        this.dailyTokens = dailyTokens;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Long getDailyConversations() {
        return dailyConversations;
    }

    public void setDailyConversations(Long dailyConversations) {
        this.dailyConversations = dailyConversations;
    }

    public Long getDailySuccess() {
        return dailySuccess;
    }

    public void setDailySuccess(Long dailySuccess) {
        this.dailySuccess = dailySuccess;
    }

    public Long getDailyFailed() {
        return dailyFailed;
    }

    public void setDailyFailed(Long dailyFailed) {
        this.dailyFailed = dailyFailed;
    }

    public Long getDailyTokens() {
        return dailyTokens;
    }

    public void setDailyTokens(Long dailyTokens) {
        this.dailyTokens = dailyTokens;
    }

    public Double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(Double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }
}
