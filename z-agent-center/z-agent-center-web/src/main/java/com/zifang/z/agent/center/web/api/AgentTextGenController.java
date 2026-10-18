package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.textgen.entity.AgentTextGenResult;
import com.zifang.z.agent.center.core.agent.textgen.entity.AgentTextGenTask;
import com.zifang.z.agent.center.core.agent.textgen.service.AgentTextGenService;
import com.zifang.z.agent.center.web.api.response.dto.PageResp;
import com.zifang.z.agent.center.web.api.response.dto.TextGenCreateAndRunResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * FEATURE013 T3.4: Agent 文本生成器 REST API
 */
@Tag(name = "Agent 文本生成器")
@RestController
@RequestMapping("/api/agent/textgen")
public class AgentTextGenController {

    @Resource
    private AgentTextGenService textGenService;

    /**
     * 分页/过滤查询任务.
     *
     * @param keyword 关键字，可为空
     * @param appCode 应用编码，可为空
     * @param status  状态，可为空
     * @param page    页码，默认 1
     * @param size    每页条数，默认 20
     * @return 包含 records / total / page / size 的响应 Map
     */
    @Operation(summary = "分页/过滤查询任务")
    @GetMapping("/page")
    public Result<PageResp<AgentTextGenTask>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String appCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        List<AgentTextGenTask> records = textGenService.pageList(keyword, appCode, status, page, size);
        long total = textGenService.count(keyword, appCode, status);
        PageResp<AgentTextGenTask> resp = new PageResp<>();
        resp.setRecords(records);
        resp.setTotal(total);
        resp.setPage(page);
        resp.setSize(size);
        return Result.success(resp);
    }

    /**
     * 按 ID 查询任务（含 output_csv）.
     *
     * @param id 任务主键 ID
     * @return 任务实体
     */
    @Operation(summary = "按 ID 查 (含 output_csv)")
    @GetMapping("/byId")
    public Result<AgentTextGenTask> getById(@RequestParam Long id) {
        return Result.success(textGenService.getById(id));
    }

    /**
     * 查询任务的每行结果.
     *
     * @param id 任务主键 ID
     * @return 每行结果列表
     */
    @Operation(summary = "查任务的每行结果")
    @GetMapping("/results")
    public Result<List<AgentTextGenResult>> listResults(@RequestParam Long id) {
        return Result.success(textGenService.listResults(id));
    }

    /**
     * 创建任务（不立即执行）.
     *
     * @param task 待创建的任务实体
     * @return 创建后的任务实体
     */
    @Operation(summary = "创建任务 (不立即执行)")
    @PostMapping("/create")
    public Result<AgentTextGenTask> create(@RequestBody AgentTextGenTask task) {
        return Result.success(textGenService.create(task));
    }

    /**
     * 软删任务.
     *
     * @param id 任务主键 ID
     * @return 是否删除成功
     */
    @Operation(summary = "软删")
    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestParam Long id) {
        return Result.success(textGenService.delete(id));
    }

    /**
     * 执行任务（同步，阻塞到全部完成）.
     *
     * @param id 任务主键 ID
     * @return 执行后的任务实体
     */
    @Operation(summary = "执行任务 (同步, 阻塞到全部完成)")
    @PostMapping("/run")
    public Result<AgentTextGenTask> run(@RequestParam Long id) {
        return Result.success(textGenService.run(id));
    }

    /**
     * 一键创建并执行（小批量 demo 用）.
     *
     * @param req 创建并执行请求体（taskName / appCode / promptTemplate / rows / ownerId / ownerName）
     * @return 创建并执行的结果 Map
     */
    @Operation(summary = "一键创建并执行 (小批量 demo 用)")
    @PostMapping("/createAndRun")
    public Result<TextGenCreateAndRunResp> createAndRun(@RequestBody CreateRunReq req) {
        Map<String, Object> map = textGenService.createAndRun(
                req.taskName, req.appCode, req.promptTemplate, req.rows, req.ownerId, req.ownerName);
        TextGenCreateAndRunResp resp = new TextGenCreateAndRunResp();
        resp.setTaskCode((String) map.get("taskCode"));
        resp.setTaskId(map.get("taskId") != null ? ((Number) map.get("taskId")).longValue() : null);
        resp.setStatus((String) map.get("status"));
        resp.setTotalCount(map.get("totalCount") != null ? ((Number) map.get("totalCount")).intValue() : 0);
        resp.setSuccessCount(map.get("successCount") != null ? ((Number) map.get("successCount")).intValue() : 0);
        resp.setFailedCount(map.get("failedCount") != null ? ((Number) map.get("failedCount")).intValue() : 0);
        resp.setElapsedMs(map.get("elapsedMs") != null ? ((Number) map.get("elapsedMs")).longValue() : null);
        return Result.success(resp);
    }

    /**
     * 健康检查端点.
     *
     * @return 固定字符串，表示 TextGen API 健康
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Agent TextGen API is healthy");
    }

    /**
     * 一键创建并执行请求体.
     */
    public static class CreateRunReq {
        public String taskName;
        public String appCode;
        public String promptTemplate;
        public String ownerId;
        public String ownerName;
        public List<Map<String, Object>> rows;
    }
}
