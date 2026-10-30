package com.zifang.z.agent.center.web.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.app.dto.AgentAppDto;
import com.zifang.z.agent.center.core.app.dto.AgentAppReq;
import com.zifang.z.agent.center.core.app.service.AgentAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * Agent 应用管理 Controller.
 * <p>
 * API 基础路径: /api/agent/app
 * 所属模块: z-agent-center-web
 * 鉴权: 由 Spring Security / 网关层拦截（具体策略见 z-ctc 模块）
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /api/agent/app/page       — 分页查询应用列表</li>
 *   <li>GET  /api/agent/app/get        — 按 appCode 查询应用详情</li>
 *   <li>POST /api/agent/app            — 创建应用</li>
 *   <li>POST /api/agent/app/update     — 更新应用</li>
 *   <li>POST /api/agent/app/delete     — 删除应用</li>
 *   <li>POST /api/agent/app/publish    — 发布应用</li>
 *   <li>POST /api/agent/app/upgrade    — 基于已发布版本创建新草稿版本（version+1）</li>
 *   <li>GET  /api/agent/app/versions   — 查询版本列表</li>
 *   <li>GET  /api/agent/app/draft      — 获取草稿</li>
 *   <li>POST /api/agent/app/draft      — 保存草稿</li>
 *   <li>POST /api/agent/app/toggleShare — 切换分享开关，返回当前 shareCode</li>
 * </ul>
 */
@Tag(name = "Agent应用")
@RestController
@RequestMapping("/api/agent/app")
public class AgentAppController {

    @Resource
    private AgentAppService agentAppService;

    /**
     * 分页查询应用列表.
     *
     * @param req 应用查询条件（appName / status / groupCode），其中 {@code req.id} 兼作页码
     * @return 分页结果（每页 12 条）
     */
    @Operation(summary = "分页查询应用")
    @PostMapping("/page")
    public Result<IPage<AgentAppDto>> page(@RequestBody AgentAppReq req) {
        int pageNum = req.getId() != null ? req.getId().intValue() : 1;
        int pageSize = 12;
        IPage<AgentAppDto> page = agentAppService.pageResp(req.getAppName(), req.getStatus(), req.getGroupCode(), pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * RESTful alias: GET /api/agent/app/list?ownerId=&pageNum=&pageSize=
     * <p>用于前端 makeApi('agent').list() 与 makeApi('agent').page() 友好调用.
     * 优先按 ownerId 过滤;无 ownerId 时返回所有.</p>
     */
    @Operation(summary = "列出应用 (RESTful 别名)")
    @GetMapping("/list")
    public Result<List<AgentAppDto>> listAlias(
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String groupCode) {
        // 用 pageResp 拿前 1000 条,简单实现 list
        IPage<AgentAppDto> page = agentAppService.pageResp(null, status, groupCode, 1, 1000);
        List<AgentAppDto> all = page.getRecords();
        if (ownerId != null) {
            // DTO 没有 getOwnerId, 用 getCreator (创建者字段)
            all = all.stream()
                    .filter(a -> ownerId.equals(a.getCreator() == null ? null : a.getCreator().toString()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return Result.success(all);
    }

    /**
     * RESTful alias: GET /api/agent/app/page?pageNum=&pageSize=&appName=&status=&groupCode=
     */
    @Operation(summary = "分页查询应用 (RESTful GET 别名)")
    @GetMapping("/page")
    public Result<IPage<AgentAppDto>> pageAlias(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String groupCode) {
        IPage<AgentAppDto> page = agentAppService.pageResp(appName, status, groupCode, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 按 appCode 查询应用详情.
     *
     * @param appCode 应用编码
     * @return 应用详情 DTO
     */
    @Operation(summary = "查询应用详情")
    @GetMapping("/get")
    public Result<AgentAppDto> get(@RequestParam String appCode) {
        return Result.success(agentAppService.getRespByAppCode(appCode));
    }

    /**
     * 创建应用.
     *
     * @param req 创建参数
     * @return 创建后的应用详情 DTO
     */
    @Operation(summary = "创建应用")
    @PostMapping
    public Result<AgentAppDto> create(@RequestBody AgentAppReq req) {
        return Result.success(agentAppService.createResp(req));
    }

    /**
     * 更新应用.
     *
     * @param req 更新参数
     * @return 更新后的应用详情 DTO
     */
    @Operation(summary = "更新应用")
    @PostMapping("/update")
    public Result<AgentAppDto> update(@RequestBody AgentAppReq req) {
        return Result.success(agentAppService.updateResp(req));
    }

    /**
     * 删除应用.
     *
     * @param id 应用主键 ID
     * @return 通用成功响应
     */
    @Operation(summary = "删除应用")
    @PostMapping("/delete")
    public Result<Object> remove(@RequestParam Long id) {
        agentAppService.remove(id);
        return Result.success();
    }

    /**
     * 发布应用.
     *
     * @param appCode 应用编码
     * @return 通用成功响应
     */
    @Operation(summary = "发布应用")
    @PostMapping("/publish")
    public Result<Object> publish(@RequestParam String appCode) {
        agentAppService.publish(appCode);
        return Result.success();
    }

    /**
     * 基于已发布版本创建新草稿版本（version+1）.
     *
     * @param appCode 应用编码
     * @return 升级后产生的新草稿版本 DTO
     */
    @Operation(summary = "升级：基于已发布版本创建新草稿版本（version+1）")
    @PostMapping("/upgrade")
    public Result<AgentAppDto> upgrade(@RequestParam String appCode) {
        return Result.success(agentAppService.upgradeResp(appCode));
    }

    /**
     * 查询指定应用的全部版本列表.
     *
     * @param appCode 应用编码
     * @return 应用版本 DTO 列表
     */
    @Operation(summary = "查询版本列表")
    @GetMapping("/versions")
    public Result<List<AgentAppDto>> versions(@RequestParam String appCode) {
        return Result.success(agentAppService.listVersionResp(appCode));
    }

    /**
     * 获取指定应用的草稿内容（JSON 字符串）.
     *
     * @param appCode 应用编码
     * @return 草稿 JSON 字符串
     */
    @Operation(summary = "获取草稿")
    @GetMapping("/draft")
    public Result<String> getDraft(@RequestParam String appCode) {
        return Result.success(agentAppService.getDraft(appCode));
    }

    /**
     * 保存草稿内容.
     *
     * @param req 草稿参数（appCode 必须），内部序列化为 JSON 存储
     * @return 通用成功响应
     */
    @Operation(summary = "保存草稿")
    @PostMapping("/draft")
    public Result<Object> saveDraft(@RequestBody AgentAppReq req) {
        String draftJson;
        try {
            draftJson = JsonUtil.toJsonExcludingNulls(req);
        } catch (Exception e) {
            draftJson = "{}";
        }
        agentAppService.saveDraft(req.getAppCode(), draftJson);
        return Result.success();
    }

    /**
     * 切换分享开关，返回当前 shareCode.
     *
     * @param appCode 应用编码
     * @param enabled 是否启用分享
     * @return 切换后的 shareCode
     */
    @Operation(summary = "切换分享开关，返回当前 shareCode")
    @PostMapping("/toggleShare")
    public Result<String> toggleShare(@RequestParam String appCode, @RequestParam Boolean enabled) {
        return Result.success(agentAppService.toggleShare(appCode, enabled));
    }
}
