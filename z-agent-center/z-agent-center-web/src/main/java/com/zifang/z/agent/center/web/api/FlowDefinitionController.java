package com.zifang.z.agent.center.web.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.flow.FlowContext;
import com.zifang.z.agent.center.core.agent.flow.FlowOrchestrationService;
import com.zifang.z.agent.center.core.agent.flow.FlowResult;
import com.zifang.z.agent.center.core.agent.flow.entity.FlowDefinitionPO;
import com.zifang.z.agent.center.core.agent.flow.service.FlowDefinitionService;
import com.zifang.z.agent.center.web.api.response.dto.FlowHealthResp;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义 Controller (FEATURE013 A4)
 * 路径: /api/agent/flow
 */
@Tag(name = "Agent工作流")
@RestController
@RequestMapping("/api/agent/flow")
public class FlowDefinitionController {


    @Autowired
    private FlowDefinitionService flowDefinitionService;

    @Autowired
    private FlowOrchestrationService flowOrchestrationService;

    /**
     * 分页查询流程列表.
     *
     * @param keyword  关键字，可为空
     * @param status   状态，可为空
     * @param appCode  应用编码，可为空
     * @param pageNum  页码，默认 1
     * @param pageSize 每页条数，默认 20
     * @return 流程定义分页结果
     */
    @Operation(summary = "分页查询流程列表")
    @GetMapping("/list")
    @BizApi(summary = "分页查询流程列表", description = "分页查询流程列表.", tags = { "z-agent" })
    public Result<IPage<FlowDefinitionPO>> list(
            @RequestParam(required = false)
            @BizParam(name = "keyword", type = BizField.FieldType.STRING, description = "关键字，可为空", position = BizParam.Position.QUERY) String keyword,
            @RequestParam(required = false)
            @BizParam(name = "status", type = BizField.FieldType.STRING, description = "状态，可为空", position = BizParam.Position.QUERY) String status,
            @RequestParam(required = false)
            @BizParam(name = "appCode", type = BizField.FieldType.STRING, description = "应用编码，可为空", position = BizParam.Position.QUERY) String appCode,
            @RequestParam(defaultValue = "1")
            @BizParam(name = "pageNum", type = BizField.FieldType.INTEGER, description = "页码，默认 1", position = BizParam.Position.QUERY) int pageNum,
            @RequestParam(defaultValue = "20")
            @BizParam(name = "pageSize", type = BizField.FieldType.INTEGER, description = "每页条数，默认 20", position = BizParam.Position.QUERY) int pageSize) {
        return Result.success(flowDefinitionService.page(keyword, status, appCode, pageNum, pageSize));
    }

