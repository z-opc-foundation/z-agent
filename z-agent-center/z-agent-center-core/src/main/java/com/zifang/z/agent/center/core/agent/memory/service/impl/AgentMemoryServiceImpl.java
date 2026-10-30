package com.zifang.z.agent.center.core.agent.memory.service.impl;

import com.zifang.z.agent.center.core.agent.memory.entity.AgentMemory;
import com.zifang.z.agent.center.core.agent.memory.mapper.AgentMemoryMapper;
import com.zifang.z.agent.center.core.agent.memory.service.AgentMemoryService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentMemoryServiceImpl implements AgentMemoryService {

    private static final Logger log = LogManager.getLogger(AgentMemoryServiceImpl.class);

    @Resource
    private AgentMemoryMapper memoryMapper;

    private static String nullToStr(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
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
        log.info("[AgentMemory] write id={} user={} app={} type={} len={}",
                m.getId(), userId, appCode, type, content.length());
        return m;
    }

    @Override
    public List<AgentMemory> searchRelevant(String userId, String appCode, String query, int limit) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }

        if (limit <= 0 || limit > 50) {
            limit = 5;
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
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public List<AgentMemory> listByUser(String userId, String appCode, int limit) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }

        if (limit <= 0 || limit > 500) {
            limit = 50;
        }

        return memoryMapper.listByUser(userId, appCode, limit);
    }

    @Override
    public int decay(float factor) {
        if (factor <= 0 || factor > 1) {
            throw new IllegalArgumentException("factor 必须在 (0, 1] 之间");
        }
        try {
            int n = memoryMapper.decayAll(factor);
            log.info("[AgentMemory] decay factor={} affected={}", factor, n);
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

        try {
            return memoryMapper.softDelete(id) > 0;
        } catch (Exception e) {
            log.warn("[AgentMemory] forget 失败 id={}: {}", id, e.getMessage());
            return false;
        }
    }

    @Override
    public String injectForPrompt(String userId, String appCode, String query) {
        List<AgentMemory> hits = searchRelevant(userId, appCode, query, 5);
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【长期记忆】你之前与该用户交互时记住了以下信息:\n");
        for (AgentMemory m : hits) {
            sb.append("- [").append(nullToStr(m.getMemoryType(), "FACT")).append("] ");
            sb.append(m.getContent().trim());
            if (m.getImportance() != null && m.getImportance() > 0.8f) {
                sb.append(" (重要)");
            }
            sb.append("\n");
        }
        sb.append("请在回复时自然地利用这些信息, 不要生硬地复述.\n");
        return sb.toString();
    }
}
