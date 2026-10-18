package com.zifang.z.agent.center.core.agent.humaninput.config;

import com.zifang.z.agent.center.core.agent.humaninput.node.HumanInputNode;
import com.zifang.z.agent.center.core.agent.humaninput.service.AgentFlowHumanInputService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * FEATURE013 T3.5: HumanInputNode 启动注册器
 *
 * <p>Spring 启动时:
 * <ol>
 *     <li>把 AgentFlowHumanInputService 注入到 HumanInputNode 的静态 ServiceLocator
 *         (FlowEngine 内部手工 new 节点, 不走 Spring 注入, 必须用静态定位器)</li>
 * </ol>
 *
 * <p>注: 节点实例化本身仍由 FlowEngine/FlowDefinitionService 完成 (按 FlowDefinition 里的
 * config 字段构造). 本类只负责把 service 注入到静态字段.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class HumanInputNodeRegistrar implements CommandLineRunner {

    private static final Logger log = LogManager.getLogger(HumanInputNodeRegistrar.class);

    @Autowired
    private AgentFlowHumanInputService humanInputService;

    @PostConstruct
    public void init() {
        HumanInputNode.registerServiceLocator(humanInputService);
        log.info("[HumanInputNodeRegistrar] 已注册 ServiceLocator (Spring @PostConstruct)");
    }

    @Override
    public void run(String... args) {
        // 二次保险: 如果 PostConstruct 未生效 (例如某些 Test 场景), 在启动完成时再注册一次
        if (!isRegistered()) {
            HumanInputNode.registerServiceLocator(humanInputService);
            log.info("[HumanInputNodeRegistrar] 二次注册 ServiceLocator (CommandLineRunner)");
        }
    }

    private boolean isRegistered() {
        // 通过反射读取静态字段
        try {
            java.lang.reflect.Field f = HumanInputNode.class.getDeclaredField("SERVICE_LOCATOR");
            f.setAccessible(true);
            return f.get(null) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
