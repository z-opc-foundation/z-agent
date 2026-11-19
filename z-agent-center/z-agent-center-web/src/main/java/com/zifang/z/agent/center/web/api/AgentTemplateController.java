package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.template.entity.AgentTemplate;
import com.zifang.z.agent.center.core.agent.template.service.AgentTemplateService;
import com.zifang.z.agent.center.web.api.response.dto.PageResp;
import com.zifang.z.agent.center.web.api.response.dto.TemplateForkResp;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FEATURE013 T3.3: Agent 模板市场 REST API
 */
@Tag(name = "Agent 模板市场")
@RestController
@RequestMapping("/api/agent/template")
public class AgentTemplateController {

    @Resource
    private AgentTemplateService templateService;

    /**
     * 分页/过滤查询模板.
     *
     * @param keyword  关键字，可为空
     * @param category 分类，可为空
     * @param status   状态，可为空
     * @param page     页码，默认 1
     * @param size     每页条数，默认 20
     * @return 包含 records / total / page / size 的响应 Map
     */
    @Operation(summary = "分页/过滤查询模板")
    @GetMapping("/page")
    @BizApi(summary = "分页/过滤查询模板", description = "分页/过滤查询模板.", tags = { "z-agent" })
    public Result<PageResp<AgentTemplate>> page(
            @RequestParam(required = false)
            @BizParam(name = "keyword", type = BizField.FieldType.STRING, description = "关键字，可为空", position = BizParam.Position.QUERY) String keyword,
            @RequestParam(required = false)
            @BizParam(name = "category", type = BizField.FieldType.STRING, description = "分类，可为空", position = BizParam.Position.QUERY) String category,
            @RequestParam(required = false)
            @BizParam(name = "status", type = BizField.FieldType.STRING, description = "状态，可为空", position = BizParam.Position.QUERY) String status,
            @RequestParam(defaultValue = "1")
            @BizParam(name = "page", type = BizField.FieldType.INTEGER, description = "页码，默认 1", position = BizParam.Position.QUERY) Integer page,
            @RequestParam(defaultValue = "20")
            @BizParam(name = "size", type = BizField.FieldType.INTEGER, description = "每页条数，默认 20", position = BizParam.Position.QUERY) Integer size) {
        List<AgentTemplate> records = templateService.pageList(keyword, category, status, null, page, size);
        long total = templateService.count(keyword, category, status, null);
        PageResp<AgentTemplate> resp = new PageResp<>();
        resp.setRecords(records);
        resp.setTotal(total);
        resp.setPage(page);
        resp.setSize(size);
        return Result.success(resp);
    }

