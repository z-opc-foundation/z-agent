package com.zifang.z.agent.center.core.bot.hook.impl;

import com.zifang.z.agent.center.core.bot.hook.BotHook;
import com.zifang.z.agent.center.core.bot.service.BotFeedbackService;
import com.zifang.z.agent.center.core.bot.service.BotMemoryService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Memory 合并 Hook — FEATURE014 Wishlist 5
 *
 * <p>postChat 触发：把最近 K 条 chat memory 做一次"合并写一条 summary"。
 * 当前是简化版：写一条 "[chat-summary] ... user 问了 X，assistant 答了 Y"，保留最近 10 条原文。
 *
 * <p>进阶：未来接入 LLM 时，让 LLM 总结用户偏好 / 关键事实再写 summary。</p>
 */
@Component
public class MemoryConsolidationHook implements BotHook {

    private static final Logger log = LogManager.getLogger(MemoryConsolidationHook.class);

    private final BotMemoryService memoryService;
    private final BotFeedbackService feedbackService;

    public MemoryConsolidationHook(BotMemoryService memoryService,
                                   BotFeedbackService feedbackService) {
        this.memoryService = memoryService;
        this.feedbackService = feedbackService;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "(empty)";
        }

        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @Override
    public String name() {
        return "MemoryConsolidationHook";
    }

    @Override
    public boolean matches(String eventType) {
        return "postChat".equalsIgnoreCase(eventType);
    }

    @Override
    public void fire(String eventType, Map<String, Object> context) throws Exception {
        String instanceCode = (String) context.get("instanceCode");
        String userId = (String) context.get("userId");
        String message = (String) context.get("message");
        String reply = (String) context.get("reply");
        if (instanceCode == null) {
            return;
        }

        // 简化版：把这一轮对话存为 memory 条目，importance 中等
        String summary = String.format("[chat-summary] user: %s | assistant: %s",
                truncate(message, 100),
                truncate(reply, 200));
        memoryService.append(instanceCode, userId, "summary",
                summary,
                new BigDecimal("0.65"),
                "consolidation",
                "chat-summary,auto");

        // 把累计反馈分数作为 memory，方便 LLM 知道"我的整体口碑"
        int score = feedbackService.scoreForInstance(instanceCode);
        if (score != 0) {
            memoryService.append(instanceCode, null, "system",
                    "[reputation] cumulative feedback score: " + score
                            + " (positive=赞, negative=踩)",
                    new BigDecimal("0.70"), "consolidation", "reputation");
        }
        log.debug("MemoryConsolidationHook: instance={} score={}", instanceCode, score);
    }
}
