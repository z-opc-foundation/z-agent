package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.trigger.entity.AgentTrigger;
import com.zifang.z.agent.center.core.agent.trigger.service.AgentTriggerService;
import com.zifang.z.agent.center.web.api.response.dto.PageResp;
import com.zifang.z.agent.center.web.api.response.dto.TriggerFireNowResp;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FEATURE013 T3.1: Agent 定时触发 REST API
 */
@Tag(name = "Agent 定时触发")
@RestController
@RequestMapping("/api/agent/trigger")
public class AgentTriggerController {

    @Resource
    private AgentTriggerService triggerService;

    /**
     * 分页/过滤查询触发器.
     *
     * @param keyword 关键字，可为空
     * @param appCode 应用编码，可为空
     * @param status  状态，可为空
     * @param page    页码，默认 1
     * @param size    每页条数，默认 20
     * @return 包含 records / total / page / size 的响应 Map
     */
    @Operation(summary = "分页/过滤查询")
    @GetMapping("/page")
    @BizApi(summary = "分页/过滤查询触发器", description = "分页/过滤查询触发器.", tags = { "z-agent" })
    public Result<PageResp<AgentTrigger>> page(
            @RequestParam(required = false)
            @BizParam(name = "keyword", type = BizField.FieldType.STRING, description = "关键字，可为空", position = BizParam.Position.QUERY) String keyword,
            @RequestParam(required = false)
            @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "应用编码，可为空", position = BizParam.Position.QUERY) String appCode,
            @RequestParam(required = false)
            @BizParam(name = "status", type = BizField.FieldType.STRING, description = "状态，可为空", position = BizParam.Position.QUERY) String status,
            @RequestParam(defaultValue = "1")
            @BizParam(name = "page", type = BizField.FieldType.INTEGER, description = "页码，默认 1", position = BizParam.Position.QUERY) Integer page,
            @RequestParam(defaultValue = "20")
            @BizParam(name = "size", type = BizField.FieldType.INTEGER, description = "每页条数，默认 20", position = BizParam.Position.QUERY) Integer size) {
        List<AgentTrigger> records = triggerService.pageList(keyword, appCode, status, page, size);
        long total = triggerService.count(keyword, appCode, status);
        PageResp<AgentTrigger> resp = new PageResp<>();
        resp.setRecords(records);
        resp.setTotal(total);
        resp.setPage(page);
        resp.setSize(size);
        return Result.success(resp);
    }

    /**
     * 按 ID 查询触发器.
     *
     * @param id 触发器主键 ID
     * @return 触发器实体
     */
    @Operation(summary = "按 ID 查")
    @GetMapping("/byId")
    @BizApi(summary = "按 ID 查询触发器", description = "按 ID 查询触发器.", tags = { "z-agent" })
    public Result<AgentTrigger> getById(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "触发器主键 ID", position = BizParam.Position.QUERY) Long id) {
        return Result.success(triggerService.getById(id));
    }

    /**
     * 创建触发器（含 cron 表达式校验 + next_fire_time 自动计算）.
     *
     * @param trigger 待创建的触发器实体
     * @return 创建后的触发器实体
     */
    @Operation(summary = "创建触发器 (含 cron 表达式校验 + next_fire_time 自动计算)")
    @PostMapping("/create")
    @BizApi(summary = "创建触发器（含 cron 表达式校验 + next_fire_time 自动计算）", description = "创建触发器（含 cron 表达式校验 + next_fire_time 自动计算）.", tags = { "z-agent" })
    public Result<AgentTrigger> create(@RequestBody @BizParam(name = "trigger", type = BizField.FieldType.STRING, description = "待创建的触发器实体", position = BizParam.Position.BODY) AgentTrigger trigger) {
        return Result.success(triggerService.create(trigger));
    }

    /**
     * 更新触发器（cron 变更时重算 next_fire_time）.
     *
     * @param trigger 待更新的触发器实体
     * @return 更新后的触发器实体
     */
    @Operation(summary = "更新触发器 (cron 变更时重算 next_fire_time)")
    @PostMapping("/update")
    @BizApi(summary = "更新触发器（cron 变更时重算 next_fire_time）", description = "更新触发器（cron 变更时重算 next_fire_time）.", tags = { "z-agent" })
    public Result<AgentTrigger> update(@RequestBody @BizParam(name = "trigger", type = BizField.FieldType.STRING, description = "待更新的触发器实体", position = BizParam.Position.BODY) AgentTrigger trigger) {
        return Result.success(triggerService.update(trigger));
    }

    /**
     * 启用/暂停/停用触发器（恢复 ACTIVE 时重算 next_fire_time）.
     *
     * @param id     触发器主键 ID
     * @param status 目标状态值
     * @return 更新后的触发器实体
     */
    @Operation(summary = "启用/暂停/停用 (恢复 ACTIVE 时重算 next_fire_time)")
    @PostMapping("/status")
    @BizApi(summary = "启用/暂停/停用触发器（恢复 ACTIVE 时重算 next_fire_time）", description = "启用/暂停/停用触发器（恢复 ACTIVE 时重算 next_fire_time）.", tags = { "z-agent" })
    public Result<AgentTrigger> setStatus(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "触发器主键 ID", position = BizParam.Position.QUERY) Long id, @RequestParam @BizParam(name = "status", type = BizField.FieldType.STRING, description = "目标状态值", position = BizParam.Position.QUERY) String status) {
        return Result.success(triggerService.setStatus(id, status));
    }

    /**
     * 软删触发器.
     *
     * @param id 触发器主键 ID
     * @return 是否删除成功
     */
    @Operation(summary = "软删")
    @PostMapping("/delete")
    @BizApi(summary = "软删触发器", description = "软删触发器.", tags = { "z-agent" })
    public Result<Boolean> delete(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "触发器主键 ID", position = BizParam.Position.QUERY) Long id) {
        return Result.success(triggerService.delete(id));
    }

    /**
     * 手动触发（立即执行一次，忽略 cron）.
     *
     * @param id 触发器主键 ID
     * @return 包含 ok / answer 的响应 Map
     */
    @Operation(summary = "手动触发 (立即执行一次, 忽略 cron)")
    @PostMapping("/fire")
    @BizApi(summary = "手动触发（立即执行一次，忽略 cron）", description = "手动触发（立即执行一次，忽略 cron）.", tags = { "z-agent" })
    public Result<TriggerFireNowResp> fireNow(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "触发器主键 ID", position = BizParam.Position.QUERY) Long id) {
        String answer = triggerService.fireNow(id);
        TriggerFireNowResp resp = new TriggerFireNowResp();
        resp.setOk(true);
        resp.setAnswer(answer);
        return Result.success(resp);
    }

    /**
     * 健康检查端点.
     *
     * @return 固定字符串，表示 Trigger API 健康
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    @BizApi(summary = "健康检查端点", description = "健康检查端点.", tags = { "z-agent" })
    public Result<String> health() {
        return Result.success("Agent Trigger API is healthy");
    }
}



