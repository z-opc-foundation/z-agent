package com.zifang.z.agent.center.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Agent Center 自动配置入口
 * 扫描 web 包下 controller + mapper 注册
 */
@Configuration
@ComponentScan(basePackages = "com.zifang.z.agent.center")
@MapperScan(
        basePackages = {
                "com.zifang.z.agent.center.core.mapper",
                "com.zifang.z.agent.center.core.app.mapper",
                "com.zifang.z.agent.center.core.agent.instance.mapper",
                "com.zifang.z.agent.center.core.agent.conversation.mapper",
                "com.zifang.z.agent.center.core.agent.share.mapper",
                "com.zifang.z.agent.center.core.workflow.mapper",
                // FEATURE013 A2: Flow 定义 + 执行 mapper
                "com.zifang.z.agent.center.core.agent.flow.mapper",
                // FEATURE013 T2: Agent 追踪/span/metric mapper (z-agent-engine 子模块)
                "com.zifang.z.agent.observe.persistence",
                "com.zifang.z.agent.conversation.persistence.mapper",
                "com.zifang.z.agent.engine.agent.instance.mapper",
                // FEATURE013 T3.2: Agent 监控面板 stats mapper
                "com.zifang.z.agent.center.core.agent.stats.mapper",
                // FEATURE013 T2.1: Agent Plugin mapper (可执行插件)
                "com.zifang.z.agent.center.core.agent.plugin.mapper",
                // FEATURE013 T2.3: Agent Memory mapper (长期记忆)
                "com.zifang.z.agent.center.core.agent.memory.mapper",
                // FEATURE013 T3.1: Agent Trigger mapper (定时触发)
                "com.zifang.z.agent.center.core.agent.trigger.mapper",
                // FEATURE013 T3.3: Agent Template mapper (模板市场)
                "com.zifang.z.agent.center.core.agent.template.mapper",
                // FEATURE013 T3.4: Agent TextGen mapper (文本生成器)
                "com.zifang.z.agent.center.core.agent.textgen.mapper",
                // FEATURE013 T3.5: Agent Human-in-the-Loop mapper (人工审批/输入)
                "com.zifang.z.agent.center.core.agent.humaninput.mapper",
                // FEATURE014: Skill 绑定 + 同步日志 mapper (FEATURE015 Bug Fix)
                "com.zifang.z.agent.center.core.skill.mapper"
        },
        sqlSessionFactoryRef = "sqlSessionFactoryConfig"
)
public class AgentCenterMapperConfig {
}
