package com.zifang.z.agent.center.core.agent.stats.dto;

import java.io.Serializable;

/**
 * FEATURE013 T3.2: 按 Agent 应用维度聚合的一行
 *
 * <p>用于"按 Agent 应用统计"表格和 Top-N 排行。
 */
public class AgentByAppRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用编码（agent_app.app_code 或 instance_code 维度）
     */
    private String appCode;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 调用次数
     */
    private Long totalCalls;

    /**
     * 成功次数
     */
    private Long successCount;

    /**
     * 失败次数
     */
    private Long failedCount;

    /**
     * 成功率（0-1）
     */
    private Double successRate;

    /**
     * 总 Token
     */
    private Long totalTokens;

    /**
     * 平均 Token
     */
    private Double avgTokens;

    /**
     * 平均延迟（毫秒）
     */
    private Double avgLatencyMs;

    /**
     * P95 延迟（毫秒）
     */
    private Long p95LatencyMs;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(Long totalCalls) {
        this.totalCalls = totalCalls;
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
}
