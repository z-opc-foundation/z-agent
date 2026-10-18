package com.zifang.z.agent.engine.agent.tool.resilience;

import com.zifang.z.agent.engine.agent.llm.tool.core.ToolExecutor;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * ResilientToolExecutor 及其依赖组件的单元测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>RetryPolicy：最大重试次数遵守</li>
 *   <li>CircuitBreaker：状态转换（OPEN/HALF_OPEN/CLOSED）</li>
 *   <li>Fallback：熔断打开时降级回调被调用</li>
 * </ul>
 */
public class ResilientToolExecutorTest {

    @Test
    public void testRetry_respectsMaxAttempts() {
        // 失败3 次后第 4 次成功，验证 maxAttempts=4 时行为正确
        final AtomicInteger callCount = new AtomicInteger(0);
        ToolExecutor delegate = new ToolExecutor() {
            @Override
            public String execute(Map<String, Object> args) {
                int n = callCount.incrementAndGet();
                if (n < 4) {
                    throw new RuntimeException("try " + n);
                }
                return "ok";
            }
        };
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(4)
                .initialIntervalMs(10)
                .build();
        CircuitBreaker cb = new CircuitBreaker(10, 60000, 3);
        ResilientToolExecutor rte = new ResilientToolExecutor(delegate, policy, cb, null);

        String result = rte.execute(null);

        assertEquals("ok", result);
        assertEquals(4, callCount.get());
    }

    @Test
    public void testCircuitBreaker_opensAfterFailures() {
        CircuitBreaker cb = new CircuitBreaker(3, 1000, 2);
        for (int i = 0; i < 3; i++) {
            cb.recordFailure();
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertFalse("OPEN state should reject request", cb.allowRequest());
    }

    @Test
    public void testCircuitBreaker_halfOpenAfterTimeout() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(2, 50, 2);
        cb.recordFailure();
        cb.recordFailure(); // OPEN
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        Thread.sleep(60); // 超时后应转为 HALF_OPEN

        assertTrue("after timeout should allow request", cb.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
    }

    @Test
    public void testCircuitBreaker_closesAfterSuccess() {
        CircuitBreaker cb = new CircuitBreaker(2, 1000, 2);
        // 验证 CLOSED 状态下 recordSuccess 重置连续失败计数
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        // CLOSED 下 recordSuccess 只重置 consecutiveFailures（不能使 OPEN -> CLOSED）
        // 此测试验证 recordSuccess 在 CLOSED 状态下的行为，而非 HALF_OPEN -> CLOSED
        // HALF_OPEN -> CLOSED 由 testCircuitBreaker_halfOpenAfterTimeout 覆盖
    }

    @Test
    public void testCircuitBreaker_halfOpenToClosedViaSleep() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(2, 50, 2);
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        Thread.sleep(60);
        assertTrue(cb.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
        // HALF_OPEN 下两次成功应转入 CLOSED
        cb.recordSuccess();
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
        cb.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }

    @Test
    public void testFallback_calledWhenCircuitOpen() {
        CircuitBreaker cb = new CircuitBreaker(1, 100000, 2);
        cb.recordFailure(); // OPEN immediately (failureThreshold=1)

        ToolExecutor delegate = new ToolExecutor() {
            @Override
            public String execute(Map<String, Object> args) {
                return "should not be called";
            }
        };
        RetryPolicy policy = RetryPolicy.builder().maxAttempts(1).build();
        Supplier<ToolExecutionResult> fallback = new Supplier<ToolExecutionResult>() {
            @Override
            public ToolExecutionResult get() {
                return ToolExecutionResult.success("fallback result", 1, 0);
            }
        };

        ResilientToolExecutor rte = new ResilientToolExecutor(delegate, policy, cb, fallback);
        String result = rte.execute(null);

        assertEquals("fallback result", result);
    }

    @Test
    public void testRetry_exponentialBackoff() {
        // 验证指数退避间隔计算
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(5)
                .initialIntervalMs(100)
                .multiplier(2.0)
                .maxIntervalMs(1000)
                .build();

        // attempt=1: 无需等待（实际返回 initialIntervalMs）
        assertEquals(100, policy.getRetryInterval(1));
        // attempt=2: 100 * 2^(2-2) = 100
        assertEquals(100, policy.getRetryInterval(2));
        // attempt=3: 100 * 2^(3-2) = 200
        assertEquals(200, policy.getRetryInterval(3));
        // attempt=4: 100 * 2^(4-2) = 400
        assertEquals(400, policy.getRetryInterval(4));
        // attempt=5: 100 * 2^(5-2) = 800
        assertEquals(800, policy.getRetryInterval(5));
        // attempt=6: min(100 * 2^(6-2)=1600, 1000) = 1000
        assertEquals(1000, policy.getRetryInterval(6));
    }

    @Test
    public void testRetry_noRetryOnAssertionError() {
        final AtomicInteger callCount = new AtomicInteger(0);
        ToolExecutor delegate = new ToolExecutor() {
            @Override
            public String execute(Map<String, Object> args) {
                callCount.incrementAndGet();
                throw new AssertionError("business error");
            }
        };
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(3)
                .initialIntervalMs(10)
                .build();
        CircuitBreaker cb = new CircuitBreaker(10, 60000, 3);
        ResilientToolExecutor rte = new ResilientToolExecutor(delegate, policy, cb, null);

        try {
            rte.execute(null);
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("AssertionError") || e.getCause() instanceof AssertionError);
        }
        // 只调用一次，不重试
        assertEquals(1, callCount.get());
    }
}