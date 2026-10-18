package com.zifang.z.agent.engine.agent.tool.resilience;

import com.zifang.z.agent.engine.agent.llm.tool.core.ToolExecutor;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 为 ToolExecutor 添加弹性能力的装饰器。
 *
 * <p>持有原 ToolExecutor（委托），附加：
 * <ul>
 *   <li>Retry：按 RetryPolicy 进行指数退避重试</li>
 *   <li>CircuitBreaker：按 CircuitBreaker 熔断请求</li>
 *   <li>Fallback：熔断打开或重试耗尽时的降级回调</li>
 * </ul>
 *
 * <p>实现 {@link ToolExecutor}（@FunctionalInterface），execute 返回 String。
 * 使用时通过 {@link ToolResultAdapter} 转换为 {@link com.zifang.z.agent.engine.agent.react.Toolkit.ToolResult}。
 *
 * <p>注意：
 * <ul>
 *   <li>CircuitBreaker 检查在重试循环之前</li>
 *   <li>Fallback 在 CircuitBreaker 拒绝或重试耗尽后调用</li>
 *   <li>所有状态变更方法已加 synchronized（由 CircuitBreaker 保证）</li>
 * </ul>
 */
public class ResilientToolExecutor implements ToolExecutor {

    private final ToolExecutor delegate;
    private final RetryPolicy retryPolicy;
    private final CircuitBreaker circuitBreaker;
    private final Supplier<ToolExecutionResult> fallback;

    /**
     * @param delegate原始     ToolExecutor（被装饰者），不能为 null
     * @param retryPolicy    重试策略，不能为 null
     * @param circuitBreaker 熔断器，不能为 null
     * @param fallback       降级回调，null 表示不降级直接抛异常
     */
    public ResilientToolExecutor(ToolExecutor delegate, RetryPolicy retryPolicy,
                                 CircuitBreaker circuitBreaker,
                                 Supplier<ToolExecutionResult> fallback) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        if (retryPolicy == null) {
            throw new IllegalArgumentException("retryPolicy must not be null");
        }
        if (circuitBreaker == null) {
            throw new IllegalArgumentException("circuitBreaker must not be null");
        }
        this.delegate = delegate;
        this.retryPolicy = retryPolicy;
        this.circuitBreaker = circuitBreaker;
        this.fallback = fallback;
    }

    @Override
    public String execute(Map<String, Object> args) {
        // 检查 CircuitBreaker：OPEN 状态直接降级
        if (!circuitBreaker.allowRequest()) {
            if (fallback != null) {
                return fallback.get().getOutput();
            }
            throw new RuntimeException("Circuit breaker is OPEN for tool: "
                    + delegate.getClass().getName());
        }

        int attempts = 0;
        long startTime = System.currentTimeMillis();
        Throwable lastError = null;

        while (true) {
            attempts++;
            try {
                String result = delegate.execute(args);
                circuitBreaker.recordSuccess();
                return result;
            } catch (Throwable e) {
                lastError = e;
                circuitBreaker.recordFailure();

                if (!retryPolicy.shouldRetry(attempts, e)) {
                    if (fallback != null) {
                        return fallback.get().getOutput();
                    }
                    throw new RuntimeException("Tool execution failed after " + attempts
                            + " attempts", e);
                }

                long waitTime = retryPolicy.getRetryInterval(attempts);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new RuntimeException("Tool execution failed", lastError);
    }
}