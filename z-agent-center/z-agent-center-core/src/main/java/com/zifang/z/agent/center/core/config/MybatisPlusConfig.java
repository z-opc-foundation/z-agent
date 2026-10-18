package com.zifang.z.agent.center.core.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置占位（空壳）。
 *
 * <p>历史：原本想在这里注册 {@code PaginationInnerInterceptor}，但该插件绑定
 * mybatis-plus / jsqlparser，与 z-team 等业务模块版本不一致时启动抛 InstantiationError。
 * 根因修复: z-opc 根 pom 锁定 jsqlparser 4.9 与 mybatis-plus 3.5.7 编译版本一致。
 *
 * <p>业务模块统一在 {@code TeamSqlSessionFactoryConfig} 等位置自行装配
 * {@code MybatisPlusInterceptor}；此处提供 {@code @ConditionalOnMissingBean}
 * 的占位 bean，避免 z-agent-center-core 缺席时下游拿不到 bean 引用。
 */
@Configuration
@ConditionalOnMissingBean(MybatisPlusInterceptor.class)
public class MybatisPlusConfig {

    private static final Logger log = LogManager.getLogger(MybatisPlusConfig.class);

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("=== MybatisPlusConfig: 占位空壳,未注册 PaginationInnerInterceptor ===");
        // 业务模块应自行装配 MybatisPlusInterceptor,这里返回空拦截器避免下游 NPE。
        return new MybatisPlusInterceptor();
    }
}
