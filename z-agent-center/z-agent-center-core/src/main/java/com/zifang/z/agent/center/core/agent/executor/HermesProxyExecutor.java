package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.util.json.JsonUtil;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Hermes 远程代理执行器
 * 通过 HTTP 调用远端本地 Hermes Agent 容器
 * <p>
 * CONTAINER 模式: Agent 运行在独立的 Docker 容器中 (Hermes Agent)
 * 通过 REST API 与容器通信
 */
public class HermesProxyExecutor implements AgentExecutor {

    private static final Logger log = LogManager.getLogger(HermesProxyExecutor.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 120;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();


    // 容器端口 -> Agent 实例编码
    private final Map<Integer, String> portToInstanceMap = new HashMap<Integer, String>();

    @Resource
    private AgentContainerService containerService;

    public HermesProxyExecutor() {
    }

    public HermesProxyExecutor(AgentContainerService containerService) {
        this.containerService = containerService;
    }

    @Override
    public String execute(String instanceCode, String userId, String userName, String message) {
        // 1. 获取实例的容器端口
        Integer port = getContainerPort(instanceCode);
        if (port == null) {
            throw new RuntimeException("Agent container not found for instance: " + instanceCode);
        }

        // 2. 调用 Hermes 容器 API
        String url = "http://localhost:" + port + "/api/agent/chat";

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<String, Object>();
            requestBody.put("instanceCode", instanceCode);
            requestBody.put("userId", userId);
            requestBody.put("userName", userName);
            requestBody.put("message", message);

            RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(requestBody), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Map<String, Object> respMap = toPlainMap(responseBody);
                    Object data = respMap.get("data");
                    if (data != null) {
                        return data.toString();
                    }
                    return (String) respMap.get("message");
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Hermes container error: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("Hermes error: " + response.code() + " - " + errorBody);
                }
            }
        } catch (IOException e) {
            log.error("Failed to execute via Hermes proxy", e);
            throw new RuntimeException("Hermes proxy execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getType() {
        return "CONTAINER";
    }

    @Override
    public boolean isHealthy() {
        // 检查 Docker 是否可用
        return containerService != null && containerService.isDockerAvailable();
    }

    /**
     * 获取容器端口
     */
    private Integer getContainerPort(String instanceCode) {
        // 从缓存中查找
        for (Map.Entry<Integer, String> entry : portToInstanceMap.entrySet()) {
            if (entry.getValue().equals(instanceCode)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 注册容器端口映射
     */
    public void registerContainer(String instanceCode, int port) {
        portToInstanceMap.put(port, instanceCode);
        log.info("Registered Hermes container: {} -> port {}", instanceCode, port);
    }

    /**
     * 注销容器
     */
    public void unregisterContainer(String instanceCode) {
        Integer portToRemove = null;
        for (Map.Entry<Integer, String> entry : portToInstanceMap.entrySet()) {
            if (entry.getValue().equals(instanceCode)) {
                portToRemove = entry.getKey();
                break;
            }
        }
        if (portToRemove != null) {
            portToInstanceMap.remove(portToRemove);
        }
        log.info("Unregistered Hermes container: {}", instanceCode);
    }

    /**
     * 创建容器并注册
     */
    public Integer createAndRegisterContainer(String instanceCode) {
        Integer port = containerService.createContainer(instanceCode);
        if (port != null) {
            registerContainer(instanceCode, port);
        }
        return port;
    }

    /**
     * 销毁容器并注销
     */
    public void destroyAndUnregisterContainer(String instanceCode) {
        unregisterContainer(instanceCode);
        containerService.destroyContainer(instanceCode);
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
