package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.humaninput.entity.AgentFlowHumanInput;
import com.zifang.z.agent.center.core.agent.humaninput.service.AgentFlowHumanInputService;
import com.zifang.z.agent.center.web.api.response.dto.HumanInboxResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * FEATURE013 T3.5: Human-in-the-Loop REST API
 *
 * <p>主要面向"收件箱"前端 (HumanTaskInbox):
 * <ul>
 *     <li>GET  /api/agent/human/inbox?assignee={name}  → 某人待办 (assignee 留空 = 全部待办)</li>
 *     <li>GET  /api/agent/human/execution?executionId={exec}  → 某次流程执行的所有 human-input 请求 (含已决策)</li>
 *     <li>GET  /api/agent/human/byId?id={id}          → 详情 (含 prompt / form_schema / result)</li>
 *     <li>POST /api/agent/human/decide?id={id}        → 决策 (通过 / 拒绝 / 取消)</li>
 *     <li>POST /api/agent/human/create                → 手动创建 PENDING 请求 (供调试/集成测试用)</li>
 *     <li>GET  /api/agent/human/health                → 健康检查</li>
 * </ul>
 */
@Tag(name = "Agent Human-in-the-Loop")
@RestController
@RequestMapping("/api/agent/human")
public class AgentFlowHumanInputController {

    @Resource
    private AgentFlowHumanInputService humanInputService;

    /**
     * 收件箱：列出某用户的待办 human-input 请求.
     *
     * @param assignee 被指派人，传 {@code _all} / {@code -} / {@code all} 表示全部待办
     * @param limit    返回条数上限，默认 50
     * @return 包含 records / total / assignee 的响应 Map
     */
    @Operation(summary = "收件箱: 列出某用户的待办 (assignee 留空=全部)")
    @GetMapping("/inbox")
    public Result<HumanInboxResp> inbox(
            @RequestParam(required = false) String assignee,
            @RequestParam(defaultValue = "50") Integer limit) {
        if ("_all".equals(assignee) || "-".equals(assignee) || "all".equals(assignee)) {
            assignee = null;
        }
        List<AgentFlowHumanInput> list = humanInputService.listPending(assignee, limit);
        HumanInboxResp resp = new HumanInboxResp();
        resp.setRecords(list);
        resp.setTotal(list.size());
        resp.setAssignee(assignee);
        return Result.success(resp);
    }

    /**
     * 列出某 Flow 执行的所有 human-input 请求（含已决策）.
     *
     * @param executionId 流程执行 ID
     * @return 该执行下的全部 human-input 列表
     */
    @Operation(summary = "列出某 Flow 执行的所有 human-input 请求")
    @GetMapping("/execution")
    public Result<List<AgentFlowHumanInput>> listByExecution(@RequestParam String executionId) {
        return Result.success(humanInputService.listByExecution(executionId));
    }

    /**
     * 按 ID 查询 human-input 详情.
     *
     * @param id 主键 ID
     * @return human-input 实体
     */
    @Operation(summary = "按 ID 查详情")
    @GetMapping("/byId")
    public Result<AgentFlowHumanInput> getById(@RequestParam Long id) {
        return Result.success(humanInputService.getById(id));
    }

    /**
     * 按 requestCode 查询 human-input 详情.
     *
     * @param requestCode 请求编码
     * @return human-input 实体
     */
    @Operation(summary = "按 requestCode 查详情")
    @GetMapping("/by-code")
    public Result<AgentFlowHumanInput> getByCode(@RequestParam String requestCode) {
        return Result.success(humanInputService.getByCode(requestCode));
    }

    /**
     * 对 human-input 请求进行决策.
     *
     * @param id  human-input 主键 ID
     * @param req 决策请求体（status / decidedBy / resultJson / resultText / comments）
     * @return 决策后的 human-input 实体
     */
    @Operation(summary = "决策: 通过 / 拒绝 / 取消")
    @PostMapping("/decide")
    public Result<AgentFlowHumanInput> decide(@RequestParam Long id, @RequestBody DecideReq req) {
        return Result.success(humanInputService.decide(
                id, req.status, req.decidedBy, req.resultJson, req.resultText, req.comments));
    }

    /**
     * 手动创建一条 PENDING 状态的 human-input 请求.
     * <p>供测试或外部系统调用。
     *
     * @param req 待创建的 human-input 实体
     * @return 创建后的 human-input 实体
     */
    @Operation(summary = "手动创建 PENDING 请求 (供测试 / 外部系统调用)")
    @PostMapping("/create")
    public Result<AgentFlowHumanInput> create(@RequestBody AgentFlowHumanInput req) {
        return Result.success(humanInputService.createPending(req));
    }

    /**
     * 健康检查端点.
     *
     * @return 固定字符串，表示 Human-in-the-Loop API 健康
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Agent Human-in-the-Loop API is healthy");
    }

    /**
     * Human-input 决策请求体.
     */
    public static class DecideReq {
        public String status;       // APPROVED / REJECTED / CANCELLED
        public String decidedBy;    // 决策人 (用户名)
        public String resultJson;   // 表单结果 (JSON 字符串, 可选)
        public String resultText;   // 文本结果 (可选)
        public String comments;     // 审批意见 (可选)
    }
}
