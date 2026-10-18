package com.zifang.z.agent.engine.agent.tool.resilience;

import java.io.Serializable;

/**
 * Tool 执行结果包装，包含成功/失败态及元数据。
 *
 * <p>记录了：
 * <ul>
 *   <li>success：是否成功</li>
 *   <li>output：成功时的输出内容</li>
 *   <li>error：失败时的异常信息</li>
 *   <li>retried：是否经过重试</li>
 *   <li>attempts：总尝试次数</li>
 *   <li>elapsedMs：总耗时（毫秒）</li>
 *   <li>circuitState：执行完成时的熔断器状态</li>
 * </ul>
 */
public class ToolExecutionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String output;
    private final Throwable error;
    private final boolean retried;
    private final int attempts;
    private final long elapsedMs;
    private final CircuitBreaker.State circuitState;

    private ToolExecutionResult(boolean success, String output, Throwable error,
                                boolean retried, int attempts, long elapsedMs,
                                CircuitBreaker.State circuitState) {
        this.success = success;
        this.output = output;
        this.error = error;
        this.retried = retried;
        this.attempts = attempts;
        this.elapsedMs = elapsedMs;
        this.circuitState = circuitState;
    }

    public static ToolExecutionResult success(String output, int attempts, long elapsedMs) {
        return new ToolExecutionResult(true, output, null, attempts > 1, attempts,
                elapsedMs, CircuitBreaker.State.CLOSED);
    }

    public static ToolExecutionResult failure(Throwable error, int attempts, long elapsedMs) {
        return new ToolExecutionResult(false, null, error, attempts > 1, attempts,
                elapsedMs, CircuitBreaker.State.CLOSED);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isRetried() {
        return retried;
    }

    public int getAttempts() {
        return attempts;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public CircuitBreaker.State getCircuitState() {
        return circuitState;
    }

    @Override
    public String toString() {
        return "ToolExecutionResult{" +
                "success=" + success +
                ", output='" + output + '\'' +
                ", error=" + (error != null ? error.getMessage() : null) +
                ", retried=" + retried +
                ", attempts=" + attempts +
                ", elapsedMs=" + elapsedMs +
                ", circuitState=" + circuitState +
                '}';
    }
}