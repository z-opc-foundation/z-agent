package com.zifang.z.agent.engine.agent.multi;

import java.util.ArrayList;
import java.util.List;

/**
 * 委托给所有 worker (fan-out)。每个 worker 都跑一遍, 结果汇总结。
 * 适用于"让所有人过一遍"的广播场景 (如全集群巡检、多模型对比等)。
 */
public class Broadcast implements RoutingStrategy {

    @Override
    public List<WorkerDescriptor> route(RoutingContext context) {
        return new ArrayList<>(context.allWorkers());
    }
}
