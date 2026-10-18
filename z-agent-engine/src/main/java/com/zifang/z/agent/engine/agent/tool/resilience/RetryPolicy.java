package com.zifang.z.agent.engine.agent.tool.resilience;

import java.io.Serializable;

/**
 * 重试策略配置。
 * 支持指数退避（Exponential Backoff），可配置最大重试次数、初始间隔、乘数、最大间隔。
 */
public class RetryPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int maxAttempts;
    private final long initialIntervalMs;
    private final double multiplier;
    private final long maxIntervalMs;

    private RetryPolicy(Builder b) {
        this.maxAttempts = b.maxAttempts;
        this.initialIntervalMs = b.initialIntervalMs;
        this.multiplier = b.multiplier;
        this.maxIntervalMs = b.maxIntervalMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 计算第 attempt 次重试的等待间隔。
     * attempt=1 时返回 initialIntervalMs（首次执行无需等待）。
     * attempt>=2 时使用指数退避：initialIntervalMs * multiplier^(attempt-2)
     *
     * @param attempt 当前尝试次数（从 1 开始）
     * @return 等待毫秒数
     */
    public long getRetryInterval(int attempt) {
        if (attempt <= 1) {
            return initialIntervalMs;
        }
        long interval = (long) (initialIntervalMs * Math.pow(multiplier, attempt - 2));
        return Math.min(interval, maxIntervalMs);
    }

    /**
     * 判断给定尝试次数和异常是否应该重试。
     *
     * @param attempt 当前尝试次数（从 1 开始）
     * @param cause   最近一次抛出的异常
     * @return true=应继续重试，false=停止重试
     */
    public boolean shouldRetry(int attempt, Throwable cause) {
        if (attempt >= maxAttempts) {
            return false;
        }
        // 默认：AssertionError/VerifyError 等业务异常不重试
        if (cause instanceof AssertionError) {
            return false;
        }
        if (cause instanceof java.lang.VerifyError) {
            return false;
        }
        return true;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getInitialIntervalMs() {
        return initialIntervalMs;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public long getMaxIntervalMs() {
        return maxIntervalMs;
    }

    public static class Builder {
        private int maxAttempts = 3;
        private long initialIntervalMs = 500;
        private double multiplier = 2.0;
        private long maxIntervalMs = 30000;

        public Builder maxAttempts(int n) {
            this.maxAttempts = n;
            return this;
        }

        public Builder initialIntervalMs(long ms) {
            this.initialIntervalMs = ms;
            return this;
        }

        public Builder multiplier(double m) {
            this.multiplier = m;
            return this;
        }

        public Builder maxIntervalMs(long ms) {
            this.maxIntervalMs = ms;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}