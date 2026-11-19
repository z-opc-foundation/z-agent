package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.conversation.dto.AgentChatReq;
import com.zifang.z.agent.center.core.agent.conversation.dto.ChatDto;
import com.zifang.z.agent.center.core.agent.conversation.service.AgentConversationService;
import com.zifang.z.agent.center.core.agent.runtime.service.AgentRuntimeService;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Agent 对话 REST API Controller.
 * <p>
 * API 基础路径: /api/agent/chat
 * 所属模块: z-agent-center-web
 * 鉴权: 由 Spring Security / 网关层拦截（具体策略见 z-ctc 模块）
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /api/agent/chat/send     — 发送对话消息（同步）</li>
 *   <li>GET  /api/agent/chat/stream   — 发送对话消息（流式 SSE）</li>
 *   <li>GET  /api/agent/chat/history  — 查询历史消息</li>
 *   <li>POST /api/agent/chat/clear    — 清除会话历史</li>
 * </ul>
 */
@Tag(name = "Agent对话")
@RestController
@RequestMapping("/api/agent/chat")
public class AgentChatController {

    @Resource
    private AgentRuntimeService agentRuntimeService;

    @Resource
    private AgentConversationService conversationService;

    /**
     * 发送对话消息（同步）.
     *
     * @param req 对话请求参数（实例编码 / 消息内容 / 用户信息）
     * @return 对话结果 DTO
     */
    @Operation(summary = "发送对话消息(同步)")
    @PostMapping("/send")
    @BizApi(summary = "发送对话消息（同步）", description = "发送对话消息（同步）.", tags = { "z-agent" })
    public Result<ChatDto> send(@RequestBody @BizParam(name = "req", type = BizField.FieldType.STRING, description = "对话请求参数（实例编码 / 消息内容 / 用户信息）", position = BizParam.Position.BODY) AgentChatReq req) {
        return Result.success(agentRuntimeService.chatResp(
                req.getInstanceCode(),
                req.getMessage(),
                req.getUserId(),
                req.getUserName()
        ));
    }

    /**
     * 发送对话消息（流式 SSE）.
     *
     * @param instanceCode Agent 实例编码
     * @param message      消息内容
     * @param userId       用户 ID
     * @param userName     用户名
     * @param response     Servlet 响应，用于写入 SSE 数据
     * @throws IOException 写入响应失败时抛出
     */
    @Operation(summary = "发送对话消息(流式)")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @BizApi(summary = "发送对话消息（流式 SSE）", description = "发送对话消息（流式 SSE）.", tags = { "z-agent" })
    public void stream(@RequestParam @BizParam(name = "instanceCode", type = BizField.FieldType.STRING, description = "Agent 实例编码", position = BizParam.Position.QUERY) String instanceCode,
                       @RequestParam @BizParam(name = "message", type = BizField.FieldType.STRING, description = "消息内容", position = BizParam.Position.QUERY) String message,
                       @RequestParam @BizParam(name = "userId", type = BizField.FieldType.STRING, description = "用户 ID", position = BizParam.Position.QUERY) String userId,
                       @RequestParam @BizParam(name = "userName", type = BizField.FieldType.STRING, description = "用户名", position = BizParam.Position.QUERY) String userName,
                       HttpServletResponse response) throws IOException {
        agentRuntimeService.chatStream(instanceCode, message, userId, userName, response);
    }

    /**
     * 查询指定实例的历史对话消息.
     *
     * @param instanceCode Agent 实例编码
     * @param limit        返回条数上限，默认 50
     * @return 对话历史 DTO 列表
     */
    @Operation(summary = "查询历史消息")
    @GetMapping("/history")
    @BizApi(summary = "查询指定实例的历史对话消息", description = "查询指定实例的历史对话消息.", tags = { "z-agent" })
    public Result<List<ChatDto>> history(@RequestParam @BizParam(name = "instanceCode", type = BizField.FieldType.STRING, description = "Agent 实例编码", position = BizParam.Position.QUERY) String instanceCode, @RequestParam(defaultValue = "50") @BizParam(name = "limit", type = BizField.FieldType.INTEGER, description = "返回条数上限，默认 50", position = BizParam.Position.QUERY) Integer limit) {
        return Result.success(conversationService.listRespByInstance(instanceCode, limit));
    }

    /**
     * 清除指定实例的会话历史.
     *
     * @param instanceCode Agent 实例编码
     * @return 通用成功响应
     */
    @Operation(summary = "清除会话历史")
    @PostMapping("/clear")
    @BizApi(summary = "清除指定实例的会话历史", description = "清除指定实例的会话历史.", tags = { "z-agent" })
    public Result<Object> clear(@RequestParam @BizParam(name = "instanceCode", type = BizField.FieldType.STRING, description = "Agent 实例编码", position = BizParam.Position.QUERY) String instanceCode) {
        agentRuntimeService.clearHistory(instanceCode);
        return Result.success();
    }
}



