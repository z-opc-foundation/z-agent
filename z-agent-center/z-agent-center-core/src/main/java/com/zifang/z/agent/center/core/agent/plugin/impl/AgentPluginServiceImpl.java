package com.zifang.z.agent.center.core.agent.plugin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.agent.plugin.AgentPluginService;
import com.zifang.z.agent.center.core.agent.plugin.entity.AgentPlugin;
import com.zifang.z.agent.center.core.agent.plugin.executor.BeanPluginDispatcher;
import com.zifang.z.agent.center.core.agent.plugin.executor.BeanPluginExecutor;
import com.zifang.z.agent.center.core.agent.plugin.executor.HttpPluginExecutor;
import com.zifang.z.agent.center.core.agent.plugin.mapper.AgentPluginMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentPluginServiceImpl implements AgentPluginService {

    private static final Logger log = LogManager.getLogger(AgentPluginServiceImpl.class);

    @Resource
    private AgentPluginMapper pluginMapper;

    @Resource
    private HttpPluginExecutor httpExecutor;

    @Resource
    private BeanPluginDispatcher beanDispatcher;

    @Override
    public AgentPlugin getById(Long id) {
        if (id == null) {
            return null;
        }

        return pluginMapper.selectById(id);
    }

    @Override
    public AgentPlugin getByCode(String pluginCode) {
        if (pluginCode == null || pluginCode.isEmpty()) {
            return null;
        }
        return pluginMapper.selectOne(new LambdaQueryWrapper<AgentPlugin>()
                .eq(AgentPlugin::getPluginCode, pluginCode)
                .eq(AgentPlugin::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    @Override
    public List<AgentPlugin> pageList(String keyword, String category, String status,
                                      Integer builtin, int page, int size) {
        if (page < 1) {
            page = 1;
        }

        if (size < 1 || size > 200) {
            size = 20;
        }

        LambdaQueryWrapper<AgentPlugin> q = new LambdaQueryWrapper<>();
        q.eq(AgentPlugin::getIsDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AgentPlugin::getPluginCode, kw)
                    .or().like(AgentPlugin::getPluginName, kw)
                    .or().like(AgentPlugin::getDescription, kw));
        }
        if (category != null && !category.trim().isEmpty()) {
            q.eq(AgentPlugin::getCategoryCode, category.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            q.eq(AgentPlugin::getStatus, status.trim());
        }
        if (builtin != null) {
            q.eq(AgentPlugin::getBuiltin, builtin);
        }
        q.orderByDesc(AgentPlugin::getBuiltin).orderByDesc(AgentPlugin::getId);
        IPage<AgentPlugin> pageResult = pluginMapper.selectPage(new Page<>(page, size), q);
        return pageResult.getRecords();
    }

    @Override
    public long count(String keyword, String category, String status, Integer builtin) {
        LambdaQueryWrapper<AgentPlugin> q = new LambdaQueryWrapper<>();
        q.eq(AgentPlugin::getIsDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AgentPlugin::getPluginCode, kw)
                    .or().like(AgentPlugin::getPluginName, kw)
                    .or().like(AgentPlugin::getDescription, kw));
        }
        if (category != null && !category.trim().isEmpty()) {
            q.eq(AgentPlugin::getCategoryCode, category.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            q.eq(AgentPlugin::getStatus, status.trim());
        }
        if (builtin != null) {
            q.eq(AgentPlugin::getBuiltin, builtin);
        }
        return pluginMapper.selectCount(q);
    }

    @Override
    public AgentPlugin create(AgentPlugin plugin) {
        if (plugin == null || plugin.getPluginCode() == null) {
            throw new IllegalArgumentException("pluginCode 必填");
        }
        AgentPlugin exist = getByCode(plugin.getPluginCode());
        if (exist != null) {
            throw new IllegalStateException("pluginCode 已存在: " + plugin.getPluginCode());
        }
        if (plugin.getBuiltin() == null) plugin.setBuiltin(0);

        if (plugin.getStatus() == null) plugin.setStatus("ACTIVE");

        if (plugin.getTimeoutMs() == null) plugin.setTimeoutMs(30000);

        if (plugin.getAuthType() == null) plugin.setAuthType("NONE");

        if (plugin.getExecuteType() == null) plugin.setExecuteType("HTTP");

        plugin.setIsDeleted(0);
        plugin.setDownloadCount(0L);
        plugin.setGmtCreate(LocalDateTime.now());
        plugin.setGmtModified(LocalDateTime.now());
        validateExecuteConfig(plugin);
        pluginMapper.insert(plugin);
        return plugin;
    }

    @Override
    public AgentPlugin update(AgentPlugin plugin) {
        if (plugin == null || plugin.getId() == null) {
            throw new IllegalArgumentException("id 必填");
        }
        AgentPlugin old = pluginMapper.selectById(plugin.getId());
        if (old == null) {
            throw new IllegalStateException("Plugin 不存在: id=" + plugin.getId());
        }
        if (old.getBuiltin() != null && old.getBuiltin() == 1) {
            // 内置插件禁止改 executeType / executeConfig
            if (plugin.getExecuteType() != null && !plugin.getExecuteType().equals(old.getExecuteType())) {
                throw new IllegalStateException("内置插件禁止修改 executeType");
            }
            if (plugin.getExecuteConfig() != null && !plugin.getExecuteConfig().equals(old.getExecuteConfig())) {
                throw new IllegalStateException("内置插件禁止修改 executeConfig");
            }
        }
        plugin.setPluginCode(old.getPluginCode()); // code 不可改
        plugin.setBuiltin(old.getBuiltin());       // builtin 不可改
        plugin.setIsDeleted(old.getIsDeleted());
        plugin.setGmtModified(LocalDateTime.now());
        validateExecuteConfig(plugin);
        pluginMapper.updateById(plugin);
        return pluginMapper.selectById(plugin.getId());
    }

    @Override
    public AgentPlugin setStatus(Long id, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status) && !"DEPRECATED".equals(status)) {
            throw new IllegalArgumentException("status 必须是 ACTIVE/DISABLED/DEPRECATED");
        }
        AgentPlugin p = pluginMapper.selectById(id);
        if (p == null) {
            throw new IllegalStateException("Plugin 不存在");
        }

        p.setStatus(status);
        p.setGmtModified(LocalDateTime.now());
        pluginMapper.updateById(p);
        return p;
    }

    @Override
    public boolean delete(Long id) {
        AgentPlugin p = pluginMapper.selectById(id);
        if (p == null) {
            return false;
        }

        if (p.getBuiltin() != null && p.getBuiltin() == 1) {
            throw new IllegalStateException("内置插件不可删除, 改为 setStatus=DISABLED");
        }
        p.setIsDeleted(1);
        p.setGmtModified(LocalDateTime.now());
        return pluginMapper.updateById(p) > 0;
    }

    @Override
    public Map<String, Object> execute(String pluginCode, Map<String, Object> inputs) {
        if (pluginCode == null || pluginCode.isEmpty()) {
            throw new IllegalArgumentException("pluginCode 必填");
        }
        AgentPlugin plugin = getByCode(pluginCode);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin 不存在: " + pluginCode);
        }
        if (!"ACTIVE".equals(plugin.getStatus())) {
            throw new IllegalStateException("Plugin 已停用: " + pluginCode);
        }
        // 引用计数 +1 (best-effort, 不阻塞执行)
        try {
            plugin.setDownloadCount((plugin.getDownloadCount() == null ? 0L : plugin.getDownloadCount()) + 1);
            plugin.setGmtModified(LocalDateTime.now());
            pluginMapper.updateById(plugin);
        } catch (Exception ignored) {
        }
        long startMs = System.currentTimeMillis();
        String mode = plugin.getExecuteType() == null ? "HTTP" : plugin.getExecuteType().toUpperCase();
        try {
            Map<String, Object> execConfig = parseJsonMap(plugin.getExecuteConfig());
            Map<String, Object> result;
            switch (mode) {
                case "HTTP":
                    result = httpExecutor.execute(execConfig, inputs, plugin.getTimeoutMs());
                    break;
                case "BEAN": {
                    String beanName = (String) execConfig.get("beanName");
                    BeanPluginExecutor exec = beanDispatcher.resolve(beanName);
                    result = exec.execute(inputs == null ? new HashMap<>() : inputs);
                    break;
                }
                case "MCP":
                    // FEATURE013 T2.1 后续扩展: MCP 模式 executor 当前为占位 (MCP server API 正在重构).
                    // 校验 serverCode/toolName 字段存在, 但实际执行留给 MCP 集成完成后再接.
                    Map<String, Object> mcpResp = new HashMap<>();
                    mcpResp.put("mode", "MCP");
                    mcpResp.put("serverCode", execConfig.get("serverCode"));
                    mcpResp.put("toolName", execConfig.get("toolName"));
                    mcpResp.put("status", "PLACEHOLDER");
                    mcpResp.put("message", "MCP 模式 executor 即将上线, 详见 FEATURE013 后续版本");
                    result = mcpResp;
                    break;
                default:
                    throw new IllegalArgumentException("不支持的 execute_type: " + mode);
            }
            long elapsed = System.currentTimeMillis() - startMs;
            log.info("[AgentPlugin] execute OK plugin={} mode={} elapsed={}ms",
                    pluginCode, mode, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startMs;
            log.warn("[AgentPlugin] execute FAIL plugin={} mode={} elapsed={}ms err={}",
                    pluginCode, mode, elapsed, e.getMessage());
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getClass().getSimpleName());
            err.put("message", e.getMessage());
            err.put("elapsedMs", elapsed);
            throw new RuntimeException("插件执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public java.util.Set<String> listBuiltinBeanNames() {
        return beanDispatcher.listAvailableBeanNames();
    }

    // ========== helpers ==========

    private void validateExecuteConfig(AgentPlugin p) {
        String mode = p.getExecuteType() == null ? "" : p.getExecuteType().toUpperCase();
        switch (mode) {
            case "HTTP":
                Map<String, Object> http = parseJsonMap(p.getExecuteConfig());
                if (http.get("url") == null || String.valueOf(http.get("url")).trim().isEmpty()) {
                    throw new IllegalArgumentException("HTTP 插件 execute_config.url 必填");
                }
                break;
            case "BEAN":
                Map<String, Object> bean = parseJsonMap(p.getExecuteConfig());
                if (bean.get("beanName") == null || String.valueOf(bean.get("beanName")).trim().isEmpty()) {
                    throw new IllegalArgumentException("BEAN 插件 execute_config.beanName 必填");
                }
                break;
            case "MCP":
                Map<String, Object> mcp = parseJsonMap(p.getExecuteConfig());
                if (mcp.get("serverCode") == null || mcp.get("toolName") == null) {
                    throw new IllegalArgumentException("MCP 插件 execute_config 需要 {serverCode, toolName}");
                }
                break;
            default:
                throw new IllegalArgumentException("不支持的 execute_type: " + mode + " (HTTP/BEAN/MCP)");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return toPlainMap(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("execute_config JSON 解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 字符串 → 深度摊平的 {@code Map<String,Object>}。
     * <p>
     * 必须过 {@code JsonUtil.toPlainValue}：{@code parseToMap} 只摊平第一层，
     * 嵌套节点仍是 {@code JsonObject}/{@code JsonArray}，下游按 {@code List}/{@code Map}
     * 强转会 ClassCastException。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toPlainMap(String json) {
        return (Map<String, Object>) JsonUtil.toPlainValue(JsonUtil.parse(json));
    }
}
