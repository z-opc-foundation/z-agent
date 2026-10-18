package com.zifang.z.agent.center.core.bot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.agent.center.core.bot.entity.BotMemoryEntry;
import com.zifang.z.agent.center.core.bot.mapper.BotMemoryMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * z-bot 长期记忆服务 — FEATURE014 Wishlist 3 (L2 反射层)
 *
 * <p>不替代 {@code z-agent-engine} 的 {@code Memory}（那是会话内 hot window）；
 * 本服务是会话外的 cold store，用于：</p>
 * <ul>
 *   <li>跨会话记住用户偏好 / 关键事实（append + recent）</li>
 *   <li>会话结束时把最近 K 条消息做摘要 / 写入冷存储（consolidate）</li>
 *   <li>新会话开始时把冷存储的摘要拉回作为 system context（recallForContext）</li>
 * </ul>
 *
 * <p><b>重要</b>：当前是文本 LIKE 检索，没有向量检索能力。等引入向量库后，
 * {@code recallForContext} 应改为 embedding 相似度检索。</p>
 */
@Service
public class BotMemoryService extends ServiceImpl<BotMemoryMapper, BotMemoryEntry> {

    private static final Logger log = LogManager.getLogger(BotMemoryService.class);

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }

        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /**
     * 追加一条记忆（来自 chat / feedback / consolidation / manual）。
     */
    public BotMemoryEntry append(String instanceCode, String userId, String role,
                                 String content, BigDecimal importance, String source, String tags) {
        BotMemoryEntry e = new BotMemoryEntry();
        e.setInstanceCode(instanceCode);
        e.setUserId(userId);
        e.setRole(role);
        e.setContent(content);
        e.setImportance(importance != null ? importance : new BigDecimal("0.50"));
        e.setSource(source != null ? source : "chat");
        e.setTags(tags);
        e.setTenantCode("default");
        e.setGmtCreate(LocalDateTime.now());
        e.setIsDeleted(0);
        this.save(e);
        log.debug("BotMemory appended: instance={} role={} importance={} src={}",
                instanceCode, role, e.getImportance(), e.getSource());
        return e;
    }

    /**
     * 拉取一个实例最近 N 条记忆（按时间倒序），过滤掉已过期的。
     */
    public List<BotMemoryEntry> recent(String instanceCode, String userId, int limit) {
        LambdaQueryWrapper<BotMemoryEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BotMemoryEntry::getInstanceCode, instanceCode);
        if (userId != null) {
            wrapper.eq(BotMemoryEntry::getUserId, userId);
        }
        wrapper.eq(BotMemoryEntry::getIsDeleted, 0);
        wrapper.and(w -> w.isNull(BotMemoryEntry::getGmtExpire)
                .or().gt(BotMemoryEntry::getGmtExpire, LocalDateTime.now()));
        wrapper.orderByDesc(BotMemoryEntry::getGmtCreate);
        wrapper.last("limit " + Math.max(1, Math.min(limit, 200)));
        return this.list(wrapper);
    }

    /**
     * 把多条历史消息汇总成一条 summary（write-then-delete 模式）。
     *
     * <p>操作：写入一条 {@code role=summary} 的高重要度记忆，然后批量软删除
     * 原始 N 条 chat 记忆（保留 audit 痕迹）。</p>
     *
     * @return 新写入的 summary entry
     */
    public BotMemoryEntry consolidate(String instanceCode, String userId, String summary,
                                      List<Long> sourceEntryIds, String tags) {
        if (summary == null || summary.trim().isEmpty()) {
            throw new IllegalArgumentException("summary must not be empty");
        }
        BotMemoryEntry summaryEntry = append(instanceCode, userId, "summary",
                summary, new BigDecimal("1.00"), "consolidation", tags);
        if (sourceEntryIds != null && !sourceEntryIds.isEmpty()) {
            LambdaQueryWrapper<BotMemoryEntry> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(BotMemoryEntry::getId, sourceEntryIds);
            BotMemoryEntry updater = new BotMemoryEntry();
            updater.setIsDeleted(1);
            this.update(updater, wrapper);
            log.info("Consolidated {} chat entries into 1 summary (id={})",
                    sourceEntryIds.size(), summaryEntry.getId());
        }
        return summaryEntry;
    }

    /**
     * 关键字检索一个实例的历史记忆。
     *
     * <p>生产环境应当用向量检索；这里是 LIKE 的 fallback。</p>
     */
    public List<BotMemoryEntry> search(String instanceCode, String userId, String query, int limit) {
        LambdaQueryWrapper<BotMemoryEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BotMemoryEntry::getInstanceCode, instanceCode);
        if (userId != null) {
            wrapper.eq(BotMemoryEntry::getUserId, userId);
        }
        wrapper.eq(BotMemoryEntry::getIsDeleted, 0);
        if (query != null && !query.trim().isEmpty()) {
            wrapper.like(BotMemoryEntry::getContent, query.trim());
        }
        wrapper.orderByDesc(BotMemoryEntry::getGmtCreate);
        wrapper.last("limit " + Math.max(1, Math.min(limit, 100)));
        return this.list(wrapper);
    }

    /**
     * 为下一次 chat 构造系统提示中的"长期记忆段落"。
     *
     * <p>把最近的若干条 summary / 高 importance 记忆压成一个 Markdown 段落，
     * 由 Agent 拼到 system prompt 前。</p>
     *
     * @return Markdown 字符串；如果没有任何记忆返回 null
     */
    public String recallForContext(String instanceCode, String userId) {
        List<BotMemoryEntry> recent = recent(instanceCode, userId, 20);
        if (recent.isEmpty()) {
            return null;
        }
        // 只用 summary + 高 importance（>= 0.7）的
        List<BotMemoryEntry> relevant = recent.stream()
                .filter(e -> "summary".equalsIgnoreCase(e.getRole())
                        || (e.getImportance() != null
                        && e.getImportance().compareTo(new BigDecimal("0.70")) >= 0))
                .limit(10)
                .collect(Collectors.toList());
        if (relevant.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 长期记忆（来自过去会话）\n\n");
        for (BotMemoryEntry e : relevant) {
            sb.append("- [").append(e.getRole()).append(" | ")
                    .append(e.getGmtCreate().toString().substring(0, 10)).append("] ");
            sb.append(truncate(e.getContent(), 200));
            if (e.getTags() != null && !e.getTags().isEmpty()) {
                sb.append("  \n  标签: `").append(e.getTags()).append("`");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 清理过期记忆（每天定时任务调用）。
     */
    public int purgeExpired() {
        LambdaQueryWrapper<BotMemoryEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(BotMemoryEntry::getGmtExpire, LocalDateTime.now());
        BotMemoryEntry updater = new BotMemoryEntry();
        updater.setIsDeleted(1);
        boolean ok = this.update(updater, wrapper);
        if (ok) {
            log.info("Purged expired memory entries (some)");
        }
        return ok ? 1 : 0;
    }
}
