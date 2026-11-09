package com.zifang.z.agent.center.core.agent.memory.service.impl;

import com.zifang.z.agent.center.core.agent.memory.entity.AgentMemory;
import com.zifang.z.agent.center.core.agent.memory.mapper.AgentMemoryMapper;
import com.zifang.z.agent.center.core.agent.memory.service.AgentMemoryService;
import com.zifang.z.cognition.memory.MemoryEntry;
import com.zifang.z.cognition.memory.MemorySearchQuery;
import com.zifang.z.cognition.memory.MemoryWrite;
import com.zifang.z.cognition.memory.client.MemoryClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FEATURE013 T2.3: Agent 长期记忆 Service。
 *
 * <p>2026-11-08 起双轨（认知舱迁移的第一步，先立后破）：
 * <ul>
 *   <li>{@code z.agent.memory.mode=local}（默认）—— 走本地 {@code z_agent_memory} 表，行为与迁移前一致</li>
 *   <li>{@code z.agent.memory.mode=remote} —— 走认知舱 {@code z-cognition-memory-server}，
 *       本地表不再读写；{@code z.agent.memory.base-url} 指向认知舱（含 context-path），
 *       {@code z.agent.memory.token} 供写操作鉴权</li>
 * </ul>
 * 对外接口不变，调用方（Controller / AgentRuntimeService）零改动。
 * 切换步骤：① 起 memory-server；② 配 base-url/token 并置 mode=remote；③ 迁移旧表数据；④ 删本地实现。
 */
@Service
public class AgentMemoryServiceImpl implements AgentMemoryService {

    private static final Logger log = LogManager.getLogger(AgentMemoryServiceImpl.class);

    private final AgentMemoryMapper memoryMapper;

    /** 认知舱记忆客户端；未装配（无 base-url）时可为 null */
    private final MemoryClient memoryClient;

    private final boolean remoteMode;

    private final String token;

    public AgentMemoryServiceImpl(AgentMemoryMapper memoryMapper,
                                  ObjectProvider<MemoryClient> memoryClientProvider,
                                  @Value("${z.agent.memory.mode:local}") String mode,
                                  @Value("${z.agent.memory.token:}") String token) {
        this.memoryMapper = memoryMapper;
        this.memoryClient = memoryClientProvider.getIfAvailable();
        this.remoteMode = "remote".equalsIgnoreCase(mode == null ? "" : mode.trim());
        this.token = token == null ? "" : token;
        if (remoteMode && memoryClient == null) {
            // 【假绿防护】remote 模式下缺 MemoryClient 是**配置错误**，不是可降级故障。
            //   历史行为：打一行 warn 后静默回退 local 表 —— 服务照常启动、接口照常返回 200，
            //   但记忆数据**全写进了本地 MySQL 表**，认知舱里的记忆永远是空的。
            //   运维看到「服务健康」却查不到记忆，就是被这行 warn 骗过去的。
            //   修法：remote 模式缺 client ⇒ 直接启动失败（fail-fast），
            //   让配置问题在发布那一刻就暴露，而不是在业务用的时候才暴露。
            throw new IllegalStateException(
                    "[AgentMemory] z.agent.memory.mode=remote 但容器里没有 MemoryClient bean。"
                            + "检查两件事：① z.cognition.memory.base-url 是否已配置"
                            + "（配了才会装配 client）；② classpath 上是否有 z-memory-client-spring-boot-starter"
                            + "（只有 z-memory-client 纯库件不会自动装配）。"
                            + "修不好请显式改回 z.agent.memory.mode=local。");
        }
        if (remoteMode) {
            log.info("[AgentMemory] 使用认知舱 memory 服务（remote 模式）");
        } else {
            log.info("[AgentMemory] 使用本地记忆表（local 模式）");
        }
    }

    private boolean useRemote() {
        return remoteMode && memoryClient != null;
    }

