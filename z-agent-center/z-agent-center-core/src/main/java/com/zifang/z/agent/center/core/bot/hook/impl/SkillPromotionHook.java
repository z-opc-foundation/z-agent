package com.zifang.z.agent.center.core.bot.hook.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zifang.z.agent.center.core.app.mapper.AgentAppMapper;
import com.zifang.z.agent.center.core.bot.entity.BotSkillPromotion;
import com.zifang.z.agent.center.core.bot.hook.BotHook;
import com.zifang.z.agent.center.core.bot.mapper.BotSkillPromotionMapper;
import com.zifang.z.agent.center.core.bot.service.BotFeedbackService;
import com.zifang.z.agent.common.entity.AgentApp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Skill 自动晋升 Hook — FEATURE014 Wishlist 6 (L3 进化层核心)
 *
 * <p>onFeedback 触发：评估某个 skill 的累计反馈分数，跨过阈值后把 skill 写入
 * {@code z_agent_app.skillCodes}，相当于让 z-bot "学会"一个新技能。</p>
 *
 * <p>阈值：</p>
 * <ul>
 *   <li>累计分数 &ge; {@link #PROMOTE_THRESHOLD}（默认 +5）→ 加到 AgentApp.skillCodes</li>
 *   <li>累计分数 &le; {@link #DEMOTE_THRESHOLD}（默认 -3）→ 从 AgentApp.skillCodes 移除</li>
 * </ul>
 */
@Component
public class SkillPromotionHook implements BotHook {

    private static final Logger log = LogManager.getLogger(SkillPromotionHook.class);

    private static final int PROMOTE_THRESHOLD = 5;
    private static final int DEMOTE_THRESHOLD = -3;

    private final BotFeedbackService feedbackService;
    private final AgentAppMapper agentAppMapper;
    private final BotSkillPromotionMapper promotionMapper;

    public SkillPromotionHook(BotFeedbackService feedbackService,
                              AgentAppMapper agentAppMapper,
                              BotSkillPromotionMapper promotionMapper) {
        this.feedbackService = feedbackService;
        this.agentAppMapper = agentAppMapper;
        this.promotionMapper = promotionMapper;
    }

    @Override
    public String name() {
        return "SkillPromotionHook";
    }

    @Override
    public boolean matches(String eventType) {
        return "onFeedback".equalsIgnoreCase(eventType);
    }

    @Override
    public void fire(String eventType, Map<String, Object> context) throws Exception {
        String instanceCode = (String) context.get("instanceCode");
        String appCode = (String) context.get("appCode");
        String skillCodesCsv = (String) context.get("skillCodes");
        if (instanceCode == null || appCode == null) {
            return;
        }

        // 拆分 skillCodes
        List<String> skillCodes = skillCodesCsv == null || skillCodesCsv.isEmpty()
                ? Collections.emptyList()
                : Arrays.asList(skillCodesCsv.split(","));

        for (String skillCode : skillCodes) {
            String trimmed = skillCode.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int score = feedbackService.scoreForInstanceAndSkill(instanceCode, trimmed);
            if (score >= PROMOTE_THRESHOLD) {
                promote(instanceCode, appCode, trimmed, score, "add",
                        "feedback_score >= " + PROMOTE_THRESHOLD);
            } else if (score <= DEMOTE_THRESHOLD) {
                promote(instanceCode, appCode, trimmed, score, "remove",
                        "feedback_score <= " + DEMOTE_THRESHOLD);
            }
        }
    }

    /**
     * 晋升 / 降级：操作 z_agent_app.skillCodes（CSV 字符串）。
     */
    private void promote(String instanceCode, String appCode, String skillCode,
                         int score, String kind, String reason) {
        try {
            AgentApp app = agentAppMapper.selectOne(
                    new QueryWrapper<AgentApp>()
                            .eq("app_code", appCode)
                            .last("limit 1"));
            if (app == null) {
                log.warn("SkillPromotionHook: no app found for appCode={}", appCode);
                return;
            }
            String currentCsv = app.getSkillCodes();
            List<String> list = currentCsv == null || currentCsv.isEmpty()
                    ? new ArrayList<>()
                    : new ArrayList<>(Arrays.asList(currentCsv.split(",")));
            list.removeAll(Arrays.asList("", null));

            boolean changed = false;
            if ("add".equals(kind) && !list.contains(skillCode)) {
                list.add(skillCode);
                changed = true;
            } else if ("remove".equals(kind)) {
                changed = list.remove(skillCode);
            }
            if (!changed) {
                return;
            }

            String newCsv = String.join(",", list);
            // 写回 — LambdaUpdateWrapper 走 MyBatis-Plus
            LambdaUpdateWrapper<AgentApp> update = new LambdaUpdateWrapper<>();
            update.eq(AgentApp::getAppCode, appCode)
                    .set(AgentApp::getSkillCodes, newCsv)
                    .set(AgentApp::getGmtModified, LocalDateTime.now());
            int rows = agentAppMapper.update(null, update);
            if (rows == 0) {
                log.warn("SkillPromotionHook: update affected 0 rows for appCode={}", appCode);
                return;
            }

            // 记 promotion 记录
            BotSkillPromotion promo = new BotSkillPromotion();
            promo.setInstanceCode(instanceCode);
            promo.setSkillCode(skillCode);
            promo.setAppCode(appCode);
            promo.setPromotionKind(kind);
            promo.setReason(reason);
            promo.setFeedbackScore(score);
            promo.setTenantCode("default");
            promo.setGmtCreate(LocalDateTime.now());
            promotionMapper.insert(promo);

            log.info("SkillPromotionHook: {} {} (score={}, reason={}) into appCode={}",
                    kind, skillCode, score, reason, appCode);
        } catch (Exception e) {
            log.error("SkillPromotionHook failed: instance={} skill={} kind={}",
                    instanceCode, skillCode, kind, e);
        }
    }
}