    /**
     * 按主键查询流程详情.
     *
     * @param id 主键 ID
     * @return 流程定义实体
     */
    @Operation(summary = "查询流程详情")
    @GetMapping("/byId")
    @BizApi(summary = "按主键查询流程详情", description = "按主键查询流程详情.", tags = { "z-agent" })
    public Result<FlowDefinitionPO> get(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "主键 ID", position = BizParam.Position.QUERY) Long id) {
        return Result.success(flowDefinitionService.getById(id));
    }

    /**
     * 按 flowId + version 查询流程详情.
     *
     * @param flowId  流程业务编码
     * @param version 版本号，默认 1
     * @return 流程定义实体
     */
    @Operation(summary = "按 flowId + version 查询详情")
    @GetMapping("/byFlowId")
    @BizApi(summary = "按 flowId + version 查询流程详情", description = "按 flowId + version 查询流程详情.", tags = { "z-agent" })
    public Result<FlowDefinitionPO> getByFlowIdAndVersion(
            @RequestParam @BizParam(name = "flowId", type = BizField.FieldType.STRING, description = "流程业务编码", position = BizParam.Position.QUERY) String flowId,
            @RequestParam(defaultValue = "1")
            @BizParam(name = "version", type = BizField.FieldType.INTEGER, description = "版本号，默认 1", position = BizParam.Position.QUERY) Integer version) {
        return Result.success(flowDefinitionService.getByFlowIdAndVersion(flowId, version));
    }

    /**
     * 查询指定流程的全部版本.
     *
     * @param flowId 流程业务编码
     * @return 流程定义列表
     */
    @Operation(summary = "查询流程所有版本")
    @GetMapping("/versions")
    @BizApi(summary = "查询指定流程的全部版本", description = "查询指定流程的全部版本.", tags = { "z-agent" })
    public Result<List<FlowDefinitionPO>> versions(@RequestParam @BizParam(name = "flowId", type = BizField.FieldType.STRING, description = "流程业务编码", position = BizParam.Position.QUERY) String flowId) {
        return Result.success(flowDefinitionService.listByFlowId(flowId));
    }

    /**
     * 创建流程定义（DRAFT）.
     *
     * @param po 待创建的流程定义
     * @return 新建流程的主键 ID
     */
    @Operation(summary = "创建流程定义 (DRAFT)")
    @PostMapping("/create")
    @BizApi(summary = "创建流程定义（DRAFT）", description = "创建流程定义（DRAFT）.", tags = { "z-agent" })
    public Result<Long> create(@RequestBody @BizParam(name = "po", type = BizField.FieldType.STRING, description = "待创建的流程定义", position = BizParam.Position.BODY) FlowDefinitionPO po) {
        if (po.getFlowId() == null || po.getFlowId().isEmpty()) {
            return Result.fail("flowId 不能为空");
        }
        return Result.success(flowDefinitionService.create(po));
    }

    /**
     * 更新流程定义.
     *
     * @param po 待更新的流程定义
     * @return 是否更新成功
     */
    @Operation(summary = "更新流程定义")
    @PostMapping("/update")
    @BizApi(summary = "更新流程定义", description = "更新流程定义.", tags = { "z-agent" })
    public Result<Boolean> update(@RequestBody @BizParam(name = "po", type = BizField.FieldType.STRING, description = "待更新的流程定义", position = BizParam.Position.BODY) FlowDefinitionPO po) {
        return Result.success(flowDefinitionService.update(po));
    }

    /**
     * 发布流程（DRAFT → PUBLISHED）.
     * <p>发布成功后会刷新 in-memory 缓存。
     *
     * @param id 主键 ID
     * @return 是否发布成功
     */
    @Operation(summary = "发布流程 (DRAFT → PUBLISHED)")
    @PostMapping("/publish")
    @BizApi(summary = "发布流程（DRAFT → PUBLISHED）", description = "发布流程（DRAFT → PUBLISHED）. 发布成功后会刷新 in-memory 缓存。", tags = { "z-agent" })
    public Result<Boolean> publish(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "主键 ID", position = BizParam.Position.QUERY) Long id) {
        boolean ok = flowDefinitionService.publish(id);
        if (ok) {
            // 刷新 in-memory cache
            FlowDefinitionPO latest = flowDefinitionService.getById(id);
            if (latest != null) {
                flowOrchestrationService.refreshFlowCache(latest.getFlowId());
            }
        }
        return Result.success(ok);
    }

    /**
     * 归档流程（PUBLISHED → ARCHIVED）.
     * <p>归档成功后会刷新 in-memory 缓存。
     *
     * @param id 主键 ID
     * @return 是否归档成功
     */
    @Operation(summary = "归档流程 (PUBLISHED → ARCHIVED)")
    @PostMapping("/archive")
    @BizApi(summary = "归档流程（PUBLISHED → ARCHIVED）", description = "归档流程（PUBLISHED → ARCHIVED）. 归档成功后会刷新 in-memory 缓存。", tags = { "z-agent" })
    public Result<Boolean> archive(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "主键 ID", position = BizParam.Position.QUERY) Long id) {
        boolean ok = flowDefinitionService.archive(id);
        if (ok) {
            FlowDefinitionPO latest = flowDefinitionService.getById(id);
            if (latest != null) {
                flowOrchestrationService.refreshFlowCache(latest.getFlowId());
            }
        }
        return Result.success(ok);
    }

    /**
     * 创建新版本（version+1，parentVersionId=当前 id）.
     *
     * @param parentId  父版本主键 ID
     * @param changelog 变更说明，可为空
     * @param creator   创建人，可为空
     * @return 新版本的主键 ID
     */
    @Operation(summary = "创建新版本 (version+1, parentVersionId=当前id)")
    @PostMapping("/newVersion")
    @BizApi(summary = "创建新版本（version+1，parentVersionId=当前 id）", description = "创建新版本（version+1，parentVersionId=当前 id）.", tags = { "z-agent" })
    public Result<Long> createNewVersion(
            @RequestParam @BizParam(name = "parentId", type = BizField.FieldType.INTEGER, description = "父版本主键 ID", position = BizParam.Position.QUERY) Long parentId,
            @RequestParam(required = false)
            @BizParam(name = "changelog", type = BizField.FieldType.STRING, description = "变更说明，可为空", position = BizParam.Position.QUERY) String changelog,
            @RequestParam(required = false)
            @BizParam(name = "creator", type = BizField.FieldType.STRING, description = "创建人，可为空", position = BizParam.Position.QUERY) String creator) {
        return Result.success(flowDefinitionService.createNewVersion(parentId, changelog, creator));
    }

    /**
     * 逻辑删除流程.
     * <p>删除成功后会刷新 in-memory 缓存。
     *
     * @param id 主键 ID
     * @return 是否删除成功
     */
    @Operation(summary = "逻辑删除流程")
    @PostMapping("/delete")
    @BizApi(summary = "逻辑删除流程", description = "逻辑删除流程. 删除成功后会刷新 in-memory 缓存。", tags = { "z-agent" })
    public Result<Boolean> delete(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "主键 ID", position = BizParam.Position.QUERY) Long id) {
        boolean ok = flowDefinitionService.delete(id);
        if (ok) {
            FlowDefinitionPO latest = flowDefinitionService.getById(id);
            if (latest != null) {
                flowOrchestrationService.refreshFlowCache(latest.getFlowId());
            }
        }
        return Result.success(ok);
    }

    /**
     * 执行流程（FEATURE013 A4：通过 DB 加载 flow）.
     * <p>仅允许执行状态为 {@code PUBLISHED} 的流程。请求体可包含 {@code userId} / {@code userName} / {@code message}
     * 以及其他业务变量（自动写入 {@link FlowContext}）。
     *
     * @param id   流程定义主键 ID
     * @param body 请求体，可为空
     * @return 流程执行结果
     */
    @Operation(summary = "执行流程 (FEATURE013 A4: 通过 DB 加载 flow)")
    @PostMapping("/execute")
    @BizApi(summary = "执行流程（FEATURE013 A4：通过 DB 加载 flow）", description = "执行流程（FEATURE013 A4：通过 DB 加载 flow）. 仅允许执行状态为 PUBLISHED 的流程。请求体可包含 userId / userName / message 以及其他业务变量（自动写入 FlowContext）。", tags = { "z-agent" })
    public Result<FlowResult> execute(
            @RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "流程定义主键 ID", position = BizParam.Position.QUERY) Long id,
            @RequestBody(required = false)
            @BizParam(name = "body", type = BizField.FieldType.JSON, description = "请求体，可为空", position = BizParam.Position.BODY) Map<String, Object> body) {
        FlowDefinitionPO po = flowDefinitionService.getById(id);
        if (po == null) {
            return Result.fail("流程不存在: " + id);
        }
        if (!"PUBLISHED".equals(po.getStatus())) {
            return Result.fail("流程未发布, 不能执行: status=" + po.getStatus());
        }
        String userId = body == null ? "anonymous" : (String) body.getOrDefault("userId", "anonymous");
        String userName = body == null ? "anonymous" : (String) body.getOrDefault("userName", "anonymous");
        String userMessage = body == null ? "" : (String) body.getOrDefault("message", "");

        FlowContext ctx = new FlowContext(userId, userName);
        ctx.setVariable("_user_message", userMessage);
        if (body != null) {
            for (Map.Entry<String, Object> e : body.entrySet()) {
                if (!e.getKey().equals("userId") && !e.getKey().equals("userName")
                        && !e.getKey().equals("message")) {
                    ctx.setVariable(e.getKey(), e.getValue());
                }
            }
        }
        FlowResult result = flowOrchestrationService.execute(po.getFlowId(), ctx);
        return Result.success(result);
    }

    /**
     * 健康检查端点（返回当前 in-memory 缓存的 flow 数）.
     *
     * @return 包含 cached_flow_count / flow_ids 的响应 Map
     */
    @Operation(summary = "健康检查: 当前 in-memory 缓存的 flow 数")
    @GetMapping("/health")
    @BizApi(summary = "健康检查端点（返回当前 in-memory 缓存的 flow 数）", description = "健康检查端点（返回当前 in-memory 缓存的 flow 数）.", tags = { "z-agent" })
    public Result<FlowHealthResp> health() {
        FlowHealthResp resp = new FlowHealthResp();
        resp.setCachedFlowCount(flowOrchestrationService.getAllFlows().size());
        resp.setFlowIds(flowOrchestrationService.getAllFlows().keySet());
        return Result.success(resp);
    }
}

