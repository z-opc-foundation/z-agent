package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.mapper.AgentInstanceMapper;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.center.core.agent.widget.WidgetCallService;
import com.zifang.z.agent.center.web.api.response.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * FEATURE013 T2.2: Agent Widget 嵌入式 REST API
 *
 * <p>路径前缀 {@code /api/agent/widget}.
 */
@Tag(name = "Agent Widget 嵌入式")
@RestController
@RequestMapping("/api/agent/widget")
public class AgentWidgetController {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Resource
    private WidgetCallService widgetCallService;

    @Resource
    private AgentInstanceService instanceService;

    @Resource
    private AgentInstanceMapper agentInstanceMapper;

    /**
     * 生成指定字节长度的随机十六进制字符串.
     *
     * @param bytes 随机字节数
     * @return 大小写不敏感的十六进制字符串，长度为 {@code bytes * 2}
     */
    private static String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * Widget 调用（POST JSON）.
     * <p>优先从 {@code X-Widget-Key} Header 取 apiKey，否则从 body.apiKey 取。
     *
     * @param headerKey 请求头中的 Widget Key
     * @param body      请求体（可包含 apiKey / message / userId / userName）
     * @param origin    请求头中的 Origin（用于 CORS 校验）
     * @return 调用结果
     */
    @Operation(summary = "Widget 调用 (POST JSON)")
    @PostMapping("/call")
    public Result<WidgetCallResp> call(
            @RequestHeader(value = "X-Widget-Key", required = false) String headerKey,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            @RequestHeader(value = "Origin", required = false) String origin) {
        String apiKey = headerKey != null ? headerKey : (body != null ? String.valueOf(body.get("apiKey")) : null);
        return Result.success(doCall(apiKey, body, origin));
    }

    /**
     * Widget 调用（GET 形式）.
     *
     * @param apiKey  Widget API Key
     * @param message 用户消息
     * @param origin  请求头中的 Origin（用于 CORS 校验）
     * @return 调用结果
     */
    @Operation(summary = "Widget 调用 (GET)")
    @GetMapping("/call/{apiKey}")
    public Result<WidgetCallResp> callGet(
            @PathVariable String apiKey,
            @RequestParam String message,
            @RequestHeader(value = "Origin", required = false) String origin) {
        java.util.Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        return Result.success(doCall(apiKey, body, origin));
    }

    /**
     * Widget 实际调用逻辑（鉴权 + 限流 + 计时 + 错误处理）.
     *
     * @param apiKey Widget API Key
     * @param body   请求体（可包含 message / userId / userName）
     * @param origin 请求头中的 Origin
     * @return 调用结果
     */
    private WidgetCallResp doCall(String apiKey, java.util.Map<String, Object> body, String origin) {
        WidgetCallResp resp = new WidgetCallResp();
        try {
            String message = body != null ? String.valueOf(body.get("message")) : null;
            String userId = body != null && body.get("userId") != null ? String.valueOf(body.get("userId")) : null;
            String userName = body != null && body.get("userName") != null ? String.valueOf(body.get("userName")) : null;
            if (apiKey == null || apiKey.trim().isEmpty()) {
                resp.setOk(false);
                resp.setError("apiKey 缺失");
                return resp;
            }
            if (message == null || message.trim().isEmpty()) {
                resp.setOk(false);
                resp.setError("message 缺失");
                return resp;
            }
            AgentInstance instance = widgetCallService.authenticate(apiKey, origin);
            if (!widgetCallService.checkRateLimit(instance)) {
                resp.setOk(false);
                resp.setError("请求频率超限");
                resp.setRateLimit(instance.getWidgetRateLimit());
                return resp;
            }
            long start = System.currentTimeMillis();
            String answer = widgetCallService.invoke(instance, message, userId, userName);
            resp.setOk(true);
            resp.setAnswer(answer);
            resp.setElapsedMs(System.currentTimeMillis() - start);
            resp.setInstanceCode(instance.getInstanceCode());
            resp.setInstanceName(instance.getInstanceName());
        } catch (Exception e) {
            resp.setOk(false);
            resp.setError(e.getMessage());
        }
        return resp;
    }

