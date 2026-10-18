package com.zifang.z.agent.center.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * z-bot 自我迭代 Mapper 扫描 — FEATURE014 Wishlist 3-6
 *
 * <p>4 张表（z_bot_memory / z_bot_feedback / z_bot_hook_event / z_bot_skill_promotion）
 * 的 MyBatis-Plus mapper 统一注册。</p>
 */
@Configuration
@MapperScan(
        basePackages = {
                "com.zifang.z.agent.center.core.bot.mapper"
        },
        sqlSessionFactoryRef = "sqlSessionFactoryConfig"
)
public class BotMapperConfig {
}
