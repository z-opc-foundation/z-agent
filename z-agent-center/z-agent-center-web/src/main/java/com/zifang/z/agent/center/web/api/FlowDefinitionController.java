package com.zifang.z.agent.center.web.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.flow.FlowContext;
import com.zifang.z.agent.center.core.agent.flow.FlowOrchestrationService;
import com.zifang.z.agent.center.core.agent.flow.FlowResult;
import com.zifang.z.agent.center.core.agent.flow.entity.FlowDefinitionPO;
import com.zifang.z.agent.center.core.agent.flow.service.FlowDefinitionService;
import com.zifang.z.agent.center.web.api.response.dto.FlowHealthResp;
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
    public Result<IPage<FlowDefinitionPO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String appCode,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
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
    public Result<FlowDefinitionPO> get(@RequestParam Long id) {
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
    public Result<FlowDefinitionPO> getByFlowIdAndVersion(
            @RequestParam String flowId,
            @RequestParam(defaultValue = "1") Integer version) {
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
    public Result<List<FlowDefinitionPO>> versions(@RequestParam String flowId) {
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
    public Result<Long> create(@RequestBody FlowDefinitionPO po) {
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
    public Result<Boolean> update(@RequestBody FlowDefinitionPO po) {
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
    public Result<Boolean> publish(@RequestParam Long id) {
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
    public Result<Boolean> archive(@RequestParam Long id) {
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
    public Result<Long> createNewVersion(
            @RequestParam Long parentId,
            @RequestParam(required = false) String changelog,
            @RequestParam(required = false) String creator) {
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
    public Result<Boolean> delete(@RequestParam Long id) {
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
    public Result<FlowResult> execute(
            @RequestParam Long id,
            @RequestBody(required = false) Map<String, Object> body) {
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
    public Result<FlowHealthResp> health() {
        FlowHealthResp resp = new FlowHealthResp();
        resp.setCachedFlowCount(flowOrchestrationService.getAllFlows().size());
        resp.setFlowIds(flowOrchestrationService.getAllFlows().keySet());
        return Result.success(resp);
    }
}