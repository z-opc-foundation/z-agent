package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.plugin.AgentPluginService;
import com.zifang.z.agent.center.core.agent.plugin.entity.AgentPlugin;
import com.zifang.z.agent.center.web.api.response.dto.PageResp;
import com.zifang.z.agent.center.web.api.response.dto.PluginExecuteResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * FEATURE013 T2.1: Agent Plugin REST API
 *
 * <p>路径前缀 {@code /api/agent/plugin}. 所有端点 GET/POST + JSON.
 */
@Tag(name = "Agent Plugin 可执行插件")
@RestController
@RequestMapping("/api/agent/plugin")
public class AgentPluginController {

    @Resource
    private AgentPluginService pluginService;

    /**
     * 分页/过滤查询插件.
     *
     * @param keyword  关键字，匹配 plugin_code / plugin_name / description
     * @param category 分类
     * @param status   状态（ACTIVE/DISABLED/DEPRECATED）
     * @param builtin  是否内置（1=内置, 0=用户, 不传=全部）
     * @param page     页码，默认 1
     * @param size     每页条数，默认 20
     * @return 包含 records / total / page / size 的响应 Map
     */
    @Operation(summary = "分页/过滤查询插件")
    @GetMapping("/page")
    public Result<PageResp<AgentPlugin>> page(
            @Parameter(description = "关键字 (匹配 plugin_code / plugin_name / description)") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "状态 (ACTIVE/DISABLED/DEPRECATED)") @RequestParam(required = false) String status,
            @Parameter(description = "是否内置 (1=内置, 0=用户, 不传=全部)") @RequestParam(required = false) Integer builtin,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        List<AgentPlugin> records = pluginService.pageList(keyword, category, status, builtin, page, size);
        long total = pluginService.count(keyword, category, status, builtin);
        PageResp<AgentPlugin> resp = new PageResp<>();
        resp.setRecords(records);
        resp.setTotal(total);
        resp.setPage(page);
        resp.setSize(size);
        return Result.success(resp);
    }

    /**
     * 按 ID 查询插件.
     *
     * @param id 插件主键 ID
     * @return 插件实体
     */
    @Operation(summary = "按 ID 查")
    @GetMapping("/byId")
    public Result<AgentPlugin> getById(@RequestParam Long id) {
        return Result.success(pluginService.getById(id));
    }

    /**
     * 按 pluginCode 查询插件.
     *
     * @param pluginCode 插件编码
     * @return 插件实体
     */
    @Operation(summary = "按 pluginCode 查")
    @GetMapping("/byCode")
    public Result<AgentPlugin> getByCode(@RequestParam String pluginCode) {
        return Result.success(pluginService.getByCode(pluginCode));
    }

    /**
     * 创建用户自定义插件（内置插件禁止通过此接口创建）.
     *
     * @param plugin 待创建的插件实体
     * @return 创建后的插件实体
     */
    @Operation(summary = "创建用户自定义插件 (内置插件禁止通过此接口创建)")
    @PostMapping("/create")
    public Result<AgentPlugin> create(@RequestBody AgentPlugin plugin) {
        return Result.success(pluginService.create(plugin));
    }

    /**
     * 更新插件（内置插件禁止改 executeType/executeConfig）.
     *
     * @param plugin 待更新的插件实体
     * @return 更新后的插件实体
     */
    @Operation(summary = "更新插件 (内置插件禁止改 executeType/executeConfig)")
    @PostMapping("/update")
    public Result<AgentPlugin> update(@RequestBody AgentPlugin plugin) {
        return Result.success(pluginService.update(plugin));
    }

    /**
     * 启用或禁用插件.
     *
     * @param id     插件主键 ID
     * @param status 目标状态值
     * @return 更新后的插件实体
     */
    @Operation(summary = "启用/禁用")
    @PostMapping("/status")
    public Result<AgentPlugin> setStatus(@RequestParam Long id, @RequestParam String status) {
        return Result.success(pluginService.setStatus(id, status));
    }

    /**
     * 删除插件（内置插件不可删，改用 setStatus=DISABLED）.
     *
     * @param id 插件主键 ID
     * @return 是否删除成功
     */
    @Operation(summary = "删除 (内置插件不可删, 改用 setStatus=DISABLED)")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestParam Long id) {
        return Result.success(pluginService.delete(id));
    }

    /**
     * 执行插件，按 pluginCode 路由到 HTTP/BEAN/MCP 调度器.
     *
     * @param pluginCode 插件编码
     * @param inputs     输入参数 Map，可为空
     * @return 插件执行结果
     */
    @Operation(summary = "执行插件 — 按 pluginCode 路由到 HTTP/BEAN/MCP 调度器")
    @PostMapping("/execute")
    public Result<PluginExecuteResp> execute(@RequestParam String pluginCode,
                                             @RequestBody(required = false) java.util.Map<String, Object> inputs) {
        return Result.success(new PluginExecuteResp(pluginService.execute(pluginCode, inputs == null ? new HashMap<>() : inputs)));
    }

    /**
     * 列出已注册的内置 Bean Plugin beanName（用于 UI 选择器）.
     *
     * @return 内置 bean 名称集合
     */
    @Operation(summary = "列出已注册的内置 Bean Plugin beanName (用于 UI 选择器)")
    @GetMapping("/builtin-beans")
    public Result<Set<String>> listBuiltinBeanNames() {
        return Result.success(pluginService.listBuiltinBeanNames());
    }

    /**
     * 健康检查端点.
     *
     * @return 固定字符串，表示 Plugin API 健康
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Agent Plugin API is healthy");
    }
}
