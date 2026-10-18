package com.zifang.z.agent.center.core.skill;

import com.zifang.z.agent.center.core.skill.adapter.SkillAdapter;
import com.zifang.z.agent.common.enums.AgentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SkillAdapter 工厂 — FEATURE014 §2.4
 *
 * <p>按 AgentType 分发到对应的 SkillAdapter 实现。
 * 所有 {@link SkillAdapter} 子类都以 Spring Bean 形式注入，按
 * {@link SkillAdapter#getAgentType()} 索引。</p>
 */
@Component
public class SkillAdapterFactory {

    private final Map<AgentType, SkillAdapter> adaptersByType = new HashMap<>();

    @Autowired
    public SkillAdapterFactory(List<SkillAdapter> adapters) {
        for (SkillAdapter adapter : adapters) {
            adaptersByType.put(adapter.getAgentType(), adapter);
            // log is not yet available in constructor; rely on caller-side log
        }
        org.apache.logging.log4j.LogManager.getLogger(SkillAdapterFactory.class)
                .info("SkillAdapterFactory initialized with {} adapters: {}",
                        adaptersByType.size(), adaptersByType.keySet());
    }

    /**
     * 获取指定 AgentType 对应的 SkillAdapter。
     */
    public SkillAdapter getAdapter(AgentType agentType) {
        SkillAdapter adapter = adaptersByType.get(agentType);
        if (adapter == null) {
            throw new RuntimeException("No SkillAdapter registered for agentType=" + agentType);
        }
        return adapter;
    }

    /**
     * 列出所有已注册的 AgentType。
     */
    public java.util.Set<AgentType> registeredTypes() {
        return adaptersByType.keySet();
    }
}
