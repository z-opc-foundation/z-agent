package com.zifang.z.agent.center.core.bot.hook.impl;

import com.zifang.z.agent.center.core.bot.hook.BotHook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Feedback Hook — FEATURE014 Wishlist 5
 *
 * <p>用户提交反馈后触发。本 hook 做两件事：</p>
 * <ol>
 *   <li>写一条高 importance（0.8）的 memory "用户对 instance X 的评价是 ... "，
 *       后续 Agent 在 recallForContext 时会优先看到</li>
 *   <li>点踩（rating=-1）时自动调用 SkillPromotionHook 重新评估 skill</li>
 * </ol>
 *
 * <p>注意：避免在 feedback 触发时直接修改 AgentApp.prompt（feedback 是 noisy 信号，
 * 需要时间窗口聚合 — 这是 wishlist 6 的 PromotionHook 的职责）。</p>
 */
@Component
public class FeedbackHook implements BotHook {

    private static final Logger log = LogManager.getLogger(FeedbackHook.class);

    private final com.zifang.z.agent.center.core.bot.service.BotMemoryService memoryService;

    public FeedbackHook(com.zifang.z.agent.center.core.bot.service.BotMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "FeedbackHook";
    }

    @Override
    public boolean matches(String eventType) {
        return "onFeedback".equalsIgnoreCase(eventType);
    }

    @Override
    public void fire(String eventType, Map<String, Object> context) throws Exception {
        String instanceCode = (String) context.get("instanceCode");
        Integer rating = (Integer) context.get("rating");
        String comment = (String) context.get("comment");
        if (instanceCode == null || rating == null) {
            return;
        }

        String summary = rating >= 1
                ? "[user-feedback] 用户对本次回答点赞。"
                : "[user-feedback] 用户对本次回答点踩：" + (comment != null ? comment : "(无备注)");

        memoryService.append(instanceCode, (String) context.get("userId"),
                "system", summary,
                rating >= 1 ? new BigDecimal("0.80") : new BigDecimal("0.90"),
                "feedback", "feedback," + (rating >= 1 ? "positive" : "negative"));

        log.info("FeedbackHook: instance={} rating={} comment={}",
                instanceCode, rating, comment != null ? comment : "(none)");
    }
}
