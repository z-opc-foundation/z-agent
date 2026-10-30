package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.BaseStatusCode;
import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.bot.entity.BotFeedback;
import com.zifang.z.agent.center.core.bot.hook.BotHookDispatcher;
import com.zifang.z.agent.center.core.bot.service.BotFeedbackService;
import com.zifang.z.agent.center.web.api.response.dto.FeedbackScoreResp;
import com.zifang.z.agent.center.web.api.response.dto.FeedbackSubmitResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;

/**
 * z-bot 用户反馈 API — FEATURE014 Wishlist 4
 *
 * <p>由 AgentChatController 在 chat 返回时把 trace_id 一起返回，
 * 前端在用户点赞 / 点踩时回调 {@code POST /api/bot/feedback}。</p>
 */
@Tag(name = "z-bot 反馈")
@RestController
@RequestMapping("/api/bot/feedback")
public class BotFeedbackController {

    @Resource
    private BotFeedbackService feedbackService;

    @Resource
    private BotHookDispatcher hookDispatcher;

    /**
     * FEATURE014 提交反馈（点赞 / 点踩 / 中性 + 备注）.
     * <p>提交后会触发 {@code onFeedback} Hook（FeedbackHook + SkillPromotionHook）。
     *
     * @param req 反馈请求体（instanceCode / traceId / userId / rating / comment / appCode / skillCodes）
     * @return 包含反馈 ID 与 hooksFired 计数的响应 Map
     */
    @Operation(summary = "FEATURE014 提交反馈（点赞 / 点踩 / 中性 + 备注）")
    @PostMapping
    public Result<FeedbackSubmitResp> submit(@RequestBody FeedbackReq req) {
        if (req.instanceCode == null) {
            return Result.<FeedbackSubmitResp>fail("instanceCode is required").code(BaseStatusCode.FAIL.getCode());
        }
        if (req.rating == null) {
            return Result.<FeedbackSubmitResp>fail("rating is required (-1 / 0 / 1)").code(BaseStatusCode.FAIL.getCode());
        }
        BotFeedback saved = feedbackService.submit(
                req.instanceCode, req.traceId, req.userId,
                req.rating, req.comment, req.appCode, req.skillCodes);

        // 触发 onFeedback hook（FeedbackHook + SkillPromotionHook）
        java.util.Map<String, Object> ctx = new HashMap<>();
        ctx.put("instanceCode", req.instanceCode);
        ctx.put("userId", req.userId);
        ctx.put("traceId", req.traceId);
        ctx.put("rating", req.rating);
        ctx.put("comment", req.comment);
        ctx.put("appCode", req.appCode);
        ctx.put("skillCodes", req.skillCodes);
        int hooksFired = hookDispatcher.fire("onFeedback", ctx);

        FeedbackSubmitResp resp = new FeedbackSubmitResp();
        resp.setId(saved.getId());
        resp.setHooksFired(hooksFired);
        return Result.success(resp);
    }

    /**
     * 查询一个实例的最近反馈.
     *
     * @param instanceCode Agent 实例编码
     * @param limit        返回条数上限，默认 20
     * @return 反馈列表
     */
    @Operation(summary = "查询一个实例的最近反馈")
    @GetMapping("/recent")
    public Result<List<BotFeedback>> recent(@RequestParam String instanceCode,
                                            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(feedbackService.recentForInstance(instanceCode, limit));
    }

    /**
     * 查询一个实例的累计反馈分数（+赞 / -踩）.
     *
     * @param instanceCode Agent 实例编码
     * @return 包含 instanceCode / score 的响应 Map
     */
    @Operation(summary = "查询一个实例的累计反馈分数（+赞 / -踩）")
    @GetMapping("/score")
    public Result<FeedbackScoreResp> score(@RequestParam String instanceCode) {
        FeedbackScoreResp resp = new FeedbackScoreResp();
        resp.setInstanceCode(instanceCode);
        resp.setScore(feedbackService.scoreForInstance(instanceCode));
        return Result.success(resp);
    }

    /**
     * 反馈请求体.
     */
    public static class FeedbackReq {
        public String instanceCode;
        public String traceId;
        public String userId;
        /**
         * 1=赞, -1=踩, 0=中性
         */
        public Integer rating;
        public String comment;
        public String appCode;
        /**
         * 逗号分隔（影响 SkillPromotionHook）
         */
        public String skillCodes;
    }
}
