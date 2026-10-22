package com.zifang.z.agent.center.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * z-agent-center 独立部署启动器。
 *
 * <p><b>2026-10-22 修：@MapperScan 从整包收窄到精确 mapper 包。</b>
 *
 * <p>原写法是 {@code @MapperScan(basePackages = {"com.zifang.z.agent.center"})}，
 * 而 {@code @MapperScan} <b>会把扫描范围内的每一个接口</b>都注册成 MyBatis mapper bean，
 * <b>不要求接口带 @Mapper 注解</b>。{@code com.zifang.z.agent.center} 这棵包树里除了 mapper
 * 之外还有 service 接口（如 {@code core.app.service.AgentAkService}），于是它被注册成一个
 * 名为 {@code agentAkService} 的 bean；容器里同时存在
 * {@code agentAkServiceImpl}（@Service 实现类）与 {@code agentAkService}（误注册的 mapper 代理），
 * 按类型注入即炸：
 * <pre>
 *   NoUniqueBeanDefinitionException: No qualifying bean of type
 *     'com.zifang.z.agent.center.core.app.service.AgentAkService' available:
 *     expected single matching bean but found 2: agentAkServiceImpl,agentAkService
 * </pre>
 *
 * <p>为什么主壳 z-opc 里没炸：主壳<b>不走本类</b>（主壳依赖的是 z-agent-center-core /
 * -web，不依赖 center-starter），它的 mapper 由 z-team-starter 的
 * {@code TeamSqlSessionFactoryConfig} 用<b>精确到 {@code *.mapper} 包</b>的 @MapperScan 注册，
 * 并且 z-team 显式排除了 z-agent-center-web 的 spring.factories 自动装配。
 * ⇒ 本类必须自己收窄，否则独立部署永远起不来。
 *
 * <p>下面的包清单与 {@code AgentCenterMapperConfig} 的清单一致（两处都留着：
 * 即使 spring.factories 自动装配被排除，本类也能保证 mapper 一定被注册；
 * 同一 mapper 类被扫两次时 Spring 按 beanClass 兼容性跳过重复注册，不会冲突）。
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.zifang.z.agent.center",
        // FEATURE013 T3/T4: 让 z-agent-engine 的 MysqlAgentTracer / MysqlAgentMetrics /
        // AgentTraceController 被主启动器扫描到
        "com.zifang.z.agent.engine"
})
@MapperScan(basePackages = {
        "com.zifang.z.agent.center.core.agent.conversation.mapper",
        "com.zifang.z.agent.center.core.agent.flow.mapper",
        "com.zifang.z.agent.center.core.agent.humaninput.mapper",
        "com.zifang.z.agent.center.core.agent.instance.mapper",
        "com.zifang.z.agent.center.core.agent.memory.mapper",
        "com.zifang.z.agent.center.core.agent.plugin.mapper",
        "com.zifang.z.agent.center.core.agent.share.mapper",
        "com.zifang.z.agent.center.core.agent.stats.mapper",
        "com.zifang.z.agent.center.core.agent.template.mapper",
        "com.zifang.z.agent.center.core.agent.textgen.mapper",
        "com.zifang.z.agent.center.core.agent.trigger.mapper",
        "com.zifang.z.agent.center.core.app.mapper",
        "com.zifang.z.agent.center.core.bot.mapper",
        "com.zifang.z.agent.center.core.skill.mapper",
        "com.zifang.z.agent.center.core.workflow.mapper"
})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
