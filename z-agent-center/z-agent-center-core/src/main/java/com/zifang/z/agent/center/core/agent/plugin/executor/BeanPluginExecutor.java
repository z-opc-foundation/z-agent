package com.zifang.z.agent.center.core.agent.plugin.executor;

import java.util.Map;

/**
 * FEATURE013 T2.1: Plugin Executor — Bean 模式插件执行接口
 *
 * <p>实现类标注 {@code @Component("beanName")} 后，会被
 * {@code BeanPluginDispatcher} 按 plugin.execute_config.beanName 字段查找并调用.
 *
 * <p>安全约束:
 * <ul>
 *     <li>Bean 必须由 Spring 容器管理 — 防止任意 Java 类被作为插件执行</li>
 *     <li>仅查找 {@code com.zifang.*} 包下的 Bean — 防止 core 包类被注入</li>
 *     <li>超时由 Plugin 实体 timeoutMs 字段控制（外层调用方负责）</li>
 * </ul>
 */
public interface BeanPluginExecutor {

    /**
     * 执行插件逻辑.
     *
     * @param inputs 入参 (来自 plugin.input_schema 校验后的对象)
     * @return 出参 (会被 plugin.output_schema 描述)
     * @throws Exception 业务异常或参数错误
     */
    Map<String, Object> execute(Map<String, Object> inputs) throws Exception;
}