    private static String nullToStr(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static LocalDateTime stringToTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 认知舱模型 → 本地实体（对外 API 形状不变的关键适配）。 */
    private static AgentMemory toAgentMemory(MemoryEntry e) {
        if (e == null) {
            return null;
        }
        AgentMemory m = new AgentMemory();
        m.setId(e.getId());
        m.setUserId(e.getUserId());
        m.setAppCode(e.getAppCode());
        m.setMemoryType(e.getMemoryType());
        m.setContent(e.getContent());
        m.setEmbedding(e.getEmbedding());
        m.setKeywords(e.getKeywords());
        m.setImportance(e.getImportance());
        m.setDecayScore(e.getDecayScore());
        m.setAccessCount(e.getAccessCount());
        m.setSource(e.getSource());
        m.setTenantCode(e.getTenantCode());
        m.setLastAccessedAt(stringToTime(e.getLastAccessedAt()));
        m.setGmtCreate(stringToTime(e.getGmtCreate()));
        m.setGmtModified(stringToTime(e.getGmtModified()));
        m.setIsDeleted(e.getIsDeleted());
        return m;
    }

    private static List<AgentMemory> toAgentMemories(List<MemoryEntry> entries) {
        if (entries == null) {
            return Collections.emptyList();
        }
        return entries.stream().map(AgentMemoryServiceImpl::toAgentMemory).collect(Collectors.toList());
    }

    @Override
    public AgentMemory write(String userId, String appCode, String memoryType,
                             String content, Float importance, String keywords) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId 必填");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("content 必填");
        }
        String type = (memoryType == null || memoryType.trim().isEmpty()) ? "FACT" : memoryType.toUpperCase();
        if (!"FACT".equals(type) && !"PREFERENCE".equals(type) && !"EVENT".equals(type)) {
            throw new IllegalArgumentException("memoryType 必须是 FACT/PREFERENCE/EVENT");
        }
        float imp = importance == null ? 0.5f : Math.max(0f, Math.min(1f, importance));

        if (useRemote()) {
            MemoryWrite w = new MemoryWrite();
            w.setUserId(userId);
            w.setAppCode(emptyToNull(appCode));
            w.setMemoryType(type);
            w.setContent(content);
            w.setKeywords(emptyToNull(keywords));
            w.setImportance(imp);
            w.setSource("MANUAL");
            AgentMemory out = toAgentMemory(memoryClient.write(token, w));
            log.info("[AgentMemory][remote] write id={} user={} app={} type={}",
                    out == null ? null : out.getId(), userId, appCode, type);
            return out;
        }

