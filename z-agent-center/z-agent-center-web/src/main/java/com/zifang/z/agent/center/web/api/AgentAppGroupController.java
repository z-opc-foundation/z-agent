package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.app.entity.AgentAppGroup;
import com.zifang.z.agent.center.core.app.service.AgentAppGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * Agent 应用分组管理 Controller.
 * <p>
 * API 基础路径: /api/agent/group
 * 所属模块: z-agent-center-web
 * 鉴权: 由 Spring Security / 网关层拦截（具体策略见 z-ctc 模块）
 *
 * <p>主要端点:
 * <ul>
 *   <li>GET  /api/agent/group/tree    — 查询分组树</li>
 *   <li>GET  /api/agent/group          — 按 groupCode 查询分组详情</li>
 *   <li>POST /api/agent/group          — 创建分组</li>
 *   <li>POST /api/agent/group/update   — 更新分组</li>
 *   <li>POST /api/agent/group/delete   — 删除分组</li>
 * </ul>
 */
@Tag(name = "Agent应用分组")
@RestController
@RequestMapping("/api/agent/group")
public class AgentAppGroupController {

    @Resource
    private AgentAppGroupService groupService;

    /**
     * 查询分组树.
     *
     * @param tenantCode 租户编码，可为空表示不过滤
     * @return 分组树列表
     */
    @Operation(summary = "查询分组树")
    @GetMapping("/tree")
    public Result<List<AgentAppGroup>> tree(@RequestParam(required = false) String tenantCode) {
        return Result.success(groupService.listTree(tenantCode));
    }

    /**
     * 按 groupCode 查询单个分组.
     *
     * @param groupCode 分组编码
     * @return 分组实体
     */
    @Operation(summary = "查询单个分组")
    @GetMapping
    public Result<AgentAppGroup> get(@RequestParam String groupCode) {
        return Result.success(groupService.getByCode(groupCode));
    }

    /**
     * 创建分组.
     *
     * @param group 待创建的分组实体
     * @return 创建后的分组实体
     */
    @Operation(summary = "创建分组")
    @PostMapping
    public Result<AgentAppGroup> create(@RequestBody AgentAppGroup group) {
        return Result.success(groupService.create(group));
    }

    /**
     * 更新分组.
     *
     * @param group 待更新的分组实体
     * @return 更新后的分组实体
     */
    @Operation(summary = "更新分组")
    @PostMapping("/update")
    public Result<AgentAppGroup> update(@RequestBody AgentAppGroup group) {
        return Result.success(groupService.update(group));
    }

    /**
     * 删除分组.
     *
     * @param groupCode 分组编码
     * @return 通用成功响应
     */
    @Operation(summary = "删除分组")
    @PostMapping("/delete")
    public Result<Object> delete(@RequestParam String groupCode) {
        groupService.delete(groupCode);
        return Result.success();
    }
}