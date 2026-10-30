package com.zifang.z.agent.center.core.skill.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.mapper.AgentInstanceMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Agent 回调鉴权拦截器 — FEATURE014 Wishlist 1
 *
 * <p>保护 {@code /api/agent/skill/*}（Agent 侧调用方）的端点：</p>
 *
 * <ul>
 *   <li>如果请求不含 {@code Authorization: Bearer ...} 头：放行（由 JWT 鉴权链处理，
 *       这是 admin 控制台的常见调用方式）</li>
 *   <li>如果含 Bearer token：从 {@code z_agent_instance.auth_token} 中查找匹配项，
 *       找不到则 401 拒绝；找到则把 instance 放进 request attribute 供下游 Controller 使用</li>
 * </ul>
 *
 * <p><b>安全注记</b>：当前 {@code auth_token} 是明文存储。生产环境强烈建议升级为 SHA-256(token)；
 * 升级路径已留好（{@link #findInstance} 的两个查询分支），等所有客户端都升级后再切换。</p>
 */
@Component
public class AgentAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LogManager.getLogger(AgentAuthInterceptor.class);

    @Autowired
    private AgentInstanceMapper instanceMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // 只保护 /api/agent/skill/* 这一族
        if (!path.startsWith("/api/agent/skill")) {
            return true;
        }
        // 控制台调用的端点（admin 已经过 JWT 鉴权）放行
        if (path.endsWith("/bind") || path.endsWith("/unbind")
                || path.endsWith("/bindings") || path.endsWith("/sync-logs")
                || path.endsWith("/register")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 没带 token：当作 admin 调用放行（admin 用的是 JWT，前面已拦过）
            return true;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            writeError(response, 401, "Empty bearer token");
            return false;
        }

        AgentInstance instance = findInstance(token);
        if (instance == null) {
            log.warn("Agent auth failed for path {}: token prefix {}",
                    path, token.length() > 8 ? token.substring(0, 8) : "***");
            writeError(response, 401, "Invalid agent token");
            return false;
        }
        if (!"ACTIVE".equalsIgnoreCase(instance.getStatus())) {
            writeError(response, 403, "Instance is not ACTIVE: status=" + instance.getStatus());
            return false;
        }

        // 把 instance 放进 request attribute 给 controller 使用
        request.setAttribute("agentInstance", instance);
        return true;
    }

    /**
     * 通过 token 查找 instance。当前按明文匹配；未来可平滑升级为 SHA-256 哈希匹配。
     */
    private AgentInstance findInstance(String token) {
        LambdaQueryWrapper<AgentInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentInstance::getAuthToken, token)
                .eq(AgentInstance::getIsDeleted, 0)
                .last("limit 1");
        return instanceMapper.selectOne(wrapper);
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        response.getWriter().write(
                "{\"code\":" + code + ",\"msg\":\"" + message + "\",\"success\":false}");
    }
}
