package com.zifang.z.agent.center.core.agent.stats.dto;

import java.io.Serializable;

/**
 * FEATURE013 T3.2: 按用户维度聚合的一行
 */
public class AgentByUserRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;

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
     * 总 Token
     */
    private Long totalTokens;

    /**
     * 平均延迟（毫秒）
     */
    private Double avgLatencyMs;

    /**
     * 最近调用时间
     */
    private String lastCallAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(Double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public String getLastCallAt() {
        return lastCallAt;
    }

    public void setLastCallAt(String lastCallAt) {
        this.lastCallAt = lastCallAt;
    }
}
