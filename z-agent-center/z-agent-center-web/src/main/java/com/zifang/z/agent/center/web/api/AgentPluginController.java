package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.plugin.AgentPluginService;
import com.zifang.z.agent.center.core.agent.plugin.entity.AgentPlugin;
import com.zifang.z.agent.center.web.api.response.dto.PageResp;
import com.zifang.z.agent.center.web.api.response.dto.PluginExecuteResp;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

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
    @BizApi(summary = "分页/过滤查询插件", description = "分页/过滤查询插件.", tags = { "z-agent" })
    public Result<PageResp<AgentPlugin>> page(
            @Parameter(description = "关键字 (匹配 plugin_code / plugin_name / description)") @RequestParam(required = false)
            @BizParam(name = "keyword", type = BizField.FieldType.STRING, description = "关键字，匹配 plugin_code / plugin_name / description", position = BizParam.Position.QUERY) String keyword,
            @Parameter(description = "分类") @RequestParam(required = false)
            @BizParam(name = "category", type = BizField.FieldType.STRING, description = "分类", position = BizParam.Position.QUERY) String category,
            @Parameter(description = "状态 (ACTIVE/DISABLED/DEPRECATED)") @RequestParam(required = false)
            @BizParam(name = "status", type = BizField.FieldType.STRING, description = "状态（ACTIVE/DISABLED/DEPRECATED）", position = BizParam.Position.QUERY) String status,
            @Parameter(description = "是否内置 (1=内置, 0=用户, 不传=全部)") @RequestParam(required = false)
            @BizParam(name = "builtin", type = BizField.FieldType.INTEGER, description = "是否内置（1=内置, 0=用户, 不传=全部）", position = BizParam.Position.QUERY) Integer builtin,
            @RequestParam(defaultValue = "1")
            @BizParam(name = "page", type = BizField.FieldType.INTEGER, description = "页码，默认 1", position = BizParam.Position.QUERY) Integer page,
            @RequestParam(defaultValue = "20")
            @BizParam(name = "size", type = BizField.FieldType.INTEGER, description = "每页条数，默认 20", position = BizParam.Position.QUERY) Integer size) {
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
    @BizApi(summary = "按 ID 查询插件", description = "按 ID 查询插件.", tags = { "z-agent" })
    public Result<AgentPlugin> getById(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "插件主键 ID", position = BizParam.Position.QUERY) Long id) {
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
    @BizApi(summary = "按 pluginCode 查询插件", description = "按 pluginCode 查询插件.", tags = { "z-agent" })
    public Result<AgentPlugin> getByCode(@RequestParam @BizParam(name = "pluginCode", type = BizField.FieldType.STRING, description = "插件编码", position = BizParam.Position.QUERY) String pluginCode) {
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
    @BizApi(summary = "创建用户自定义插件（内置插件禁止通过此接口创建）", description = "创建用户自定义插件（内置插件禁止通过此接口创建）.", tags = { "z-agent" })
    public Result<AgentPlugin> create(@RequestBody @BizParam(name = "plugin", type = BizField.FieldType.STRING, description = "待创建的插件实体", position = BizParam.Position.BODY) AgentPlugin plugin) {
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
    @BizApi(summary = "更新插件（内置插件禁止改 executeType/executeConfig）", description = "更新插件（内置插件禁止改 executeType/executeConfig）.", tags = { "z-agent" })
    public Result<AgentPlugin> update(@RequestBody @BizParam(name = "plugin", type = BizField.FieldType.STRING, description = "待更新的插件实体", position = BizParam.Position.BODY) AgentPlugin plugin) {
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
    @BizApi(summary = "启用或禁用插件", description = "启用或禁用插件.", tags = { "z-agent" })
    public Result<AgentPlugin> setStatus(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "插件主键 ID", position = BizParam.Position.QUERY) Long id, @RequestParam @BizParam(name = "status", type = BizField.FieldType.STRING, description = "目标状态值", position = BizParam.Position.QUERY) String status) {
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
    @BizApi(summary = "删除插件（内置插件不可删，改用 setStatus=DISABLED）", description = "删除插件（内置插件不可删，改用 setStatus=DISABLED）.", tags = { "z-agent" })
    public Result<Boolean> delete(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "插件主键 ID", position = BizParam.Position.QUERY) Long id) {
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
    @BizApi(summary = "执行插件，按 pluginCode 路由到 HTTP/BEAN/MCP 调度器", description = "执行插件，按 pluginCode 路由到 HTTP/BEAN/MCP 调度器.", tags = { "z-agent" })
    public Result<PluginExecuteResp> execute(@RequestParam @BizParam(name = "pluginCode", type = BizField.FieldType.STRING, description = "插件编码", position = BizParam.Position.QUERY) String pluginCode,
                                             @RequestBody(required = false)
                                             @BizParam(name = "inputs", type = BizField.FieldType.STRING, description = "输入参数 Map，可为空", position = BizParam.Position.BODY) java.util.Map<String, Object> inputs) {
        return Result.success(new PluginExecuteResp(pluginService.execute(pluginCode, inputs == null ? new HashMap<>() : inputs)));
    }

    /**
     * 列出已注册的内置 Bean Plugin beanName（用于 UI 选择器）.
     *
     * @return 内置 bean 名称集合
     */
    @Operation(summary = "列出已注册的内置 Bean Plugin beanName (用于 UI 选择器)")
    @GetMapping("/builtin-beans")
    @BizApi(summary = "列出已注册的内置 Bean Plugin beanName（用于 UI 选择器）", description = "列出已注册的内置 Bean Plugin beanName（用于 UI 选择器）.", tags = { "z-agent" })
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
    @BizApi(summary = "健康检查端点", description = "健康检查端点.", tags = { "z-agent" })
    public Result<String> health() {
        return Result.success("Agent Plugin API is healthy");
    }
}



