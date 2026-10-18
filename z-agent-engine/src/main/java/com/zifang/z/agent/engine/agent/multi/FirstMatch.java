package com.zifang.z.agent.engine.agent.multi;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 只挑第一个命中的 worker (按注册顺序)。
 * 适用于"主备链"场景: 第一个 worker 失败就 Skip, 不并发出第二份。
 */
public class FirstMatch implements RoutingStrategy {

    @Override
    public List<WorkerDescriptor> route(RoutingContext context) {
        List<String> queryTags = context.tags();
        if (queryTags == null || queryTags.isEmpty()) {
            return Collections.emptyList();
        }
        return context.allWorkers().stream()
                .filter(w -> w.capability().matchesAny(queryTags))
                .limit(1)
                .collect(Collectors.toList());
    }
}