    /**
     * 按 ID 查询模板.
     *
     * @param id 模板主键 ID
     * @return 模板实体
     */
    @Operation(summary = "按 ID 查")
    @GetMapping("/byId")
    @BizApi(summary = "按 ID 查询模板", description = "按 ID 查询模板.", tags = { "z-agent" })
    public Result<AgentTemplate> getById(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "模板主键 ID", position = BizParam.Position.QUERY) Long id) {
        return Result.success(templateService.getById(id));
    }

    /**
     * 热门模板（按下载数 + 评分排序）.
     *
     * @param limit 返回条数上限，默认 12
     * @return 热门模板列表
     */
    @Operation(summary = "热门模板 (按下载数 + 评分排序)")
    @GetMapping("/hot")
    @BizApi(summary = "热门模板（按下载数 + 评分排序）", description = "热门模板（按下载数 + 评分排序）.", tags = { "z-agent" })
    public Result<List<AgentTemplate>> hot(@RequestParam(defaultValue = "12") @BizParam(name = "limit", type = BizField.FieldType.INTEGER, description = "返回条数上限，默认 12", position = BizParam.Position.QUERY) Integer limit) {
        return Result.success(templateService.listHot(limit));
    }

    /**
     * 创建模板.
     *
     * @param template 待创建的模板实体
     * @return 创建后的模板实体
     */
    @Operation(summary = "创建模板")
    @PostMapping("/create")
    @BizApi(summary = "创建模板", description = "创建模板.", tags = { "z-agent" })
    public Result<AgentTemplate> create(@RequestBody @BizParam(name = "template", type = BizField.FieldType.STRING, description = "待创建的模板实体", position = BizParam.Position.BODY) AgentTemplate template) {
        return Result.success(templateService.create(template));
    }

    /**
     * 更新模板.
     *
     * @param template 待更新的模板实体
     * @return 更新后的模板实体
     */
    @Operation(summary = "更新模板")
    @PostMapping("/update")
    @BizApi(summary = "更新模板", description = "更新模板.", tags = { "z-agent" })
    public Result<AgentTemplate> update(@RequestBody @BizParam(name = "template", type = BizField.FieldType.STRING, description = "待更新的模板实体", position = BizParam.Position.BODY) AgentTemplate template) {
        return Result.success(templateService.update(template));
    }

    /**
     * 设置模板状态（DRAFT/PUBLISHED/DEPRECATED）.
     *
     * @param id     模板主键 ID
     * @param status 目标状态值
     * @return 更新后的模板实体
     */
    @Operation(summary = "设置状态 (DRAFT/PUBLISHED/DEPRECATED)")
    @PostMapping("/status")
    @BizApi(summary = "设置模板状态（DRAFT/PUBLISHED/DEPRECATED）", description = "设置模板状态（DRAFT/PUBLISHED/DEPRECATED）.", tags = { "z-agent" })
    public Result<AgentTemplate> setStatus(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "模板主键 ID", position = BizParam.Position.QUERY) Long id, @RequestParam @BizParam(name = "status", type = BizField.FieldType.STRING, description = "目标状态值", position = BizParam.Position.QUERY) String status) {
        return Result.success(templateService.setStatus(id, status));
    }

    /**
     * 软删模板.
     *
     * @param id 模板主键 ID
     * @return 是否删除成功
     */
    @Operation(summary = "软删")
    @PostMapping("/delete")
    @BizApi(summary = "软删模板", description = "软删模板.", tags = { "z-agent" })
    public Result<Boolean> delete(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "模板主键 ID", position = BizParam.Position.QUERY) Long id) {
        return Result.success(templateService.delete(id));
    }

    /**
     * 一键 fork（用模板创建新 AgentApp）.
     *
     * @param id      模板主键 ID
     * @param appName 新建应用的名称，可为空时由 Service 端生成
     * @return fork 结果的响应 Map
     */
    @Operation(summary = "一键 fork (用模板创建新 AgentApp)")
    @PostMapping("/fork")
    @BizApi(summary = "一键 fork（用模板创建新 AgentApp）", description = "一键 fork（用模板创建新 AgentApp）.", tags = { "z-agent" })
    public Result<TemplateForkResp> fork(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "模板主键 ID", position = BizParam.Position.QUERY) Long id,
                                         @RequestParam(required = false)
                                         @BizParam(name = "appName", type = BizField.FieldType.STRING, description = "新建应用的名称，可为空时由 Service 端生成", position = BizParam.Position.QUERY) String appName) {
        java.util.Map<String, Object> map = templateService.fork(id, appName, null, null);
        TemplateForkResp resp = new TemplateForkResp();
        resp.setTemplateId(map.get("templateId") != null ? ((Number) map.get("templateId")).longValue() : null);
        resp.setTemplateCode((String) map.get("templateCode"));
        resp.setNewAppCode((String) map.get("newAppCode"));
        resp.setNewAppName((String) map.get("newAppName"));
        resp.setRequiredPlugins((String) map.get("requiredPlugins"));
        resp.setRequiredSkills((String) map.get("requiredSkills"));
        resp.setRequiredKnowledge((String) map.get("requiredKnowledge"));
        resp.setAppDto(map.get("appDto"));
        return Result.success(resp);
    }

    /**
     * 健康检查端点.
     *
     * @return 固定字符串，表示 Template API 健康
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    @BizApi(summary = "健康检查端点", description = "健康检查端点.", tags = { "z-agent" })
    public Result<String> health() {
        return Result.success("Agent Template API is healthy");
    }
}



