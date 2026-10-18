package com.zifang.z.agent.engine.agent.multi;

import java.util.Objects;

/**
 * 把一个 {@link WorkerAgent} 跟它的 {@link WorkerCapability} 绑在一起。
 * Supervisor 用它来做 routing 决策, 然后通过 {@link #worker()} 拿到底层 Agent。
 *
 * <p>设计上是不可变 POJO, 一旦注册到 supervisor 就不应再修改。
 */
public class WorkerDescriptor {

    private final WorkerAgent worker;
    private final WorkerCapability capability;

    public WorkerDescriptor(WorkerAgent worker, WorkerCapability capability) {
        this.worker = Objects.requireNonNull(worker, "worker");
        this.capability = Objects.requireNonNull(capability, "capability");
    }

    public static WorkerDescriptor of(WorkerAgent worker, WorkerCapability capability) {
        return new WorkerDescriptor(worker, capability);
    }

    public WorkerAgent worker() {
        return worker;
    }

    public WorkerCapability capability() {
        return capability;
    }

    public String name() {
        return worker.name();
    }
}
