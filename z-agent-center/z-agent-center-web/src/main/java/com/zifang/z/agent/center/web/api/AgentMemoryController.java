package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.memory.entity.AgentMemory;
import com.zifang.z.agent.center.core.agent.memory.service.AgentMemoryService;
import com.zifang.z.agent.center.web.api.response.dto.PromptFragmentResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * FEATURE013 T2.3: Agent 长期记忆 REST API.
 *
 * <p>路径前缀 {@code /api/agent/memory}.
 */
@Tag(name = "Agent 长期记忆")
@RestController
@RequestMapping("/api/agent/memory")
public class AgentMemoryController {

    @Resource
    private AgentMemoryService memoryService;

    /**
     * 写入一条记忆.
     *
     * @param req 记忆写入请求体（userId / appCode / memoryType / content / importance / keywords）
     * @return 写入后的记忆实体
     * @throws IllegalArgumentException 请求体为空时抛出
     */
    @Operation(summary = "写入一条记忆")
    @PostMapping("/write")
    public Result<AgentMemory> write(@RequestBody WriteReq req) {
        if (req == null) {
            throw new IllegalArgumentException("body 必填");
        }

        return Result.success(memoryService.write(
                req.userId, req.appCode, req.memoryType,
                req.content, req.importance, req.keywords));
    }

    /**
     * 检索相关记忆（命中后自动 touch，更新最近访问时间）.
     *
     * @param userId  用户 ID
     * @param appCode 应用编码，可为空
     * @param query   查询关键词，可为空
     * @param limit   返回条数上限，默认 5
     * @return 相关记忆列表
     */
    @Operation(summary = "检索相关记忆 (命中后自动 touch)")
    @GetMapping("/search")
    public Result<List<AgentMemory>> search(@RequestParam String userId,
                                            @RequestParam(required = false) String appCode,
                                            @RequestParam(required = false) String query,
                                            @RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(memoryService.searchRelevant(userId, appCode, query, limit));
    }

    /**
     * 列出某用户某应用的全部记忆（管理后台用）.
     *
     * @param userId  用户 ID
     * @param appCode 应用编码，可为空
     * @param limit   返回条数上限，默认 50
     * @return 记忆列表
     */
    @Operation(summary = "列出某用户某应用的全部记忆 (管理后台用)")
    @GetMapping("/list")
    public Result<List<AgentMemory>> list(@RequestParam String userId,
                                          @RequestParam(required = false) String appCode,
                                          @RequestParam(defaultValue = "50") Integer limit) {
        return Result.success(memoryService.listByUser(userId, appCode, limit));
    }

    /**
     * 软删一条记忆.
     *
     * @param id 记忆主键 ID
     * @return 是否删除成功
     */
    @Operation(summary = "软删一条记忆")
    @PostMapping("/forget")
    public Result<Boolean> forget(@RequestParam Long id) {
        return Result.success(memoryService.forget(id));
    }

    /**
     * 周期衰减所有记忆（factor ∈ (0, 1]）.
     *
     * @param factor 衰减因子，默认 0.95
     * @return 受影响的记忆条数
     */
    @Operation(summary = "周期衰减所有记忆 (factor∈(0,1])")
    @PostMapping("/decay")
    public Result<Integer> decay(@RequestParam(defaultValue = "0.95") Float factor) {
        return Result.success(memoryService.decay(factor));
    }

    /**
     * 检索并格式化为 system prompt 片段.
     *
     * @param userId  用户 ID
     * @param appCode 应用编码，可为空
     * @param query   查询关键词，可为空
     * @return 包含 fragment 文本及其长度的响应 Map
     */
    @Operation(summary = "检索并格式化为 system prompt 片段")
    @GetMapping("/prompt-fragment")
    public Result<PromptFragmentResp> promptFragment(@RequestParam String userId,
                                                     @RequestParam(required = false) String appCode,
                                                     @RequestParam(required = false) String query) {
        String fragment = memoryService.injectForPrompt(userId, appCode, query);
        PromptFragmentResp resp = new PromptFragmentResp();
        resp.setFragment(fragment);
        resp.setLength(fragment.length());
        return Result.success(resp);
    }

    /**
     * 健康检查端点.
     *
     * @return 固定字符串，表示 Memory API 健康
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Agent Memory API is healthy");
    }

    // ========== 请求体 ==========

    /**
     * 记忆写入请求体.
     */
    public static class WriteReq {
        public String userId;
        public String appCode;       // null = 全局用户记忆
        public String memoryType;     // FACT/PREFERENCE/EVENT
        public String content;
        public Float importance;      // 0-1
        public String keywords;       // 空格分隔
    }
}
