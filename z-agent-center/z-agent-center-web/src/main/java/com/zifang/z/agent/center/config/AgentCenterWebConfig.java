package com.zifang.z.agent.center.config;

import com.zifang.z.agent.center.core.skill.auth.AgentAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Agent Center Web 配置 — FEATURE014
 *
 * <p>注册 Agent 鉴权拦截器，保护 {@code /api/agent/skill/*}（Agent 侧调用方）。</p>
 */
@Configuration
public class AgentCenterWebConfig implements WebMvcConfigurer {

    @Autowired
    private AgentAuthInterceptor agentAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(agentAuthInterceptor)
                .addPathPatterns("/api/agent/skill/**");
    }
}