        AgentMemory m = new AgentMemory();
        m.setUserId(userId);
        m.setAppCode(emptyToNull(appCode));
        m.setMemoryType(type);
        m.setContent(content);
        m.setImportance(imp);
        m.setDecayScore(1.0f);
        m.setAccessCount(0);
        m.setKeywords(emptyToNull(keywords));
        m.setSource("MANUAL");
        m.setIsDeleted(0);
        m.setGmtCreate(LocalDateTime.now());
        m.setGmtModified(LocalDateTime.now());
        memoryMapper.insert(m);
        log.info("[AgentMemory][local] write id={} user={} app={} type={} len={}",
                m.getId(), userId, appCode, type, content.length());
        return m;
    }

    @Override
    public List<AgentMemory> searchRelevant(String userId, String appCode, String query, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }

        if (limit <= 0 || limit > 50) {
            limit = 5;
        }

        if (useRemote()) {
            try {
                return toAgentMemories(memoryClient.search(
                        new MemorySearchQuery(userId, appCode, query, limit)));
            } catch (Exception e) {
                log.warn("[AgentMemory][remote] searchRelevant 失败 user={} app={}: {}",
                        userId, appCode, e.getMessage());
                return Collections.emptyList();
            }
        }

        try {
            List<AgentMemory> hits = memoryMapper.searchRelevant(userId, appCode, query, limit);
            // 命中后异步刷新 (best-effort)
            for (AgentMemory m : hits) {
                try {
                    memoryMapper.touchOnHit(m.getId());
                } catch (Exception ignored) {
                }
            }
            return hits;
        } catch (Exception e) {
            log.warn("[AgentMemory] searchRelevant 失败 user={} app={}: {}", userId, appCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<AgentMemory> listByUser(String userId, String appCode, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }

        if (limit <= 0 || limit > 500) {
            limit = 50;
        }

        if (useRemote()) {
            try {
                return toAgentMemories(memoryClient.list(userId, appCode, limit));
            } catch (Exception e) {
                log.warn("[AgentMemory][remote] listByUser 失败 user={} app={}: {}",
                        userId, appCode, e.getMessage());
                return Collections.emptyList();
            }
        }

        return memoryMapper.listByUser(userId, appCode, limit);
    }

    @Override
    public int decay(float factor) {
        if (factor <= 0 || factor > 1) {
            throw new IllegalArgumentException("factor 必须在 (0, 1] 之间");
        }
        if (useRemote()) {
            try {
                int n = memoryClient.decay(factor);
                log.info("[AgentMemory][remote] decay factor={} affected={}", factor, n);
                return n;
            } catch (Exception e) {
                log.warn("[AgentMemory][remote] decay 失败: {}", e.getMessage());
                return 0;
            }
        }
        try {
            int n = memoryMapper.decayAll(factor);
            log.info("[AgentMemory][local] decay factor={} affected={}", factor, n);
            return n;
        } catch (Exception e) {
            log.warn("[AgentMemory] decay 失败: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean forget(Long id) {
        if (id == null) {
            return false;
        }
        if (useRemote()) {
            try {
                return memoryClient.forget(id);
            } catch (Exception e) {
                log.warn("[AgentMemory][remote] forget 失败 id={}: {}", id, e.getMessage());
                return false;
            }
        }
        try {
            return memoryMapper.softDelete(id) > 0;
        } catch (Exception e) {
            log.warn("[AgentMemory] forget 失败 id={}: {}", id, e.getMessage());
            return false;
        }
    }

    @Override
    public List<AgentMemory> searchByTier(String userId, String appCode, String query, int limit, String tier) {
        if (!useRemote() || tier == null || tier.isBlank()) {
            return searchRelevant(userId, appCode, query, limit);
        }
        try {
            MemorySearchQuery q = new MemorySearchQuery(userId, appCode, query, limit);
            q.setTier(tier);
            return toAgentMemories(memoryClient.search(q));
        } catch (Exception e) {
            log.warn("[AgentMemory][remote] searchByTier 失败 user={} app={} tier={}: {}",
                    userId, appCode, tier, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String injectForPrompt(String userId, String appCode, String query) {
        // 分层注入（Phase 3）：
        //   1) WORKING —— 本会话即时上下文，紧凑置顶（remote 下显式 tier=WORKING 检索；
        //      local 表无 tier 概念，回退普通检索，不会混入工作记忆）
        //   2) SHORT_TERM + LONG_TERM —— 跨会话混合召回（默认检索已排除 WORKING）
        List<AgentMemory> working = searchByTier(userId, appCode, query, 3, "WORKING");
        List<AgentMemory> longTerm = searchRelevant(userId, appCode, query, 5);

        StringBuilder sb = new StringBuilder();
        int total = 0;
        if (working != null && !working.isEmpty()) {
            sb.append("【工作记忆】本会话进行中的即时上下文:\n");
            for (AgentMemory m : working) {
                if (m.getContent() == null || m.getContent().trim().isEmpty()) {
                    continue;
                }
                sb.append("- ").append(m.getContent().trim()).append("\n");
                total++;
            }
        }
        if (longTerm != null && !longTerm.isEmpty()) {
            sb.append("【长期记忆】你之前与该用户交互时记住的跨会话信息:\n");
            for (AgentMemory m : longTerm) {
                if (m.getContent() == null || m.getContent().trim().isEmpty()) {
                    continue;
                }
                sb.append("- [").append(nullToStr(m.getMemoryType(), "FACT")).append("] ");
                sb.append(m.getContent().trim());
                if (m.getImportance() != null && m.getImportance() > 0.8f) {
                    sb.append(" (重要)");
                }
                sb.append("\n");
                total++;
            }
        }
        if (total == 0) {
            return "";
        }
        sb.append("请在回复时自然地利用这些信息，不要生硬地复述.\n");
        return sb.toString();
    }
}