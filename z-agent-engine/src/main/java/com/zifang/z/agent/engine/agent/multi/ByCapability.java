package com.zifang.z.agent.engine.agent.multi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 按 capability.tags 匹配: 任何一个 worker tag 命中 query tags, 就会被选中。
 * 这是最常用的策略, supervisor 根据 LLM 解析出的 tags 决定派给谁。
 */
public class ByCapability implements RoutingStrategy {

    @Override
    public List<WorkerDescriptor> route(RoutingContext context) {
        List<String> queryTags = context.tags();
        if (queryTags == null || queryTags.isEmpty()) {
            return new ArrayList<>();
        }
        return context.allWorkers().stream()
                .filter(w -> w.capability().matchesAny(queryTags))
                .collect(Collectors.toList());
    }
}
