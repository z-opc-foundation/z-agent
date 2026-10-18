package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.common.enums.AgentType;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 本地进程 Agent 代理执行器 — FEATURE014
 *
 * <p>统一的本地进程 HTTP 代理实现，通过 {@link LocalProcessAgentRuntime} 维护进程映射。
 * Day 1 先用这一个类支撑 CLAUDE_CODE / OPENCODE / HERMES_LOCAL / CUSTOM 四种类型；
 * Day 2/3 会按 agentType 拆出 {@code ClaudeCodeExecutor} / {@code OpenCodeExecutor} /
 * {@code HermesLocalExecutor} 三个子类以便做命令参数 / 健康检查协议 / skill 路径的特化。</p>
 *
 * <p>本地进程 Agent 的约定接口：</p>
 * <ul>
 *   <li>{@code POST /api/agent/chat}  — body: {instanceCode, userId, userName, message}, resp: {code, data}</li>
 *   <li>{@code GET /health}            — 200 即视为 healthy</li>
 *   <li>{@code POST /api/skill/push}   — body: {skillCode, content}, 用于接收 skill 推送（FEATURE014 §4.2）</li>
 *   <li>{@code GET /api/skill/list}    — 返回本地已安装 skill 列表</li>
 * </ul>
 */
public class LocalProcessProxyExecutor implements AgentExecutor {

    private static final Logger log = LogManager.getLogger(LocalProcessProxyExecutor.class);

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 120;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();


    private final LocalProcessAgentRuntime runtime;
    private final AgentType agentType;

    /**
     * instanceCode → port + 可选 callbackUrl
     */
    private final Map<String, PortBinding> portBindings = new ConcurrentHashMap<>();

    public LocalProcessProxyExecutor(LocalProcessAgentRuntime runtime, AgentType agentType) {
        this.runtime = runtime;
        this.agentType = agentType;
    }

    @Override
    public String execute(String instanceCode, String userId, String userName, String message) {
        PortBinding binding = portBindings.get(instanceCode);
        if (binding == null) {
            throw new RuntimeException("No local process registered for instance: " + instanceCode
                    + " (agentType=" + agentType + ")");
        }
        String url = "http://localhost:" + binding.port + "/api/agent/chat";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("instanceCode", instanceCode);
            body.put("userId", userId);
            body.put("userName", userName);
            body.put("message", message);
            // 顺便告诉被调用方 center 的回调地址，方便它反向注册 / 上报
            body.put("centerCallbackUrl", "http://localhost:8888");

            RequestBody reqBody = RequestBody.create(JsonUtil.toJsonExcludingNulls(body), JSON);
            Request request = new Request.Builder().url(url).post(reqBody).build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String respBody = response.body() != null ? response.body().string() : "{}";
                    Map<String, Object> respMap = toPlainMap(respBody);
                    Object data = respMap.get("data");
                    if (data != null) {
                        return data.toString();
                    }

                    return String.valueOf(respMap.getOrDefault("message", ""));
                }
                String errBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Local agent {} error: {} - {}", agentType, response.code(), errBody);
                throw new RuntimeException("Local agent error: " + response.code() + " - " + errBody);
            }
        } catch (IOException e) {
            log.error("Local agent proxy call failed: instance={} agentType={}", instanceCode, agentType, e);
            throw new RuntimeException("Local agent execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getType() {
        return agentType.name();
    }

    @Override
    public boolean isHealthy() {
        if (runtime == null) {
            return false;
        }

        // 聚合所有已注册实例的健康状态
        boolean anyRegistered = false;
        for (String code : portBindings.keySet()) {
            anyRegistered = true;
            if (!runtime.isHealthy(code)) {
                return false;
            }
        }
        return anyRegistered || runtime.isCliAvailable(agentType);
    }

    /**
     * 注册一个 instanceCode → port 的映射（供 AgentExecutorFactory 调用）。
     */
    public void registerProcess(String instanceCode, int port, String callbackUrl) {
        portBindings.put(instanceCode, new PortBinding(port, callbackUrl));
        log.info("Registered local process: instanceCode={} agentType={} port={}",
                instanceCode, agentType, port);
    }

    /**
     * 注销。
     */
    public void unregisterProcess(String instanceCode) {
        PortBinding removed = portBindings.remove(instanceCode);
        if (removed != null) {
            log.info("Unregistered local process: instanceCode={} port={}", instanceCode, removed.port);
        }
    }

    /**
     * 列出已注册的所有 instanceCode（用于健康检查 / 监控）。
     */
    public java.util.Set<String> listInstances() {
        return portBindings.keySet();
    }

    /**
     * 内部数据结构：port + 可选 callbackUrl。
     */
    private static class PortBinding {
        final int port;
        final String callbackUrl;

        PortBinding(int port, String callbackUrl) {
            this.port = port;
            this.callbackUrl = callbackUrl;
        }
    }

    /**
     * JSON 字符串 → 深度摊平的 {@code Map<String,Object>}。
     * <p>
     * 必须过 {@code JsonUtil.toPlainValue}：{@code parseToMap} 只摊平第一层，
     * 嵌套节点仍是 {@code JsonObject}/{@code JsonArray}，下游按 {@code List}/{@code Map}
     * 强转会 ClassCastException。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toPlainMap(String json) {
        return (Map<String, Object>) JsonUtil.toPlainValue(JsonUtil.parse(json));
    }
}
