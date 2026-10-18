package com.zifang.z.agent.engine.agent.tool.resilience;

/**
 * 熔断器状态机：CLOSED -> OPEN -> HALF_OPEN -> CLOSED。
 *
 * <p>状态转换规则：
 * <ul>
 *   <li>CLOSED：正常状态，连续失败次数达到 threshold 时转为 OPEN</li>
 *   <li>OPEN：熔断状态，超时 recoveryTimeoutMs 后转为 HALF_OPEN</li>
 *   <li>HALF_OPEN：半开状态，允许一个请求试探，成功次数达到 successThreshold 时转为 CLOSED，失败则回到 OPEN</li>
 * </ul>
 *
 * <p>线程安全：所有状态读写方法均使用 synchronized。
 */
public class CircuitBreaker {

    /**
     * 触发 OPEN所需的连续失败次数阈值。
     */
    private final int failureThreshold;
    /**
     * OPEN 到 HALF_OPEN 的超时时间（毫秒）。
     */
    private final long recoveryTimeoutMs;
    /**
     * HALF_OPEN 状态下判定熔断恢复所需的连续成功次数。
     */
    private final int successThreshold;
    private volatile State state = State.CLOSED;
    /**
     * CLOSED 状态下连续失败计数。
     */
    private int consecutiveFailures = 0;
    /**
     * HALF_OPEN 状态下连续成功计数。
     */
    private int consecutiveSuccesses = 0;
    /**
     * 最近一次失败的时间戳（毫秒）。
     */
    private long lastFailureTime = 0;

    /**
     * @param failureThreshold  触发熔断的连续失败次数（建议值 3-5）
     * @param recoveryTimeoutMs OPEN 到 HALF_OPEN 的超时时间（毫秒）
     * @param successThreshold  HALF_OPEN 状态下判定恢复所需的连续成功次数（建议值 2-4）
     */
    public CircuitBreaker(int failureThreshold, long recoveryTimeoutMs, int successThreshold) {
        this.failureThreshold = failureThreshold;
        this.recoveryTimeoutMs = recoveryTimeoutMs;
        this.successThreshold = successThreshold;
    }

    public State getState() {
        return state;
    }

    /**
     * 判断当前是否允许请求通过。
     *
     * <ul>
     *   <li>CLOSED：直接返回 true</li>
     *   <li>OPEN：检查超时，未超时返回 false，已超时转入 HALF_OPEN 并返回 true</li>
     *   <li>HALF_OPEN：直接返回 true（允许试探请求）</li>
     * </ul>
     *
     * @return true=允许，false=拒绝
     */
    public synchronized boolean allowRequest() {
        if (state == State.CLOSED) {
            return true;
        }
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime >= recoveryTimeoutMs) {
                state = State.HALF_OPEN;
                consecutiveSuccesses = 0;
                return true;
            }
            return false;
        }
        // HALF_OPEN: 允许一个请求试探
        return true;
    }

    /**
     * 记录一次成功执行。
     * <ul>
     *   <li>HALF_OPEN：连续成功计数，达到阈值时转入 CLOSED</li>
     *   <li>CLOSED：重置连续失败计数</li>
     * <li>OPEN：不处理</li>
     * </ul>
     */
    public synchronized void recordSuccess() {
        if (state == State.HALF_OPEN) {
            consecutiveSuccesses++;
            if (consecutiveSuccesses >= successThreshold) {
                state = State.CLOSED;
                consecutiveFailures = 0;
            }
        } else if (state == State.CLOSED) {
            consecutiveFailures = 0;
        }
    }

    /**
     * 记录一次失败执行。
     * <ul>
     *   <li>HALF_OPEN：立即转入 OPEN</li>
     *   <li>CLOSED：连续失败计数，达到阈值时转入 OPEN</li>
     *   <li>OPEN：不处理</li>
     * </ul>
     */
    public synchronized void recordFailure() {
        consecutiveFailures++;
        lastFailureTime = System.currentTimeMillis();
        if (state == State.HALF_OPEN) {
            state = State.OPEN;
        } else if (state == State.CLOSED && consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
        }
    }

    /**
     * 手动重置熔断器到 CLOSED 状态。
     */
    public synchronized void reset() {
        state = State.CLOSED;
        consecutiveFailures = 0;
        consecutiveSuccesses = 0;
        lastFailureTime = 0;
    }

    public enum State {
        /**
         * 正常状态，允许请求通过。
         */
        CLOSED,
        /**
         * 熔断状态，拒绝请求。
         */
        OPEN,
        /**
         * 半开状态，允许有限请求试探。
         */
        HALF_OPEN
    }
}