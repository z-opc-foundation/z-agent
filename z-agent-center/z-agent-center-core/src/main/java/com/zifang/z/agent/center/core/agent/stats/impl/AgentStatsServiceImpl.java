package com.zifang.z.agent.center.core.agent.stats.impl;

import com.zifang.z.agent.center.core.agent.stats.AgentStatsService;
import com.zifang.z.agent.center.core.agent.stats.dto.*;
import com.zifang.z.agent.center.core.agent.stats.mapper.AgentStatsMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FEATURE013 T3.2: Agent 监控面板 Service 实现
 *
 * <p>所有方法走 {@link AgentStatsMapper} 的纯 SQL 聚合（不下拉全表），对生产
 * 数据量（百万级对话）线性可扩展. 失败容忍：单条 SQL 异常被吞, 返回空集 —
 * 监控页面不能因为统计失败而 5xx.
 */
@Service
public class AgentStatsServiceImpl implements AgentStatsService {

    private static final Logger log = LogManager.getLogger(AgentStatsServiceImpl.class);

    @Resource
    private AgentStatsMapper agentStatsMapper;

    private static Long asLong(Object o) {
        if (o == null) {
            return 0L;
        }

        if (o instanceof Number) {
            return ((Number) o).longValue();
        }

        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static Integer asInteger(Object o) {
        if (o == null) {
            return 0;
        }

        if (o instanceof Number) {
            return ((Number) o).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return 0;
        }
    }

    private static Double asDouble(Object o) {
        if (o == null) {
            return 0d;
        }

        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }

        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return 0d;
        }
    }

    private static String asString(Object o) {
        if (o == null) {
            return null;
        }

        String s = String.valueOf(o);
        return s.isEmpty() || "null".equals(s) ? null : s;
    }

    @Override
    public AgentStatsOverview getOverview(String appCode) {
        AgentStatsOverview o = new AgentStatsOverview();
        try {
            // 累计：从 90 天前起算（避免全表扫，性能与代表性折中）
            LocalDateTime since90 = LocalDateTime.now().minusDays(90);
            Map<String, Object> total = agentStatsMapper.overviewTotal(appCode, since90);
            if (total != null) {
                o.setTotalConversations(asLong(total.get("total_conversations")));
                o.setSuccessCount(asLong(total.get("success_count")));
                o.setFailedCount(asLong(total.get("failed_count")));
                o.setTotalTokens(asLong(total.get("total_tokens")));
                o.setAvgTokens(asDouble(total.get("avg_tokens")));
                o.setAvgLatencyMs(asDouble(total.get("avg_latency_ms")));
                o.setActiveAppCount(asLong(total.get("active_app_count")));
                o.setActiveUserCount(asLong(total.get("active_user_count")));
                // 成功率
                long totalCnt = o.getTotalConversations() == null ? 0L : o.getTotalConversations();
                long successCnt = o.getSuccessCount() == null ? 0L : o.getSuccessCount();
                o.setSuccessRate(totalCnt == 0 ? 0d : ((double) successCnt) / totalCnt);
            }

            // 今日
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            Map<String, Object> today = agentStatsMapper.overviewToday(appCode, todayStart);
            if (today != null) {
                o.setTodayConversations(asLong(today.get("today_conversations")));
                o.setTodayTokens(asLong(today.get("today_tokens")));
                o.setTodayFailedCount(asLong(today.get("today_failed_count")));
            }

            // P95 延迟（独立查询, 单独 try）
            try {
                Long p95 = agentStatsMapper.p95LatencyMs(appCode);
                o.setP95LatencyMs(p95);
            } catch (Exception ex) {
                log.warn("[AgentStats] p95LatencyMs 失败 appCode={}: {}", appCode, ex.getMessage());
            }
        } catch (Exception e) {
            log.warn("[AgentStats] getOverview 失败 appCode={}: {}", appCode, e.getMessage());
        }
        return o;
    }

    // ========== 类型安全转换辅助 ==========

    @Override
    public List<AgentTrendPoint> getTrend(String appCode, int days) {
        if (days <= 0 || days > 90) {
            days = 7;
        }
        List<AgentTrendPoint> result = new ArrayList<>();
        try {
            LocalDateTime since = LocalDate.now().minusDays(days - 1L).atStartOfDay();
            List<Map<String, Object>> rows = agentStatsMapper.trend(appCode, since);

            // 补全缺失日期（让前端图表横轴连续）
            Map<String, Map<String, Object>> byDate = new java.util.HashMap<>();
            for (Map<String, Object> row : rows) {
                byDate.put(String.valueOf(row.get("d")), row);
            }
            for (int i = days - 1; i >= 0; i--) {
                String dateKey = LocalDate.now().minusDays(i).toString();
                Map<String, Object> row = byDate.get(dateKey);
                AgentTrendPoint p = new AgentTrendPoint();
                p.setDate(dateKey);
                if (row != null) {
                    p.setDailyConversations(asLong(row.get("cnt")));
                    p.setDailyTokens(asLong(row.get("sum_tokens")));
                    p.setDailySuccess(asLong(row.get("success_cnt")));
                    p.setDailyFailed(asLong(row.get("failed_cnt")));
                    p.setAvgLatencyMs(asDouble(row.get("avg_latency")));
                } else {
                    p.setDailyConversations(0L);
                    p.setDailyTokens(0L);
                    p.setDailySuccess(0L);
                    p.setDailyFailed(0L);
                    p.setAvgLatencyMs(0d);
                }
                result.add(p);
            }
        } catch (Exception e) {
            log.warn("[AgentStats] getTrend 失败 appCode={}, days={}: {}", appCode, days, e.getMessage());
        }
        return result;
    }

    @Override
    public List<AgentLatencyBucket> getLatencyDistribution(String appCode) {
        List<AgentLatencyBucket> result = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = agentStatsMapper.latencyDistribution(appCode);
            for (Map<String, Object> row : rows) {
                AgentLatencyBucket b = new AgentLatencyBucket();
                b.setLabel(String.valueOf(row.get("bucket")));
                b.setLowerMs(asInteger(row.get("lower_ms")));
                b.setUpperMs(asInteger(row.get("upper_ms")));
                b.setCount(asLong(row.get("cnt")));
                b.setAvgLatencyMs(asDouble(row.get("avg_latency")));
                result.add(b);
            }
        } catch (Exception e) {
            log.warn("[AgentStats] getLatencyDistribution 失败 appCode={}: {}", appCode, e.getMessage());
        }
        return result;
    }

    @Override
    public List<AgentByAppRow> getByApp(String appCode, int limit) {
        if (limit <= 0 || limit > 200) {
            limit = 20;
        }
        List<AgentByAppRow> result = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = agentStatsMapper.byApp(appCode, limit);
            for (Map<String, Object> row : rows) {
                AgentByAppRow r = new AgentByAppRow();
                r.setAppCode(asString(row.get("app_code")));
                r.setAppName(asString(row.get("app_name")));
                long total = asLong(row.get("total_calls"));
                long success = asLong(row.get("success_count"));
                r.setTotalCalls(total);
                r.setSuccessCount(success);
                r.setFailedCount(asLong(row.get("failed_count")));
                r.setSuccessRate(total == 0 ? 0d : ((double) success) / total);
                r.setTotalTokens(asLong(row.get("total_tokens")));
                r.setAvgTokens(asDouble(row.get("avg_tokens")));
                r.setAvgLatencyMs(asDouble(row.get("avg_latency_ms")));
                result.add(r);
            }
        } catch (Exception e) {
            log.warn("[AgentStats] getByApp 失败 appCode={}, limit={}: {}", appCode, limit, e.getMessage());
        }
        return result;
    }

    @Override
    public List<AgentByUserRow> getByUser(String appCode, int limit) {
        if (limit <= 0 || limit > 200) {
            limit = 20;
        }
        List<AgentByUserRow> result = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = agentStatsMapper.byUser(appCode, limit);
            for (Map<String, Object> row : rows) {
                AgentByUserRow r = new AgentByUserRow();
                r.setUserId(asString(row.get("user_id")));
                r.setUserName(asString(row.get("user_name")));
                r.setTotalCalls(asLong(row.get("total_calls")));
                r.setSuccessCount(asLong(row.get("success_count")));
                r.setFailedCount(asLong(row.get("failed_count")));
                r.setTotalTokens(asLong(row.get("total_tokens")));
                r.setAvgLatencyMs(asDouble(row.get("avg_latency_ms")));
                r.setLastCallAt(asString(row.get("last_call_at")));
                result.add(r);
            }
        } catch (Exception e) {
            log.warn("[AgentStats] getByUser 失败 appCode={}, limit={}: {}", appCode, limit, e.getMessage());
        }
        return result;
    }
}
