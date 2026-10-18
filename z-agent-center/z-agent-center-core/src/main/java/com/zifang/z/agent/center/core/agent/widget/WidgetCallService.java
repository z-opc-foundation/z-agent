package com.zifang.z.agent.center.core.agent.widget;

import com.zifang.z.agent.center.core.agent.conversation.entity.AgentConversation;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.mapper.AgentInstanceMapper;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.center.core.agent.runtime.service.AgentRuntimeService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FEATURE013 T2.2: Agent Widget 嵌入式调用 Service
 *
 * <p>职责:
 * <ul>
 *     <li>authByApiKey — 验证 widget API Key + 启用状态 + CORS 来源</li>
 *     <li>checkRateLimit — 滑窗式内存计数 (按 instanceCode + apiKey 限流)</li>
 *     <li>callAgent — 委托给 AgentRuntimeService 执行</li>
 * </ul>
 *
 * <p>安全考虑:
 * <ul>
 *     <li>API Key 缺失 / 禁用 / CORS 不匹配 → 拒绝</li>
 *     <li>每分钟请求数 > widget_rate_limit → 拒绝</li>
 *     <li>失败降级为 warn log, 不抛 5xx</li>
 * </ul>
 */
@Service
public class WidgetCallService {

    private static final Logger log = LogManager.getLogger(WidgetCallService.class);
    /**
     * 简单内存限流: apiKey → [当前分钟, 计数]
     */
    private final Map<String, AtomicBucket> rateBuckets = new ConcurrentHashMap<>();
    @Resource
    private AgentInstanceService instanceService;
    @Resource
    private AgentInstanceMapper agentInstanceMapper;
    @Resource
    private AgentRuntimeService agentRuntimeService;

    /**
     * 验证 API Key 并返回 Agent 实例. 失败抛 IllegalArgumentException.
     */
    public AgentInstance authenticate(String apiKey, String origin) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key 缺失");
        }
        AgentInstance instance = instanceService.getByWidgetApiKey(apiKey.trim());
        if (instance == null) {
            throw new IllegalArgumentException("API Key 无效");
        }
        if (!"ACTIVE".equals(instance.getStatus())) {
            throw new IllegalArgumentException("实例已停用: " + instance.getInstanceCode());
        }
        if (instance.getWidgetEnabled() == null || instance.getWidgetEnabled() != 1) {
            throw new IllegalArgumentException("Widget 未启用");
        }
        // CORS 校验
        String cors = instance.getWidgetCors();
        if (cors != null && !cors.trim().isEmpty() && !"*".equals(cors.trim())) {
            if (origin == null) {
                throw new IllegalArgumentException("Origin 缺失, CORS 拒绝");
            }
            boolean allowed = false;
            for (String o : cors.split(",")) {
                if (o.trim().equalsIgnoreCase(origin.trim())) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new IllegalArgumentException("Origin 不在 CORS 白名单: " + origin);
            }
        }
        return instance;
    }

    /**
     * 检查限流. true=允许, false=超限.
     */
    public boolean checkRateLimit(AgentInstance instance) {
        Integer limit = instance.getWidgetRateLimit();
        if (limit == null || limit == 0) return true; // 0=不限
        String key = instance.getInstanceCode();
        long currentMinute = System.currentTimeMillis() / 60_000;
        AtomicBucket bucket = rateBuckets.computeIfAbsent(key, k -> new AtomicBucket());
        synchronized (bucket) {
            if (bucket.minute != currentMinute) {
                bucket.minute = currentMinute;
                bucket.count.set(0);
            }
            return bucket.count.incrementAndGet() <= limit;
        }
    }

    /**
     * Widget 调用入口. 返回 Assistant 文本.
     */
    public String invoke(AgentInstance instance, String message, String userId, String userName) {
        String uid = (userId == null || userId.isEmpty()) ? "widget_" + instance.getInstanceCode() : userId;
        String uname = (userName == null || userName.isEmpty()) ? "Widget User" : userName;
        long start = System.currentTimeMillis();
        try {
            AgentConversation result = agentRuntimeService.chat(
                    instance.getInstanceCode(), message, uid, uname);
            long elapsed = System.currentTimeMillis() - start;
            // 累计 widget 调用数
            try {
                Long c = instance.getWidgetEmbedCount();
                instance.setWidgetEmbedCount(c == null ? 1L : c + 1);
                instance.setLastVisitTime(LocalDateTime.now());
                agentInstanceMapper.updateById(instance);
            } catch (Exception ignored) {
            }
            if (result == null) {
                return "(空响应)";
            }

            if (!"SUCCESS".equals(result.getStatus())) {
                return "[错误] " + (result.getErrorMsg() != null ? result.getErrorMsg() : result.getStatus());
            }
            log.info("[Widget] invoke OK instance={} user={} elapsed={}ms",
                    instance.getInstanceCode(), uid, elapsed);
            return result.getAssistantMessage();
        } catch (Exception e) {
            log.warn("[Widget] invoke FAIL instance={}: {}", instance.getInstanceCode(), e.getMessage());
            return "[错误] " + e.getMessage();
        }
    }

    private static class AtomicBucket {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long minute;
    }
}
