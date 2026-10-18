package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.trigger.entity.AgentTrigger;
import com.zifang.z.agent.center.core.agent.trigger.service.AgentTriggerService;
import com.zifang.z.agent.center.web.api.response.dto.PageResp;
import com.zifang.z.agent.center.web.api.response.dto.TriggerFireNowResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
    public Result<PageResp<AgentTrigger>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
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
    public Result<AgentTrigger> getById(@RequestParam Long id) {
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
    public Result<AgentTrigger> create(@RequestBody AgentTrigger trigger) {
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
    public Result<AgentTrigger> update(@RequestBody AgentTrigger trigger) {
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
    public Result<AgentTrigger> setStatus(@RequestParam Long id, @RequestParam String status) {
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
    public Result<Boolean> delete(@RequestParam Long id) {
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
    public Result<TriggerFireNowResp> fireNow(@RequestParam Long id) {
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
    public Result<String> health() {
        return Result.success("Agent Trigger API is healthy");
    }
}
