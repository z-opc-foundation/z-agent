package com.zifang.z.agent.center.core.agent.plugin.executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FEATURE013 T2.1: Plugin Executor — Bean 模式调度器
 *
 * <p>按 {@code execute_config.beanName} 从 Spring 容器取 {@link BeanPluginExecutor} 实现.
 */
@Component
public class BeanPluginDispatcher implements ApplicationContextAware {

    private static final Logger log = LogManager.getLogger(BeanPluginDispatcher.class);

    private final Map<String, BeanPluginExecutor> cache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            Map<String, BeanPluginExecutor> beans = ctx.getBeansOfType(BeanPluginExecutor.class);
            for (Map.Entry<String, BeanPluginExecutor> e : beans.entrySet()) {
                String beanName = e.getKey();
                Class<?> beanClass = e.getValue().getClass();
                String pkg = beanClass.getPackage() == null ? "" : beanClass.getPackage().getName();
                if (!pkg.startsWith("com.zifang.")) {
                    log.warn("[BeanPluginDispatcher] 拒绝注册非 com.zifang 包 Bean: {} ({})",
                            beanName, beanClass.getName());
                    continue;
                }
                cache.put(beanName, e.getValue());
                log.info("[BeanPluginDispatcher] 注册内置 Bean Plugin: {} -> {}",
                        beanName, beanClass.getSimpleName());
            }
            initialized = true;
        }
    }

    public BeanPluginExecutor resolve(String beanName) {
        if (beanName == null || beanName.isEmpty()) {
            throw new IllegalArgumentException("execute_config.beanName 不能为空");
        }
        BeanPluginExecutor exec = cache.get(beanName);
        if (exec == null) {
            throw new IllegalArgumentException("找不到 Bean Plugin: " + beanName
                    + "（已注册: " + cache.keySet() + "）");
        }
        return exec;
    }

    public Set<String> listAvailableBeanNames() {
        return new LinkedHashSet<>(cache.keySet());
    }
}
