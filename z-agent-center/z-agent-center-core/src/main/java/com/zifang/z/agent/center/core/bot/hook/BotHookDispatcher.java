package com.zifang.z.agent.center.core.bot.hook;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.bot.entity.BotHookEvent;
import com.zifang.z.agent.center.core.bot.mapper.BotHookEventMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Bot Hook 分发器 — FEATURE014 Wishlist 5
 *
 * <p>Spring 启动时把所有 {@link BotHook} 实现类注入，事件触发时按顺序调用匹配的实现。
 * 每次 fire 都写一行 {@code z_bot_hook_event} 审计（无论 hook 成功失败）。</p>
 */
@Component
public class BotHookDispatcher {

    private static final Logger log = LogManager.getLogger(BotHookDispatcher.class);
    @Autowired
    private List<BotHook> hooks;
    @Autowired
    private BotHookEventMapper eventMapper;

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }

        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * 触发一个事件。
     *
     * <p>不抛异常 — 即使所有 hook 都失败也不会中断调用方。失败情况记到 z_bot_hook_event
     * 和 SLF4J 日志。</p>
     *
     * @return 实际触发的 hook 数（成功+失败的合计）
     */
    public int fire(String eventType, Map<String, Object> context) {
        if (hooks == null || hooks.isEmpty()) {
            return 0;
        }
        String instanceCode = context == null ? null
                : String.valueOf(context.get("instanceCode"));
        int triggered = 0;
        for (BotHook hook : hooks) {
            if (!hook.matches(eventType)) {
                continue;
            }

            triggered++;
            fireOne(hook, eventType, instanceCode, context);
        }
        return triggered;
    }

    private void fireOne(BotHook hook, String eventType, String instanceCode,
                         Map<String, Object> context) {
        long t0 = System.currentTimeMillis();
        BotHookEvent event = new BotHookEvent();
        event.setInstanceCode(instanceCode);
        event.setHookName(hook.name());
        event.setEventType(eventType);
        event.setTenantCode("default");
        event.setGmtCreate(LocalDateTime.now());
        try {
            event.setPayload(JsonUtil.toJsonExcludingNulls(context));
            hook.fire(eventType, context);
            event.setResult("SUCCESS");
            event.setDurationMs(System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.error("[hook:{}] {} failed: {}", hook.name(), eventType, e.getMessage(), e);
            event.setResult("FAIL");
            event.setErrorMsg(truncate(e.getMessage(), 500));
            event.setDurationMs(System.currentTimeMillis() - t0);
        } finally {
            try {
                eventMapper.insert(event);
            } catch (Exception ex) {
                log.warn("Failed to persist hook event {}: {}", hook.name(), ex.getMessage());
            }
        }
    }
}
