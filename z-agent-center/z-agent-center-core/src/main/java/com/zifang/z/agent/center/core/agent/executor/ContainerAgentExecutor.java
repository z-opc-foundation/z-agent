package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 容器模式 Agent 执行器
 * 通过 HTTP 调用远程 Hermes Agent 容器
 */
@Component
public class ContainerAgentExecutor implements AgentExecutor {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 60;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();

    @Resource
    private AgentInstanceService agentInstanceService;

    @Override
    public String execute(String instanceCode, String userId, String userName, String message) {
        // 1. 获取实例信息
        AgentInstance instance = agentInstanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            throw new RuntimeException("Agent instance not found: " + instanceCode);
        }

        // 2. 检查运行模式
        if (!"CONTAINER".equals(instance.getRunMode())) {
            throw new RuntimeException("Instance is not in CONTAINER mode: " + instanceCode);
        }

        // 3. 获取容器端口
        Integer containerPort = instance.getContainerPort();
        if (containerPort == null) {
            throw new RuntimeException("Container port not configured: " + instanceCode);
        }

        // 4. 构建请求 URL (容器内Hermes API)
        String url = "http://localhost:" + containerPort + "/api/agent/chat";

        // 5. 构建请求体
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("instanceCode", instanceCode);
            requestBody.put("userId", userId);
            requestBody.put("userName", userName);
            requestBody.put("message", message);

            RequestBody body = RequestBody.create(JsonUtil.toJson(requestBody), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Object plain = JsonUtil.toPlainValue(JsonUtil.parse(responseBody));
                    if (!(plain instanceof Map)) {
                        throw new RuntimeException("Unexpected Hermes agent response shape: " + responseBody);
                    }
                    return (String) ((Map<?, ?>) plain).get("data");
                } else {
                    throw new RuntimeException("Hermes agent error: " + response.code() + " - " + response.body().string());
                }
            }
        } catch (IOException | RuntimeException e) {
            // JsonUtil 抛的是 RuntimeException（Jackson 时代这里是 JsonProcessingException），同语义一并归一
            throw new RuntimeException("Failed to execute agent: " + e.getMessage(), e);
        }
    }

    @Override
    public String getType() {
        return "CONTAINER";
    }

    @Override
    public boolean isHealthy() {
        return true; // 容器健康由外部监控
    }
}