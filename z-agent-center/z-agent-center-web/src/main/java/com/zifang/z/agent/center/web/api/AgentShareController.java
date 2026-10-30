package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.share.dto.AgentShareDto;
import com.zifang.z.agent.center.core.agent.share.dto.AgentShareReq;
import com.zifang.z.agent.center.core.agent.share.service.AgentShareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * Agent 分享管理 Controller.
 * <p>
 * API 基础路径: /api/agent/share
 * 所属模块: z-agent-center-web
 * 鉴权: 由 Spring Security / 网关层拦截（具体策略见 z-ctc 模块）
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /api/agent/share/create  — 生成分享链接</li>
 *   <li>GET  /api/agent/share/verify — 验证分享码并返回实例信息</li>
 *   <li>GET  /api/agent/share/list   — 查询实例的所有分享记录</li>
 *   <li>POST /api/agent/share/disable — 禁用分享</li>
 * </ul>
 */
@Tag(name = "Agent分享")
@RestController
@RequestMapping("/api/agent/share")
public class AgentShareController {

    @Resource
    private AgentShareService shareService;

    /**
     * 生成分享链接.
     *
     * @param req 分享请求参数
     * @return 分享 DTO（含 shareCode / shareUrl 等）
     */
    @Operation(summary = "生成分享链接")
    @PostMapping("/create")
    public Result<AgentShareDto> create(@RequestBody AgentShareReq req) {
        return Result.success(shareService.createResp(req));
    }

    /**
     * 验证分享码并返回实例信息.
     *
     * @param shareCode 分享码
     * @return 分享 DTO（含对应实例信息）
     */
    @Operation(summary = "验证分享码并返回实例信息")
    @GetMapping("/verify")
    public Result<AgentShareDto> verify(@RequestParam String shareCode) {
        return Result.success(shareService.verifyResp(shareCode));
    }

    /**
     * 查询指定实例的所有分享记录.
     *
     * @param instanceCode Agent 实例编码
     * @return 分享 DTO 列表
     */
    @Operation(summary = "查询实例的所有分享记录")
    @GetMapping("/list")
    public Result<List<AgentShareDto>> list(@RequestParam String instanceCode) {
        return Result.success(shareService.listRespByInstance(instanceCode));
    }

    /**
     * 禁用指定分享.
     *
     * @param shareCode 分享码
     * @return 通用成功响应
     */
    @Operation(summary = "禁用分享")
    @PostMapping("/disable")
    public Result<Object> disable(@RequestParam String shareCode) {
        shareService.disable(shareCode);
        return Result.success();
    }
}
