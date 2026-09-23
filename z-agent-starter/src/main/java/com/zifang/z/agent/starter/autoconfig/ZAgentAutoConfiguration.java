package com.zifang.z.agent.starter.autoconfig;

import com.zifang.z.agent.core.agent.LlmAdapter;
import com.zifang.z.agent.core.properties.AgentProperties;
import com.zifang.z.agent.core.registry.AgentRegistry;
import com.zifang.z.agent.core.runner.AgentRunner;
import com.zifang.z.agent.kernel.llm.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * z-agent 自动装配. 业务方 @SpringBootApplication 后 Spring 会自动扫到这个 @Configuration,
 * 拉起 AgentRegistry + AgentRunner + AgentController + LlmAdapter.
 *
 * <p>LlmAdapter 会自动注入所有 kernel.llm.LlmProvider bean (6 个 provider 默认实现).
 *
 * <p>AgentController 由 @ComponentScan 通过 @RestController 自动发现, 不要重复 @Bean.
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
@ComponentScan(basePackages = {
        "com.zifang.z.agent.core.controller",
        "com.zifang.z.agent.core.registry",
        "com.zifang.z.agent.core.runner",
        "com.zifang.z.agent.core.agent",
        "com.zifang.z.agent.core.properties"
})
public class ZAgentAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ZAgentAutoConfiguration.class);

    @Bean
    public AgentRegistry agentRegistry() {
        return new AgentRegistry();
    }

    @Bean
    public LlmAdapter llmAdapter(AgentProperties props,
                                  ObjectProvider<LlmProvider> providerBeans) {
        LlmAdapter adapter = new LlmAdapter(props);
        int n = 0;
        for (LlmProvider p : providerBeans) {
            if (p == null) continue;
            adapter.registerProvider(p);
            n++;
        }
        log.info("ZAgentAutoConfiguration: registered {} LlmProvider(s) into LlmAdapter", n);
        return adapter;
    }

    @Bean
    public AgentRunner agentRunner(AgentRegistry registry) {
        return new AgentRunner(registry);
    }
}