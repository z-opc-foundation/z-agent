package com.zifang.z.agent.center.core.agent.stats.dto;

import java.io.Serializable;

/**
 * FEATURE013 T3.2: 延迟分布直方图的一个分桶
 *
 * <p>例如 [0-500ms] 1223 桶，[500-1000ms] 345 桶 …
 * 用于绘制延迟分布柱状图。
 */
public class AgentLatencyBucket implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 桶标签（如 "<500ms", "500-1000ms", "1000-2000ms", "2000-5000ms", ">=5000ms"）
     */
    private String label;

    /**
     * 桶下界（毫秒，含）
     */
    private Integer lowerMs;

    /**
     * 桶上界（毫秒，不含；Integer.MAX_VALUE 表示 +∞）
     */
    private Integer upperMs;

    /**
     * 桶内对话数
     */
    private Long count;

    /**
     * 桶内平均延迟
     */
    private Double avgLatencyMs;

    public AgentLatencyBucket() {
    }

    public AgentLatencyBucket(String label, Integer lowerMs, Integer upperMs) {
        this.label = label;
        this.lowerMs = lowerMs;
        this.upperMs = upperMs;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getLowerMs() {
        return lowerMs;
    }

    public void setLowerMs(Integer lowerMs) {
        this.lowerMs = lowerMs;
    }

    public Integer getUpperMs() {
        return upperMs;
    }

    public void setUpperMs(Integer upperMs) {
        this.upperMs = upperMs;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(Double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }
}
