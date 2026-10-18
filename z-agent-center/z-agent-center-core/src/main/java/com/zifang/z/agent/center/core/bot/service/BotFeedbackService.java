package com.zifang.z.agent.center.core.bot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.agent.center.core.bot.entity.BotFeedback;
import com.zifang.z.agent.center.core.bot.mapper.BotFeedbackMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * z-bot 用户反馈服务 — FEATURE014 Wishlist 4 (L2 反射层)
 *
 * <p>记录用户对每次 chat 的点赞 / 点踩 + 评论。
 * 后续 Hook 实现（Wishlist 5）会根据本服务的累计分数触发 skill 晋升 / prompt 优化。</p>
 */
@Service
public class BotFeedbackService extends ServiceImpl<BotFeedbackMapper, BotFeedback> {

    /**
     * 提交反馈（点赞 / 点踩 / 中性 + 可选评论）。
     */
    public BotFeedback submit(String instanceCode, String traceId, String userId,
                              int rating, String comment, String appCode, String skillCodes) {
        BotFeedback f = new BotFeedback();
        f.setInstanceCode(instanceCode);
        f.setTraceId(traceId);
        f.setUserId(userId);
        f.setRating(rating);
        f.setComment(comment);
        f.setAppCode(appCode);
        f.setSkillCodes(skillCodes);
        f.setTenantCode("default");
        f.setGmtCreate(LocalDateTime.now());
        f.setIsDeleted(0);
        this.save(f);
        return f;
    }

    /**
     * 累计反馈分数（点赞 +1, 点踩 -1, 中性 0）。
     */
    public int scoreForInstance(String instanceCode) {
        LambdaQueryWrapper<BotFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BotFeedback::getInstanceCode, instanceCode)
                .eq(BotFeedback::getIsDeleted, 0)
                .select(BotFeedback::getRating);
        List<BotFeedback> all = this.list(wrapper);
        int sum = 0;
        for (BotFeedback f : all) {
            sum += f.getRating() == null ? 0 : f.getRating();
        }
        return sum;
    }

    /**
     * 累计反馈分数（按 skill_code）— 用于 wishlist 6 决定是否晋升。
     */
    public int scoreForInstanceAndSkill(String instanceCode, String skillCode) {
        LambdaQueryWrapper<BotFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BotFeedback::getInstanceCode, instanceCode)
                .eq(BotFeedback::getIsDeleted, 0)
                .isNotNull(BotFeedback::getSkillCodes)
                .select(BotFeedback::getRating, BotFeedback::getSkillCodes);
        int sum = 0;
        for (BotFeedback f : this.list(wrapper)) {
            if (f.getSkillCodes() == null) continue;

            for (String s : f.getSkillCodes().split(",")) {
                if (s.trim().equals(skillCode)) {
                    sum += f.getRating() == null ? 0 : f.getRating();
                    break;
                }
            }
        }
        return sum;
    }

    public List<BotFeedback> recentForInstance(String instanceCode, int limit) {
        LambdaQueryWrapper<BotFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BotFeedback::getInstanceCode, instanceCode)
                .eq(BotFeedback::getIsDeleted, 0)
                .orderByDesc(BotFeedback::getGmtCreate)
                .last("limit " + Math.max(1, Math.min(limit, 200)));
        return this.list(wrapper);
    }
}
