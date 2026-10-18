package com.zifang.z.agent.center.core.bot.hook.impl;

import com.zifang.z.agent.center.core.bot.hook.BotHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Skill 加载追踪 Hook — FEATURE014 Wishlist 5
 *
 * <p>每次 preSkill / postSkill 触发时记录：哪个 instance、哪个 skill、耗时多少、是否成功。
 * 数据落到 z_bot_hook_event（dispatcher 已经写），本 hook 还做一件事：
 * 写一条中等重要度的 memory（importance=0.6），方便后续 Agent 知道"最近一次同步是什么时候"。</p>
 */
@Component
public class SkillTraceHook implements BotHook {

    private static final Logger log = LogManager.getLogger(SkillTraceHook.class);

    private final com.zifang.z.agent.center.core.bot.service.BotMemoryService memoryService;

    public SkillTraceHook(com.zifang.z.agent.center.core.bot.service.BotMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "SkillTraceHook";
    }

    @Override
    public boolean matches(String eventType) {
        return "postSkill".equalsIgnoreCase(eventType);
    }

    @Override
    public void fire(String eventType, Map<String, Object> context) throws Exception {
        String instanceCode = (String) context.get("instanceCode");
        String skillCode = (String) context.get("skillCode");
        Object result = context.get("result");
        boolean success = result == null || !"FAIL".equals(result.toString());
        if (instanceCode == null || skillCode == null) {
            return;
        }

        // 写一条 audit-level memory（importance=0.4 — 较低，避免污染长期记忆）
        memoryService.append(instanceCode, null, "system",
                "[skill-trace] " + skillCode + " " + (success ? "loaded" : "FAILED")
                        + " @ " + java.time.LocalDateTime.now(),
                new BigDecimal("0.40"), "skill-trace", "skill-trace,audit");
        log.debug("SkillTraceHook: instance={} skill={} success={}", instanceCode, skillCode, success);
    }
}
