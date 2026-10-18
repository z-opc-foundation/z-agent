package com.zifang.z.agent.engine.agent.multi;

import java.util.List;

/**
 * Supervisor 用来挑 worker 的路由策略。
 *
 * <p>典型实现:
 * <ul>
 *   <li>{@link ByCapability} - 按 tags 匹配, 命中全部委托</li>
 *   <li>{@link Broadcast} - 委托给所有 worker (fan-out)</li>
 *   <li>{@link FirstMatch} - 只挑第一个命中的 worker (fan-in by order)</li>
 * </ul>
 */
public interface RoutingStrategy {

    /**
     * 给定上下文 (任务描述 + 可选 tags), 返回应被委托的 worker 列表。
     * 返回空列表 = "没人能接" — supervisor 会把任务直接交给自己的 delegate 处理。
     */
    List<WorkerDescriptor> route(RoutingContext context);

    /**
     * 路由决策的上下文。
     */
    final class RoutingContext {
        private final String task;
        private final List<String> tags;
        private final List<WorkerDescriptor> allWorkers;

        public RoutingContext(String task, List<String> tags, List<WorkerDescriptor> allWorkers) {
            this.task = task;
            this.tags = tags;
            this.allWorkers = allWorkers;
        }

        public String task() {
            return task;
        }

        public List<String> tags() {
            return tags;
        }

        public List<WorkerDescriptor> allWorkers() {
            return allWorkers;
        }
    }
}