    /**
     * 为指定实例生成 Widget API Key.
     *
     * @param instanceCode Agent 实例编码
     * @return 包含 widgetApiKey / widgetEnabled / instanceCode 的响应
     */
    @Operation(summary = "生成 Widget API Key")
    @PostMapping("/key/generate")
    public Result<WidgetKeyGenerateResp> generateKey(@RequestParam String instanceCode) {
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            throw new IllegalArgumentException("实例不存在: " + instanceCode);
        }

        String key = "wkey_" + randomHex(32);
        instance.setWidgetApiKey(key);
        instance.setWidgetEnabled(instance.getWidgetEnabled() == null ? 0 : instance.getWidgetEnabled());
        instance.setGmtModified(LocalDateTime.now());
        agentInstanceMapper.updateById(instance);
        WidgetKeyGenerateResp resp = new WidgetKeyGenerateResp();
        resp.setWidgetApiKey(key);
        resp.setWidgetEnabled(instance.getWidgetEnabled());
        resp.setInstanceCode(instanceCode);
        return Result.success(resp);
    }

    /**
     * 启用或禁用 Widget.
     *
     * @param instanceCode Agent 实例编码
     * @param enabled      是否启用
     * @return 包含 widgetEnabled / instanceCode 的响应
     */
    @Operation(summary = "启用/禁用 Widget")
    @PostMapping("/key/toggle")
    public Result<WidgetToggleResp> toggleWidget(@RequestParam String instanceCode, @RequestParam Boolean enabled) {
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            throw new IllegalArgumentException("实例不存在");
        }

        instance.setWidgetEnabled(enabled ? 1 : 0);
        instance.setGmtModified(LocalDateTime.now());
        agentInstanceMapper.updateById(instance);
        WidgetToggleResp resp = new WidgetToggleResp();
        resp.setWidgetEnabled(instance.getWidgetEnabled());
        resp.setInstanceCode(instanceCode);
        return Result.success(resp);
    }

    /**
     * 配置 CORS 白名单与限流值.
     *
     * @param instanceCode Agent 实例编码
     * @param cors         CORS 白名单（多个 Origin 用逗号分隔），可为空表示不修改
     * @param rateLimit    每实例的请求频率上限（次/分钟），可为空表示不修改
     * @return 包含 widgetCors / widgetRateLimit / instanceCode 的响应
     */
    @Operation(summary = "配置 CORS + 限流")
    @PostMapping("/key/config")
    public Result<WidgetConfigResp> configWidget(@RequestParam String instanceCode,
                                                 @RequestParam(required = false) String cors,
                                                 @RequestParam(required = false) Integer rateLimit) {
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            throw new IllegalArgumentException("实例不存在");
        }

        if (cors != null) {
            instance.setWidgetCors(cors);
        }

        if (rateLimit != null) {
            instance.setWidgetRateLimit(rateLimit);
        }

        instance.setGmtModified(LocalDateTime.now());
        agentInstanceMapper.updateById(instance);
        WidgetConfigResp resp = new WidgetConfigResp();
        resp.setWidgetCors(instance.getWidgetCors());
        resp.setWidgetRateLimit(instance.getWidgetRateLimit());
        resp.setInstanceCode(instanceCode);
        return Result.success(resp);
    }

    /**
     * 获取 Widget 配置（含脱敏后的 API Key）.
     *
     * @param instanceCode Agent 实例编码
     * @return 包含 widgetEnabled / widgetCors / widgetRateLimit / widgetEmbedCount / widgetApiKeyMasked 的响应
     */
    @Operation(summary = "获取 Widget 配置")
    @GetMapping("/key")
    public Result<WidgetConfigDetailResp> getWidgetConfig(@RequestParam String instanceCode) {
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            throw new IllegalArgumentException("实例不存在");
        }

        WidgetConfigDetailResp resp = new WidgetConfigDetailResp();
        resp.setInstanceCode(instanceCode);
        resp.setWidgetEnabled(instance.getWidgetEnabled());
        resp.setWidgetCors(instance.getWidgetCors());
        resp.setWidgetRateLimit(instance.getWidgetRateLimit());
        resp.setWidgetEmbedCount(instance.getWidgetEmbedCount());
        String key = instance.getWidgetApiKey();
        resp.setWidgetApiKeyMasked(key != null && key.length() > 4 ? "****" + key.substring(key.length() - 4) : key);
        return Result.success(resp);
    }

    /**
     * 健康检查端点.
     *
     * @return 固定字符串，表示 Widget API 健康
     */
    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("Agent Widget API is healthy");
    }
}
