package com.zifang.z.agent.engine.agent.multi;

/**
 * Worker 失败时的策略。
 *
 * <p>注意: 这个接口是 *声明式* 的, 只回答"下一个 worker 是什么"。
 * 真正执行 retry / 等待超时是 supervisor 的责任 (它持有 message queue 和线程)。
 */
public interface FailoverPolicy {

    /**
     * 重试 N 次同一个 worker, 然后 Skip。
     */
    static FailoverPolicy retryThenSkip(int maxAttempts) {
        return (failed, attempt, all) -> {
            if (attempt < maxAttempts) {
                return Decision.retry(failed, maxAttempts);
            }
            return Decision.skip(maxAttempts);
        };
    }

    /**
     * 重试 N 次同一个 worker, 然后 Abort。
     */
    static FailoverPolicy retryThenAbort(int maxAttempts) {
        return (failed, attempt, all) -> {
            if (attempt < maxAttempts) {
                return Decision.retry(failed, maxAttempts);
            }
            return Decision.abort(maxAttempts);
        };
    }

    /**
     * 失败立即 Skip (默认值, 最保守)。
     */
    static FailoverPolicy skip() {
        return (failed, attempt, all) -> Decision.skip(1);
    }

    // ===== 内置策略 =====

    /**
     * 失败立即 Abort。
     */
    static FailoverPolicy abort() {
        return (failed, attempt, all) -> Decision.abort(1);
    }

    /**
     * 上一个 worker 失败后, 决定下一步动作。
     *
     * @param failed     失败的 worker descriptor (可能为 null, 表示根本没选到 worker)
     * @param attempt    当前第几次尝试 (从 1 开始)
     * @param allWorkers 当前可用的全部 worker
     * @return 决策
     */
    Decision onFailure(WorkerDescriptor failed, int attempt, java.util.List<WorkerDescriptor> allWorkers);

    enum Action {
        /**
         * 重试同一个 worker
         */
        RETRY_SAME,
        /**
         * 换下一个候选 worker (由 policy 内部排序决定)
         */
        TRY_NEXT,
        /**
         * 跳过失败, 接受当前部分结果继续
         */
        SKIP,
        /**
         * 立即终止, 抛出异常给 supervisor
         */
        ABORT
    }

    final class Decision {
        private final Action action;
        private final WorkerDescriptor next;
        private final int maxAttempts;

        public Decision(Action action, WorkerDescriptor next, int maxAttempts) {
            this.action = action;
            this.next = next;
            this.maxAttempts = maxAttempts;
        }

        public static Decision retry(WorkerDescriptor same, int max) {
            return new Decision(Action.RETRY_SAME, same, max);
        }

        public static Decision next(WorkerDescriptor nxt, int max) {
            return new Decision(Action.TRY_NEXT, nxt, max);
        }

        public static Decision skip(int max) {
            return new Decision(Action.SKIP, null, max);
        }

        public static Decision abort(int max) {
            return new Decision(Action.ABORT, null, max);
        }

        public Action action() {
            return action;
        }

        public WorkerDescriptor next() {
            return next;
        }

        public int maxAttempts() {
            return maxAttempts;
        }
    }
}
