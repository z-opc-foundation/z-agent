package com.zifang.z.agent.center.core.agent.plugin;

import com.zifang.z.agent.center.core.agent.plugin.entity.AgentPlugin;

import java.util.List;
import java.util.Map;

/**
 * FEATURE013 T2.1: Agent Plugin Service
 *
 * <p>Plugin = 可执行工具. 三种 execute_type:
 * <ul>
 *     <li>HTTP — 调用外部 REST API</li>
 *     <li>BEAN — 调用 Spring 容器中的 {@code com.zifang.*} Bean</li>
 *     <li>MCP — 桥接到 MCP Tool</li>
 * </ul>
 */
public interface AgentPluginService {

    /**
     * 按 ID 查.
     */
    AgentPlugin getById(Long id);

    /**
     * 按 pluginCode 查.
     */
    AgentPlugin getByCode(String pluginCode);

    /**
     * 分页/过滤查询. 任意参数可空, 空表示不过滤.
     *
     * @param keyword  关键字 (匹配 plugin_code / plugin_name / description)
     * @param category 分类
     * @param status   状态
     * @param builtin  是否内置 (null = 全部)
     * @param page     1-based
     * @param size     分页大小
     */
    List<AgentPlugin> pageList(String keyword, String category, String status, Integer builtin, int page, int size);

    /**
     * 计数. 同过滤条件.
     */
    long count(String keyword, String category, String status, Integer builtin);

    /**
     * 创建插件 (user-defined). 内置插件不通过此接口创建.
     */
    AgentPlugin create(AgentPlugin plugin);

    /**
     * 更新插件. 内置插件禁止编辑 executeType / executeConfig.
     */
    AgentPlugin update(AgentPlugin plugin);

    /**
     * 启用 / 禁用.
     */
    AgentPlugin setStatus(Long id, String status);

    /**
     * 软删. 内置插件不通过此接口删除.
     */
    boolean delete(Long id);

    /**
     * 执行插件 — 主入口. 按 plugin.execute_type 自动路由到 HTTP / BEAN / MCP 调度器.
     *
     * @param pluginCode 插件编码
     * @param inputs     入参
     * @return 执行结果 Map
     */
    Map<String, Object> execute(String pluginCode, Map<String, Object> inputs);

    /**
     * 列出已注册的内置 Bean Plugin beanName. (用于 UI 选择器)
     */
    java.util.Set<String> listBuiltinBeanNames();
}
