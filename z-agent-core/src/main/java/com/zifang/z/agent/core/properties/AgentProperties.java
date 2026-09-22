package com.zifang.z.agent.core.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * z-agent 平台配置树 (前缀 z.agent.*).
 *
 * <p>defaultModel: 平台默认模型(给 agent 不指定 model 时用).
 * <p>defaultMaxSteps: agent 默认最大推理步数(超过则 finishedReason=max_steps).
 * <p>defaultTemperature / defaultTopP: 默认采样参数.
 * <p>providers: 模型 → kernel.llm.LlmProvider bean 名映射(如 "gpt-4o" → "openai").
 */
@ConfigurationProperties(prefix = "z.agent")
public class AgentProperties {

    private String defaultModel = "gpt-4o-mini";
    private int defaultMaxSteps = 8;
    private Double defaultTemperature = 0.7;
    private Double defaultTopP = 0.0; // null 表示不设置
    private Integer defaultMaxTokens = 2048;
    private long defaultTimeoutMs = 300_000L;
    private Map<String, String> providers = new HashMap<String, String>();

    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String v) { this.defaultModel = v; }

    public int getDefaultMaxSteps() { return defaultMaxSteps; }
    public void setDefaultMaxSteps(int v) { this.defaultMaxSteps = v; }

    public Double getDefaultTemperature() { return defaultTemperature; }
    public void setDefaultTemperature(Double v) { this.defaultTemperature = v; }

    public Double getDefaultTopP() { return defaultTopP; }
    public void setDefaultTopP(Double v) { this.defaultTopP = v; }

    public Integer getDefaultMaxTokens() { return defaultMaxTokens; }
    public void setDefaultMaxTokens(Integer v) { this.defaultMaxTokens = v; }

    public long getDefaultTimeoutMs() { return defaultTimeoutMs; }
    public void setDefaultTimeoutMs(long v) { this.defaultTimeoutMs = v; }

    public Map<String, String> getProviders() { return providers; }
    public void setProviders(Map<String, String> v) { this.providers = v; }
}