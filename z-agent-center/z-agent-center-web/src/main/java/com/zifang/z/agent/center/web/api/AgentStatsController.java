package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.stats.AgentStatsService;
import com.zifang.z.agent.center.core.agent.stats.dto.*;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FEATURE013 T3.2: Agent 监控面板 REST API
 *
 * <p>所有端点 GET + JSON，路径前缀 {@code /api/agent/stats}.
 *
 * <p>前端 AgentDashboard.jsx 调用:
 * <pre>
 *   GET /api/agent/stats/overview       → KPI 卡片
 *   GET /api/agent/stats/trend          → 7/30 天趋势图
 *   GET /api/agent/stats/latency        → 延迟分布直方图
 *   GET /api/agent/stats/by-app         → 按应用统计表 / TOP-N
 *   GET /api/agent/stats/by-user        → 按用户统计表 / TOP-N
 * </pre>
 */
@Tag(name = "Agent 监控面板")
@RestController
@RequestMapping("/api/agent/stats")
public class AgentStatsController {

    @Resource
    private AgentStatsService agentStatsService;

    /**
     * 将空字符串或纯空白字符串归一化为 {@code null}.
     *
     * @param s 原始字符串
     * @return 去除两端空白后的字符串，若为空或纯空白则返回 {@code null}
     */
    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /**
     * 顶部 KPI 总览（累计 + 今日）.
     *
     * @param appCode 可选应用编码过滤
     * @return 统计总览 DTO
     */
    @Operation(summary = "顶部 KPI 总览（累计 + 今日）")
    @GetMapping("/overview")
    @BizApi(summary = "顶部 KPI 总览（累计 + 今日）", description = "顶部 KPI 总览（累计 + 今日）.", tags = { "z-agent" })
    public Result<AgentStatsOverview> overview(
            @Parameter(description = "可选应用编码过滤") @RequestParam(required = false)
            @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "可选应用编码过滤", position = BizParam.Position.QUERY) String appCode) {
        return Result.success(agentStatsService.getOverview(emptyToNull(appCode)));
    }

    /**
     * 近 N 天每日趋势（默认 7 天，最大 90 天）.
     *
     * @param appCode 可选应用编码过滤
     * @param days    天数，默认 7
     * @return 每日趋势点列表
     */
    @Operation(summary = "近 N 天每日趋势（默认 7 天，最大 90 天）")
    @GetMapping("/trend")
    @BizApi(summary = "近 N 天每日趋势（默认 7 天，最大 90 天）", description = "近 N 天每日趋势（默认 7 天，最大 90 天）.", tags = { "z-agent" })
    public Result<List<AgentTrendPoint>> trend(
            @RequestParam(required = false)
            @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "可选应用编码过滤", position = BizParam.Position.QUERY) String appCode,
            @Parameter(description = "天数, 默认 7") @RequestParam(defaultValue = "7")
            @BizParam(name = "days", type = BizField.FieldType.INTEGER, description = "天数，默认 7", position = BizParam.Position.QUERY) Integer days) {
        return Result.success(agentStatsService.getTrend(emptyToNull(appCode), days));
    }

    /**
     * 延迟分布直方图（6 桶固定区间）.
     *
     * @param appCode 可选应用编码过滤
     * @return 延迟桶列表
     */
    @Operation(summary = "延迟分布直方图（6 桶固定区间）")
    @GetMapping("/latency")
    @BizApi(summary = "延迟分布直方图（6 桶固定区间）", description = "延迟分布直方图（6 桶固定区间）.", tags = { "z-agent" })
    public Result<List<AgentLatencyBucket>> latency(
            @RequestParam(required = false)
            @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "可选应用编码过滤", position = BizParam.Position.QUERY) String appCode) {
        return Result.success(agentStatsService.getLatencyDistribution(emptyToNull(appCode)));
    }

    /**
     * 按 Agent 应用聚合统计（TOP-N）.
     *
     * @param appCode 可选应用编码过滤
     * @param limit   TOP N，默认 20，最大 200
     * @return 按应用聚合的行数据列表
     */
    @Operation(summary = "按 Agent 应用聚合统计（TOP-N，默认 20）")
    @GetMapping("/by-app")
    @BizApi(summary = "按 Agent 应用聚合统计（TOP-N）", description = "按 Agent 应用聚合统计（TOP-N）.", tags = { "z-agent" })
    public Result<List<AgentByAppRow>> byApp(
            @RequestParam(required = false)
            @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "可选应用编码过滤", position = BizParam.Position.QUERY) String appCode,
            @Parameter(description = "TOP N, 默认 20, 最大 200") @RequestParam(defaultValue = "20")
            @BizParam(name = "limit", type = BizField.FieldType.INTEGER, description = "TOP N，默认 20，最大 200", position = BizParam.Position.QUERY) Integer limit) {
        return Result.success(agentStatsService.getByApp(emptyToNull(appCode), limit));
    }

    /**
     * 按用户聚合统计（TOP-N）.
     *
     * @param appCode 可选应用编码过滤
     * @param limit   TOP N，默认 20，最大 200
     * @return 按用户聚合的行数据列表
     */
    @Operation(summary = "按用户聚合统计（TOP-N，默认 20）")
    @GetMapping("/by-user")
    @BizApi(summary = "按用户聚合统计（TOP-N）", description = "按用户聚合统计（TOP-N）.", tags = { "z-agent" })
    public Result<List<AgentByUserRow>> byUser(
            @RequestParam(required = false)
            @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "可选应用编码过滤", position = BizParam.Position.QUERY) String appCode,
            @Parameter(description = "TOP N, 默认 20, 最大 200") @RequestParam(defaultValue = "20")
            @BizParam(name = "limit", type = BizField.FieldType.INTEGER, description = "TOP N，默认 20，最大 200", position = BizParam.Position.QUERY) Integer limit) {
        return Result.success(agentStatsService.getByUser(emptyToNull(appCode), limit));
    }

    /**
     * 健康检查端点.
     *
     * @return 固定字符串，表示 Stats API 健康
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    @BizApi(summary = "健康检查端点", description = "健康检查端点.", tags = { "z-agent" })
    public Result<String> health() {
        return Result.success("Agent Stats API is healthy");
    }
}



