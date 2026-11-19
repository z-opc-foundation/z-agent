package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.app.entity.AgentAk;
import com.zifang.z.agent.center.core.app.service.AgentAkService;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AccessKey (AK) 管理 Controller.
 * <p>
 * API 基础路径: /api/ak
 * 所属模块: z-agent-center-web
 * 鉴权: 由 Spring Security / 网关层拦截（具体策略见 z-ctc 模块）
 *
 * <p>主要端点:
 * <ul>
 *   <li>GET  /api/ak/list   — 查询 AK 列表（支持 tenantCode / akType 过滤）</li>
 *   <li>GET  /api/ak/get    — 按 accessKey 查询单个 AK 详情</li>
 *   <li>POST /api/ak/create — 创建新的 AK 并返回 accessKey / secretKey</li>
 *   <li>POST /api/ak/update — 更新 AK 信息</li>
 *   <li>POST /api/ak/delete — 删除 AK</li>
 *   <li>POST /api/ak/toggle — 启用或禁用 AK</li>
 * </ul>
 */
@Tag(name = "AccessKey管理")
@RestController
@RequestMapping("/api/ak")
public class AgentAkController {

    @Resource
    private AgentAkService akService;

    /**
     * 查询 AK 列表.
     *
     * @param tenantCode 租户编码，可为空表示不过滤
     * @param akType     AK 类型，可为空表示不过滤
     * @return 符合条件的 AK 列表
     */
    @Operation(summary = "查询AK列表")
    @GetMapping("/list")
    @BizApi(summary = "查询 AK 列表", description = "查询 AK 列表.", tags = { "z-agent" })
    public Result<List<AgentAk>> list(@RequestParam(required = false) @BizParam(name = "tenantCode", type = BizField.FieldType.STRING, description = "租户编码，可为空表示不过滤", position = BizParam.Position.QUERY) String tenantCode,
                                      @RequestParam(required = false)
                                      @BizParam(name = "akType", type = BizField.FieldType.STRING, description = "AK 类型，可为空表示不过滤", position = BizParam.Position.QUERY) String akType) {
        return Result.success(akService.list(tenantCode, akType));
    }

    /**
     * 按 accessKey 查询单个 AK 详情.
     *
     * @param accessKey 访问密钥
     * @return 对应的 AK 实体，未找到时返回空结果
     */
    @Operation(summary = "查询单个AK")
    @GetMapping("/get")
    @BizApi(summary = "按 accessKey 查询单个 AK 详情", description = "按 accessKey 查询单个 AK 详情.", tags = { "z-agent" })
    public Result<AgentAk> get(@RequestParam @BizParam(name = "accessKey", type = BizField.FieldType.STRING, description = "访问密钥", position = BizParam.Position.QUERY) String accessKey) {
        return Result.success(akService.getByAccessKey(accessKey));
    }

    /**
     * 创建新的 AK.
     *
     * @param ak 待创建的 AK 实体（accessKey / secretKey 可由服务端生成）
     * @return 包含生成的 accessKey 与 secretKey 的 Map
     */
    @Operation(summary = "创建AK")
    @PostMapping("/create")
    @BizApi(summary = "创建新的 AK", description = "创建新的 AK.", tags = { "z-agent" })
    public Result<Map<String, String>> create(@RequestBody @BizParam(name = "ak", type = BizField.FieldType.STRING, description = "待创建的 AK 实体（accessKey / secretKey 可由服务端生成）", position = BizParam.Position.BODY) AgentAk ak) {
        AgentAk created = akService.create(ak);
        Map<String, String> result = new HashMap<>();
        result.put("accessKey", created.getAccessKey());
        result.put("secretKey", created.getSecretKey());
        return Result.success(result);
    }

    /**
     * 更新 AK 信息.
     *
     * @param ak 待更新的 AK 实体（按主键匹配）
     * @return 更新后的 AK 实体
     */
    @Operation(summary = "更新AK")
    @PostMapping("/update")
    @BizApi(summary = "更新 AK 信息", description = "更新 AK 信息.", tags = { "z-agent" })
    public Result<AgentAk> update(@RequestBody @BizParam(name = "ak", type = BizField.FieldType.STRING, description = "待更新的 AK 实体（按主键匹配）", position = BizParam.Position.BODY) AgentAk ak) {
        return Result.success(akService.update(ak));
    }

    /**
     * 删除指定 AK.
     *
     * @param id 主键 ID
     * @return 通用成功响应
     */
    @Operation(summary = "删除AK")
    @PostMapping("/delete")
    @BizApi(summary = "删除指定 AK", description = "删除指定 AK.", tags = { "z-agent" })
    public Result<Object> delete(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "主键 ID", position = BizParam.Position.QUERY) Long id) {
        akService.delete(id);
        return Result.success();
    }

    /**
     * 启用或禁用指定 AK.
     *
     * @param id     主键 ID
     * @param status 目标状态值（具体语义由 Service 层定义）
     * @return 通用成功响应
     */
    @Operation(summary = "启用/禁用AK")
    @PostMapping("/toggle")
    @BizApi(summary = "启用或禁用指定 AK", description = "启用或禁用指定 AK.", tags = { "z-agent" })
    public Result<Object> toggle(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "主键 ID", position = BizParam.Position.QUERY) Long id, @RequestParam @BizParam(name = "status", type = BizField.FieldType.INTEGER, description = "目标状态值（具体语义由 Service 层定义）", position = BizParam.Position.QUERY) Integer status) {
        akService.toggleStatus(id, status);
        return Result.success();
    }
}



