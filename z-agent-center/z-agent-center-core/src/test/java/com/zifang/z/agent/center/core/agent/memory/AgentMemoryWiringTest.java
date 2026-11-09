package com.zifang.z.agent.center.core.agent.memory;

import com.zifang.z.agent.center.core.agent.memory.service.impl.AgentMemoryServiceImpl;
import com.zifang.z.cognition.memory.client.MemoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 记忆层接线回归测试 —— **防假绿**。
 *
 * <p>要 pin 住的两个根因（都是「服务健康但功能没生效」的形态）：</p>
 *
 * <ol>
 *   <li><b>装配层缺失</b>：z-agent 只引 {@code z-memory-client}（纯 JDK 库件，**不带**
 *       {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}）
 *       ⇒ 容器里永远没有 {@link MemoryClient} bean ⇒ remote 模式静默回退本地表，
 *       记忆全写进本地 MySQL，认知舱里永远是空的。</li>
 *   <li><b>降级掩盖故障</b>：remote 模式缺 client 时只打一行 warn 就回退 local，
 *       服务照常启动、接口照常 200 ⇒ 配置问题要到业务用的时候才暴露。</li>
 * </ol>
 *
 * <p>本测试不依赖 Spring 上下文（不起容器）：第 1 条直接断言 classpath 上存在自动装配注册文件，
 * 第 2 条直接断言 remote 模式缺 client 会 fail-fast。</p>
 */
class AgentMemoryWiringTest {

    /** 自动装配注册文件的位置（Spring Boot 3 口径） */
    private static final String AUTOCONFIG_IMPORTS =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    /**
     * 第 1 条：classpath 上必须有 z-memory-client-spring-boot-starter 的自动装配注册。
     *
     * <p>失败意味着「只引了 client 没引 starter」⇒ MemoryClient bean 永远不装配。</p>
     *
     * <p>⚠ 实现坑：{@code ClassLoader.getResourceAsStream(path)} 只返回**第一个**命中的资源，
     * 而 {@code META-INF/spring/...AutoConfiguration.imports} 在 classpath 上会有**多个**
     * （druid / mybatis-plus / 各 starter 各一份）⇒ 用它断言必然读到别人的文件。
     * ⇒ 必须用 {@code getResources(path)} 拿到**全部**，逐个读。</p>
     */
    @Test
    void memoryClientStarterMustBeOnClasspathWithAutoconfigRegistration() throws IOException {
        Enumeration<URL> urls = getClass().getClassLoader().getResources(AUTOCONFIG_IMPORTS);
        boolean found = false;
        StringBuilder all = new StringBuilder();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String content;
            try (InputStream in = url.openStream()) {
                content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            all.append(url).append('\n');
            if (content.contains("MemoryClientAutoConfiguration")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "classpath 的 " + AUTOCONFIG_IMPORTS + " 里没有 MemoryClientAutoConfiguration"
                + " ⇒ z-memory-client-spring-boot-starter 没进依赖，"
                + "MemoryClient bean 永远不会装配（remote 模式将静默回退本地表）。"
                + "扫到的资源：" + all);
    }

    /** 兜底：确认 MemoryClient 类型本身可加载（坐标写错/构件缺失时会在这里炸） */
    @Test
    void memoryClientTypeIsLoadable() {
        assertNotNull(MemoryClient.class);
    }

    /**
     * 第 2 条：remote 模式缺 MemoryClient ⇒ 必须 **fail-fast**，不能静默回退 local。
     *
     * <p>这是本次修复的核心行为变更：过去是 warn + 回退（假绿），现在是启动失败。</p>
     */
    @Test
    void remoteModeWithoutClientMustFailFast() {
        ObjectProvider<MemoryClient> emptyProvider = new ObjectProvider<>() {
            @Override
            public MemoryClient getObject() {
                throw new IllegalStateException("should not be called");
            }

            @Override
            public MemoryClient getObject(Object... args) {
                throw new IllegalStateException("should not be called");
            }

            @Override
            public MemoryClient getIfAvailable() {
                return null;
            }

            @Override
            public MemoryClient getIfUnique() {
                return null;
            }
        };

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new AgentMemoryServiceImpl(null, emptyProvider, "remote", "tk"));
        assertTrue(e.getMessage().contains("z.cognition.memory.base-url"),
                "报错信息要指向真正的原因（缺 base-url / 缺 starter），实际：" + e.getMessage());
        assertTrue(e.getMessage().contains("z-memory-client-spring-boot-starter"),
                "报错信息要提示 starter 缺失，实际：" + e.getMessage());
    }

    /** local 模式缺 client 是合法的（默认本地表），不得抛异常 */
    @Test
    void localModeWithoutClientMustNotFail() {
        ObjectProvider<MemoryClient> emptyProvider = new ObjectProvider<>() {
            @Override
            public MemoryClient getObject() {
                throw new IllegalStateException("should not be called");
            }

            @Override
            public MemoryClient getObject(Object... args) {
                throw new IllegalStateException("should not be called");
            }

            @Override
            public MemoryClient getIfAvailable() {
                return null;
            }

            @Override
            public MemoryClient getIfUnique() {
                return null;
            }
        };
        assertDoesNotThrow(() -> new AgentMemoryServiceImpl(null, emptyProvider, "local", ""));
    }
}
