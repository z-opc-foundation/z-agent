package com.zifang.z.agent.center.core.agent.stats.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FEATURE013 T3.2: Agent 监控面板专用 SQL 聚合 Mapper.
 *
 * <p>所有方法返回 {@code List<Map<String, Object>>}, 由 Service 端把每行
 * 映射到 DTO. 这种 raw map 风格避免在 XML 里写 resultMap, 兼顾表达力与可读性.
 *
 * <p>列别名约定:
 * <ul>
 *     <li>所有聚合列取稳定别名 (cnt / sum_tokens / avg_latency / ...)</li>
 *     <li>日期列别名为 d (yyyy-MM-dd 字符串)</li>
 * </ul>
 */
@Mapper
public interface AgentStatsMapper {

    // ============ 概览 (overview) ============

    @Select("SELECT " +
            "  COUNT(*)                                    AS total_conversations, " +
            "  SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count, " +
            "  SUM(CASE WHEN status = 'FAILED'  THEN 1 ELSE 0 END) AS failed_count, " +
            "  COALESCE(SUM(token_count), 0)              AS total_tokens, " +
            "  COALESCE(AVG(token_count), 0)              AS avg_tokens, " +
            "  COALESCE(AVG(latency_ms), 0)               AS avg_latency_ms, " +
            "  COUNT(DISTINCT instance_code)              AS active_app_count, " +
            "  COUNT(DISTINCT user_id)                    AS active_user_count " +
            "FROM z_agent_conversation " +
            "WHERE gmt_create >= #{since} " +
            "  AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "       OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode}))")
    Map<String, Object> overviewTotal(@Param("appCode") String appCode,
                                      @Param("since") LocalDateTime since);

    @Select("SELECT " +
            "  COUNT(*)                                    AS today_conversations, " +
            "  COALESCE(SUM(token_count), 0)              AS today_tokens, " +
            "  SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS today_failed_count " +
            "FROM z_agent_conversation " +
            "WHERE gmt_create >= #{todayStart} " +
            "  AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "       OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode}))")
    Map<String, Object> overviewToday(@Param("appCode") String appCode,
                                      @Param("todayStart") LocalDateTime todayStart);

    /**
     * P95 延迟：取 latency_ms 升序第 95% 位置（剔除 NULL）。
     * 用子查询代替 PERCENTILE_CONT（MySQL 8.0 不支持）。
     */
    @Select("SELECT MAX(latency_ms) AS p95_latency_ms FROM ( " +
            "  SELECT latency_ms FROM z_agent_conversation " +
            "  WHERE latency_ms IS NOT NULL " +
            "    AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "         OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})) " +
            "  ORDER BY latency_ms ASC " +
            "  LIMIT 1 OFFSET (SELECT GREATEST(CAST(COUNT(*) * 0.95 AS UNSIGNED) - 1, 0) " +
            "                   FROM z_agent_conversation " +
            "                   WHERE latency_ms IS NOT NULL " +
            "                     AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "                          OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})))) " +
            ") t")
    Long p95LatencyMs(@Param("appCode") String appCode);

    // ============ 趋势 (trend) ============

    /**
     * 近 N 天每日聚合（MySQL DATE_FORMAT）。
     * 返回字段：d (yyyy-MM-dd), cnt, sum_tokens, success_cnt, failed_cnt, avg_latency
     */
    @Select("SELECT " +
            "  DATE_FORMAT(gmt_create, '%Y-%m-%d')            AS d, " +
            "  COUNT(*)                                        AS cnt, " +
            "  COALESCE(SUM(token_count), 0)                   AS sum_tokens, " +
            "  SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_cnt, " +
            "  SUM(CASE WHEN status = 'FAILED'  THEN 1 ELSE 0 END) AS failed_cnt, " +
            "  COALESCE(AVG(latency_ms), 0)                    AS avg_latency " +
            "FROM z_agent_conversation " +
            "WHERE gmt_create >= #{since} " +
            "  AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "       OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})) " +
            "GROUP BY DATE_FORMAT(gmt_create, '%Y-%m-%d') " +
            "ORDER BY d ASC")
    List<Map<String, Object>> trend(@Param("appCode") String appCode,
                                    @Param("since") LocalDateTime since);

    // ============ 延迟分布 (latency distribution) ============

    /**
     * 6 个固定分桶：&lt;500 / 500-1k / 1k-2k / 2k-5k / 5k-10k / &gt;=10k ms
     * 每桶返回：lower_ms, upper_ms, cnt, avg_latency
     */
    @Select("SELECT bucket, lower_ms, upper_ms, cnt, avg_latency FROM ( " +
            "  SELECT '<500ms'       AS bucket, 0          AS lower_ms, 500        AS upper_ms, " +
            "         SUM(CASE WHEN latency_ms >= 0    AND latency_ms < 500   THEN 1 ELSE 0 END) AS cnt, " +
            "         AVG(CASE WHEN latency_ms >= 0    AND latency_ms < 500   THEN latency_ms END) AS avg_latency " +
            "  FROM z_agent_conversation WHERE latency_ms IS NOT NULL " +
            "    AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "         OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})) " +
            "  UNION ALL " +
            "  SELECT '500-1000ms'   AS bucket, 500        AS lower_ms, 1000       AS upper_ms, " +
            "         SUM(CASE WHEN latency_ms >= 500  AND latency_ms < 1000  THEN 1 ELSE 0 END) AS cnt, " +
            "         AVG(CASE WHEN latency_ms >= 500  AND latency_ms < 1000  THEN latency_ms END) AS avg_latency " +
            "  FROM z_agent_conversation WHERE latency_ms IS NOT NULL " +
            "    AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "         OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})) " +
            "  UNION ALL " +
            "  SELECT '1000-2000ms'  AS bucket, 1000       AS lower_ms, 2000       AS upper_ms, " +
            "         SUM(CASE WHEN latency_ms >= 1000 AND latency_ms < 2000  THEN 1 ELSE 0 END) AS cnt, " +
            "         AVG(CASE WHEN latency_ms >= 1000 AND latency_ms < 2000  THEN latency_ms END) AS avg_latency " +
            "  FROM z_agent_conversation WHERE latency_ms IS NOT NULL " +
            "    AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "         OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})) " +
            "  UNION ALL " +
            "  SELECT '2000-5000ms'  AS bucket, 2000       AS lower_ms, 5000       AS upper_ms, " +
            "         SUM(CASE WHEN latency_ms >= 2000 AND latency_ms < 5000  THEN 1 ELSE 0 END) AS cnt, " +
            "         AVG(CASE WHEN latency_ms >= 2000 AND latency_ms < 5000  THEN latency_ms END) AS avg_latency " +
            "  FROM z_agent_conversation WHERE latency_ms IS NOT NULL " +
            "    AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "         OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})) " +
            "  UNION ALL " +
            "  SELECT '5000-10000ms' AS bucket, 5000       AS lower_ms, 10000      AS upper_ms, " +
            "         SUM(CASE WHEN latency_ms >= 5000 AND latency_ms < 10000 THEN 1 ELSE 0 END) AS cnt, " +
            "         AVG(CASE WHEN latency_ms >= 5000 AND latency_ms < 10000 THEN latency_ms END) AS avg_latency " +
            "  FROM z_agent_conversation WHERE latency_ms IS NOT NULL " +
            "    AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "         OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})) " +
            "  UNION ALL " +
            "  SELECT '>=10000ms'    AS bucket, 10000      AS lower_ms, 2147483647 AS upper_ms, " +
            "         SUM(CASE WHEN latency_ms >= 10000                       THEN 1 ELSE 0 END) AS cnt, " +
            "         AVG(CASE WHEN latency_ms >= 10000                       THEN latency_ms END) AS avg_latency " +
            "  FROM z_agent_conversation WHERE latency_ms IS NOT NULL " +
            "    AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "         OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})) " +
            ") t " +
            "ORDER BY lower_ms ASC")
    List<Map<String, Object>> latencyDistribution(@Param("appCode") String appCode);

    // ============ 按 Agent 应用聚合 (by app) ============

    /**
     * 按 instance_code 聚合，再 JOIN z_agent_instance 拿 app_code/app_name。
     */
    @Select("SELECT " +
            "  ai.app_code        AS app_code, " +
            "  ai.app_code        AS app_name, " +
            "  COUNT(*)           AS total_calls, " +
            "  SUM(CASE WHEN c.status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count, " +
            "  SUM(CASE WHEN c.status = 'FAILED'  THEN 1 ELSE 0 END) AS failed_count, " +
            "  COALESCE(SUM(c.token_count), 0) AS total_tokens, " +
            "  COALESCE(AVG(c.token_count), 0) AS avg_tokens, " +
            "  COALESCE(AVG(c.latency_ms), 0)  AS avg_latency_ms " +
            "FROM z_agent_conversation c " +
            "JOIN z_agent_instance ai ON ai.instance_code = c.instance_code " +
            "WHERE (#{appCode} IS NULL OR #{appCode} = '' OR ai.app_code = #{appCode}) " +
            "GROUP BY ai.app_code " +
            "ORDER BY total_calls DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> byApp(@Param("appCode") String appCode,
                                    @Param("limit") int limit);

    // ============ 按用户聚合 (by user) ============

    @Select("SELECT " +
            "  user_id                                          AS user_id, " +
            "  MAX(user_name)                                   AS user_name, " +
            "  COUNT(*)                                         AS total_calls, " +
            "  SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count, " +
            "  SUM(CASE WHEN status = 'FAILED'  THEN 1 ELSE 0 END) AS failed_count, " +
            "  COALESCE(SUM(token_count), 0)                   AS total_tokens, " +
            "  COALESCE(AVG(latency_ms), 0)                    AS avg_latency_ms, " +
            "  DATE_FORMAT(MAX(gmt_create), '%Y-%m-%d %H:%i:%s') AS last_call_at " +
            "FROM z_agent_conversation " +
            "WHERE user_id IS NOT NULL AND user_id != '' " +
            "  AND (#{appCode} IS NULL OR #{appCode} = '' " +
            "       OR instance_code IN (SELECT instance_code FROM z_agent_instance WHERE app_code = #{appCode})) " +
            "GROUP BY user_id " +
            "ORDER BY total_calls DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> byUser(@Param("appCode") String appCode,
                                     @Param("limit") int limit);
}
