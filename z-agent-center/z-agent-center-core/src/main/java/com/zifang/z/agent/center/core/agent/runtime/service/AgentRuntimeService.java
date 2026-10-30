package com.zifang.z.agent.center.core.agent.runtime.service;

import com.zifang.util.json.JsonCursor;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.agent.center.core.agent.conversation.dto.ChatDto;
import com.zifang.z.agent.center.core.agent.conversation.entity.AgentConversation;
import com.zifang.z.agent.center.core.agent.conversation.service.AgentConversationService;
import com.zifang.z.agent.center.core.agent.executor.AgentExecutor;
import com.zifang.z.agent.center.core.agent.executor.AgentExecutorFactory;
import com.zifang.z.agent.center.core.agent.executor.ReActAgentFactory;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.center.core.agent.memory.service.AgentMemoryService;
import com.zifang.z.agent.center.core.app.service.AgentAppService;
import com.zifang.z.agent.common.entity.AgentApp;
import com.zifang.z.agent.common.entity.AgentAppVersion;
import com.zifang.z.agent.engine.agent.observe.AgentTracer;
import com.zifang.z.llm.gateway.api.dto.UnifiedMessage;
import com.zifang.z.llm.gateway.api.dto.UnifiedRequest;
import com.zifang.z.llm.gateway.api.dto.UnifiedResponse;

import com.zifang.z.llm.gateway.core.service.ChatGatewayService;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentRuntimeService {

    private static final Logger log = LogManager.getLogger(AgentRuntimeService.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS).readTimeout(120, java.util.concurrent.TimeUnit.SECONDS).build();
    // 内存缓存会话上下文（生产环境应使用Redis）
    private final Map<String, List<ChatMessage>> conversationHistoryMap = new ConcurrentHashMap<>();
    @Resource
    private AgentAppService agentAppService;
    @Resource
    private AgentInstanceService instanceService;
    @Resource
    private AgentConversationService conversationService;
    // Agent 执行器工厂 (支持 IN_PROCESS 和 CONTAINER 模式)
    @Resource
    private AgentExecutorFactory agentExecutorFactory;
    // FEATURE013 T4: AgentTracer (MysqlAgentTracer 自动注入, 落库到 z_agent_trace / z_agent_span)
    @Autowired(required = false)
    private AgentTracer agentTracer;
    // FEATURE013 T2.3: Agent 长期记忆 (注入到 system prompt)
    @Autowired(required = false)
    private AgentMemoryService agentMemoryService;
    // LLM 网关服务 (替代硬编码 Ollama)
    @Autowired(required = false)
    /**
     * FEATURE066: z-llm-core 0.1.2 ChatGatewayService — 同步 chat, 内部按 request.getModel() 路由到对应 LLM provider.
     */
    private ChatGatewayService chatGatewayService;

    /**
     * 从App加载版本配置（temperature/topP等）
     */
    private AgentAppVersion loadVersionConfig(String appCode) {
        try {
            List<AgentAppVersion> versions = agentAppService.listVersions(appCode);
            if (versions != null && !versions.isEmpty()) {
                return versions.get(0); // 取最新版本
            }
        } catch (Exception e) {
            log.warn("加载App版本配置失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取带默认值的LLM参数
     */
    private Map<String, Object> buildLlmOptions(AgentAppVersion ver) {
        Map<String, Object> opts = new HashMap<>();
        opts.put("temperature", ver != null && ver.getTemperature() != null ? ver.getTemperature() : 0.7f);
        opts.put("top_p", ver != null && ver.getTopP() != null ? ver.getTopP() : 0.9f);
        if (ver != null && ver.getMaxTokens() != null) {
            opts.put("num_predict", ver.getMaxTokens());
        }
        return opts;
    }

    /**
     * 对话入口 - 返回DTO
     */
    public ChatDto chatResp(String instanceCode, String userMessage, String userId, String userName) {
        return toDto(chat(instanceCode, userMessage, userId, userName));
    }

    /**
     * 对话入口 - 返回Entity
     */
    public AgentConversation chat(String instanceCode, String userMessage, String userId, String userName) {
        long startTime = System.currentTimeMillis();

        // FEATURE013 T4: 开始 Trace (落库到 z_agent_trace)
        String sessionId = instanceCode + "_" + userId;
        if (agentTracer != null) {
            try {
                agentTracer.beginTrace(sessionId, instanceCode, userMessage);
            } catch (Exception ignore) {
            }
        }

        // 1. 获取实例和应用配置
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null || !"ACTIVE".equals(instance.getStatus())) {
            AgentConversation err = buildErrorConversation(instanceCode, userId, userName, userMessage, "实例不存在或已停用", startTime);
            if (agentTracer != null) {
                try {
                    agentTracer.endTrace(null, "FAILURE");
                } catch (Exception ignore) {
                }
            }
            return err;
        }

        // 1.1 根据运行模式选择执行器
        String runMode = instance.getRunMode();
        if ("CONTAINER".equals(runMode)) {
            // CONTAINER 模式: 调用容器代理执行
            AgentConversation result = executeViaContainer(instanceCode, userId, userName, userMessage, startTime);
            if (agentTracer != null) {
                try {
                    agentTracer.endTrace(null, "SUCCESS".equals(result.getStatus()) ? "SUCCESS" : "FAILURE");
                } catch (Exception ignore) {
                }
            }
            return result;
        }

        if ("IN_PROCESS".equals(runMode)) {
            // IN_PROCESS 模式: 使用 ReAct Agent 在内存中执行
            AgentConversation result = executeInProcess(instanceCode, userId, userName, userMessage, startTime);
            if (agentTracer != null) {
                try {
                    agentTracer.endTrace(null, "SUCCESS".equals(result.getStatus()) ? "SUCCESS" : "FAILURE");
                } catch (Exception ignore) {
                }
            }
            return result;
        }

        // 默认: 兼容旧逻辑，直接调用 LLM (需要 app 和 ver)
        AgentApp app = agentAppService.getByAppCode(instance.getAppCode());
        if (app == null) {
            if (agentTracer != null) {
                try {
                    agentTracer.endTrace(null, "FAILURE");
                } catch (Exception ignore) {
                }
            }
            return buildErrorConversation(instanceCode, userId, userName, userMessage, "应用不存在", startTime);
        }
        AgentAppVersion ver = loadVersionConfig(app.getAppCode());
        AgentConversation result = chatViaLlm(instance, app, userMessage, userId, userName, startTime, ver);
        // FEATURE013 T4: 结束 Trace
        if (agentTracer != null) {
            try {
                agentTracer.endTrace(null, "SUCCESS".equals(result.getStatus()) ? "SUCCESS" : "FAILURE");
            } catch (Exception ignore) {
            }
        }
        return result;
    }

    /**
     * IN_PROCESS 模式: 使用 ReAct Agent 在内存中执行
     */
    private AgentConversation executeInProcess(String instanceCode, String userId, String userName,
                                               String userMessage, long startTime) {
        try {
            // 通过工厂获取 ReAct Agent 并执行
            ReActAgentFactory.ReActAgent agent = agentExecutorFactory.getReActAgentFactory().getAgent(instanceCode);
            String reply = agent.execute(userMessage);

            AgentConversation conversation = new AgentConversation();
            conversation.setConversationCode(instanceCode + "_" + System.currentTimeMillis() / 60000);
            conversation.setInstanceCode(instanceCode);
            conversation.setUserId(userId);
            conversation.setUserName(userName);
            conversation.setUserMessage(userMessage);
            conversation.setAssistantMessage(reply);
            conversation.setLatencyMs((int) (System.currentTimeMillis() - startTime));
            conversation.setStatus("SUCCESS");
            conversationService.saveEntity(conversation);
            return conversation;
        } catch (Exception e) {
            log.error("InProcess agent execution failed", e);
            return buildErrorConversation(instanceCode, userId, userName, userMessage,
                    "ReAct执行失败: " + e.getMessage(), startTime);
        }
    }

    /**
     * 兼容旧逻辑: 直接调用 LLM (非 ReAct)
     */
    private AgentConversation chatViaLlm(AgentInstance instance, AgentApp app, String userMessage,
                                         String userId, String userName, long startTime, AgentAppVersion ver) {
        String instanceCode = instance.getInstanceCode();
        // 1. 记录访问
        instanceService.recordVisit(instanceCode);

        // 2. 获取历史消息
        String conversationCode = instance.getInstanceCode() + "_" + System.currentTimeMillis() / 60000;
        List<ChatMessage> history = conversationHistoryMap.computeIfAbsent(conversationCode, k -> new ArrayList<>());

        // 添加系统prompt
        if (history.isEmpty() && StringUtils.isNotEmpty(app.getPrompt())) {
            String systemPrompt = buildSystemPrompt(app, ver, userId, userMessage);
            history.add(new ChatMessage("system", systemPrompt));
        }

        // 添加用户消息
        history.add(new ChatMessage("user", userMessage));

        // 记忆管理：裁剪历史窗口
        int windowSize = (ver != null && ver.getHistoryWindowSize() != null) ? ver.getHistoryWindowSize() : 50;
        trimHistory(history, windowSize);

        // 调用LLM
        String assistantReply;
        try {
            assistantReply = callLlm(app.getModelProvider(), app.getModelName(), history, ver);
        } catch (Exception e) {
            log.error("LLM调用失败", e);
            assistantReply = "抱歉，AI服务暂时不可用：" + e.getMessage();
        }

        // 记录助手回复
        history.add(new ChatMessage("assistant", assistantReply));

        // 保持历史不超过50条
        while (history.size() > 50) {
            history.remove(1);
        }

        // 保存会话记录
        AgentConversation conversation = new AgentConversation();
        conversation.setConversationCode(conversationCode);
        conversation.setInstanceCode(instance.getInstanceCode());
        conversation.setUserId(userId);
        conversation.setUserName(userName);
        conversation.setUserMessage(userMessage);
        conversation.setAssistantMessage(assistantReply);
        conversation.setModelName(app.getModelName());
        conversation.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        conversation.setStatus("SUCCESS");
        conversationService.saveEntity(conversation);

        return conversation;
    }

    /**
     * // 5. 记录助手回复
     * 流式对话入口
     */
    public void chatStream(String instanceCode, String userMessage, String userId, String userName, HttpServletResponse response) throws IOException {
        long startTime = System.currentTimeMillis();

        // 1. 获取实例和应用配置
        AgentInstance instance = instanceService.getByInstanceCode(instanceCode);
        if (instance == null || !"ACTIVE".equals(instance.getStatus())) {
            writeSseError(response, "实例不存在或已停用");
            return;
        }

        AgentApp app = agentAppService.getByAppCode(instance.getAppCode());
        if (app == null) {
            writeSseError(response, "应用不存在");
            return;
        }

        // 1.5 加载版本配置（temperature/topP等）
        AgentAppVersion ver = loadVersionConfig(app.getAppCode());

        // 2. 记录访问
        instanceService.recordVisit(instanceCode);

        // 3. 获取历史消息
        String conversationCode = instanceCode + "_" + System.currentTimeMillis() / 60000;
        List<ChatMessage> history = conversationHistoryMap.computeIfAbsent(conversationCode, k -> new ArrayList<>());

        // 添加系统prompt
        if (history.isEmpty() && StringUtils.isNotEmpty(app.getPrompt())) {
            // 构建增强的系统提示词（包含工具、技能、知识库配置）
            String systemPrompt = buildSystemPrompt(app, ver, userId, userMessage);
            history.add(new ChatMessage("system", systemPrompt));
        }

        // 添加用户消息
        history.add(new ChatMessage("user", userMessage));

        // 3.5 记忆管理：裁剪历史窗口
        int windowSize = (ver != null && ver.getHistoryWindowSize() != null) ? ver.getHistoryWindowSize() : 50;
        trimHistory(history, windowSize);

        // 4. 流式调用LLM
        response.setContentType("text/event-stream;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        StringBuilder fullReply = new StringBuilder();
        try {
            callLlmStream(app.getModelProvider(), app.getModelName(), history, response, fullReply, ver);
        } catch (Exception e) {
            log.error("LLM流式调用失败", e);
            writeSseError(response, "AI服务暂时不可用：" + e.getMessage());
            return;
        }

        // 5. 保存助手回复到历史
        history.add(new ChatMessage("assistant", fullReply.toString()));

        // 6. 保存会话记录
        AgentConversation conversation = new AgentConversation();
        conversation.setConversationCode(conversationCode);
        conversation.setInstanceCode(instanceCode);
        conversation.setUserId(userId);
        conversation.setUserName(userName);
        conversation.setUserMessage(userMessage);
        conversation.setAssistantMessage(fullReply.toString());
        conversation.setModelName(app.getModelName());
        conversation.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        conversation.setStatus("SUCCESS");
        conversationService.saveEntity(conversation);
    }

    private void writeSseError(HttpServletResponse response, String errorMsg) throws IOException {
        response.setContentType("text/event-stream;charset=utf-8");
        response.getWriter().write("data: " + errorMsg + "\n\n");
        response.getWriter().flush();
    }

    /**
     * 流式调用LLM (SSE)
     */
    private void callLlmStream(String provider, String modelName, List<ChatMessage> messages,
                               HttpServletResponse response, StringBuilder fullReply,
                               AgentAppVersion ver) throws IOException {
        // FEATURE066: 流式也必须优先走大模型网关（与同步路径 callLlm 一致）。
        // 原实现无条件直连 OLLAMA_BASE_URL(=localhost:11434)，容器里必然连不上，
        // 且完全忽略应用的 provider/model 配置 ⇒ 所有 Agent 的就地聊天都是死路。
        if (chatGatewayService != null) {
            try {
                UnifiedRequest req = new UnifiedRequest();
                List<UnifiedMessage> reqMessages = new ArrayList<>();
                for (ChatMessage msg : messages) {
                    UnifiedMessage m = new UnifiedMessage();
                    m.setRole(msg.role);
                    m.setContent(msg.content);
                    reqMessages.add(m);
                }
                req.setMessages(reqMessages);
                req.setModel(StringUtils.defaultIfEmpty(provider, "minimax") + "/"
                        + StringUtils.defaultIfEmpty(modelName, "minimax2.7"));
                if (ver != null && ver.getTemperature() != null) {
                    req.setTemperature(ver.getTemperature().doubleValue());
                }
                if (ver != null && ver.getTopP() != null) {
                    req.setTopP(ver.getTopP().doubleValue());
                }
                if (ver != null && ver.getMaxTokens() != null) {
                    req.setMaxTokens(ver.getMaxTokens());
                }
                response.setContentType("text/event-stream;charset=utf-8");
                final Throwable[] gatewayError = {null};
                final long[] chunkCount = {0};
                ChatGatewayService.StreamOutcome outcome = chatGatewayService.streamChat(req,
                        chunk -> {
                            try {
                                chunkCount[0]++;
                                String content = extractChunkContent(chunk);
                                if (content != null && !content.isEmpty()) {
                                    fullReply.append(content);
                                    response.getWriter().write("data: " + content + "\n\n");
                                    response.getWriter().flush();
                                }
                            } catch (IOException ignore) {
                                // 客户端断开：忽略，结束语统一在外层补
                            }
                        },
                        err -> gatewayError[0] = err,
                        () -> {
                        });
                if (gatewayError[0] == null && fullReply.length() > 0) {
                    response.getWriter().write("data: [DONE]\n\n");
                    response.getWriter().flush();
                    return;
                }
                // 网关异步失败只会走 errorConsumer, 不抛异常; 不打出来的话线上只能看到
                // "回退 Ollama" 而永远拿不到真因(凭据/模型/上游), 故必须落日志。
                log.warn("网关流式调用未产出内容, 回退 Ollama 直连: model={}, chunks={}, err={}",
                        req.getModel(),
                        outcome == null ? "n/a" : outcome.chunks() + "/" + chunkCount[0],
                        gatewayError[0] == null ? "none" : gatewayError[0].getClass().getSimpleName() + ": " + gatewayError[0].getMessage());
            } catch (Exception e) {
                log.warn("网关流式调用失败, 回退 Ollama 直连: {}", e.getMessage());
            }
        }

        String actualModel = StringUtils.defaultIfEmpty(modelName, "qwen2.5:7b");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", actualModel);

        List<Map<String, String>> ollamaMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.role);
            m.put("content", msg.content);
            ollamaMessages.add(m);
        }
        requestBody.put("messages", ollamaMessages);
        requestBody.put("stream", true);

        // 应用模型参数配置
        Map<String, Object> opts = buildLlmOptions(ver);
        requestBody.put("options", opts);

        RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(requestBody), JSON);
        Request request = new Request.Builder()
                .url(OLLAMA_BASE_URL + "/api/chat")
                .post(body)
                .build();

        try (Response resp = httpClient.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("LLM请求失败: " + resp);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body().byteStream(), java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                // Ollama returns plain JSON lines (not SSE format with "data: " prefix)
                // Each line is a JSON object like: {"model":"...","message":{"content":"..."},"done":false}
                if (line.startsWith("{")) {
                    try {
                        JsonCursor node = JsonCursor.of(JsonUtil.parse(line));
                        String content = node.path("message").path("content").asText();
                        if (!content.isEmpty()) {
                            fullReply.append(content);
                            response.getWriter().write("data: " + content + "\n\n");
                            response.getWriter().flush();
                        }
                        if (node.path("done").asBoolean(false)) {
                            break;
                        }
                    } catch (Exception e) {
                        // 忽略解析异常
                    }
                } else if (line.startsWith("data: ")) {
                    // Handle SSE format (if ever used)
                    String jsonStr = line.substring(6);
                    if ("[DONE]".equals(jsonStr)) {
                        break;
                    }
                }
            }
            response.getWriter().write("data: [DONE]\n\n");
            response.getWriter().flush();
        }
    }

    /**
     * 从网关流式分片中取文本内容（兼容 message.content 与 delta.content 两种承载）。
     */
    private String extractChunkContent(com.zifang.z.llm.gateway.api.dto.UnifiedStreamChunk chunk) {
        if (chunk == null || chunk.getChoices() == null) {
            return "";
        }
        for (com.zifang.z.llm.gateway.api.dto.Choice choice : chunk.getChoices()) {
            if (choice == null) {
                continue;
            }
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                return choice.getMessage().getContent();
            }
            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                return choice.getDelta().getContent();
            }
        }
        return "";
    }

    /**
     * 调用LLM
     */
    private String callLlm(String provider, String modelName, List<ChatMessage> messages, AgentAppVersion ver) throws IOException {
        String actualModel = StringUtils.defaultIfEmpty(modelName, "minimax2.7");
        String actualProvider = StringUtils.defaultIfEmpty(provider, "minimax");

        // FEATURE066: 优先使用 z-boot-llm-starter 0.1.2 的 ChatGatewayService (替代老 z-agent-llm-gateway LlmGatewayService)
        if (chatGatewayService != null) {
            try {
                List<UnifiedMessage> reqMessages = new ArrayList<>();
                for (ChatMessage msg : messages) {
                    UnifiedMessage m = new UnifiedMessage();
                    m.setRole(msg.role);
                    m.setContent(msg.content);
                    reqMessages.add(m);
                }
                UnifiedRequest req = new UnifiedRequest();
                req.setMessages(reqMessages);
                req.setModel(actualProvider + "/" + actualModel);
                if (ver != null && ver.getTemperature() != null) {
                    req.setTemperature(ver.getTemperature().doubleValue());
                }
                if (ver != null && ver.getMaxTokens() != null) {
                    req.setMaxTokens(ver.getMaxTokens());
                }

                UnifiedResponse resp = chatGatewayService.chat(req);
                if (resp.getChoices() != null && !resp.getChoices().isEmpty()
                        && resp.getChoices().get(0).getMessage() != null) {
                    String content = resp.getChoices().get(0).getMessage().getContent();
                    return content == null ? "" : content;
                }
                return "LLM调用无响应";
            } catch (Exception e) {
                log.error("ChatGatewayService调用失败, fallback到Ollama: {}", e.getMessage());
            }
        }

        // fallback: Ollama
        String baseUrl = OLLAMA_BASE_URL;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", actualModel);
        List<Map<String, String>> ollamaMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.role);
            m.put("content", msg.content);
            ollamaMessages.add(m);
        }
        requestBody.put("messages", ollamaMessages);
        requestBody.put("stream", false);
        Map<String, Object> opts = buildLlmOptions(ver);
        requestBody.put("options", opts);
        RequestBody body = RequestBody.create(JsonUtil.toJsonExcludingNulls(requestBody), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/chat")
                .post(body)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LLM请求失败: " + response);
            }
            String responseBody = response.body().string();
            JsonCursor jsonNode = JsonCursor.of(JsonUtil.parse(responseBody));
            return jsonNode.path("message").path("content").asText();
        }
    }

    /**
     * 清除会话历史
     */
    public void clearHistory(String instanceCode) {
        conversationHistoryMap.entrySet().removeIf(entry -> entry.getKey().startsWith(instanceCode));
    }

    /**
     * 构建增强的系统提示词
     * 将工具、技能、知识库、长期记忆配置注入到 system prompt
     *
     * @param userId      当前用户 ID (用于检索长期记忆)
     * @param userMessage 当前用户消息 (用于在记忆中做相关性检索)
     */
    private String buildSystemPrompt(AgentApp app, AgentAppVersion ver,
                                     String userId, String userMessage) {
        StringBuilder sb = new StringBuilder();

        // 基础 prompt
        if (StringUtils.isNotEmpty(app.getPrompt())) {
            sb.append(app.getPrompt()).append("\n\n");
        }

        // 注入工具配置
        if (StringUtils.isNotEmpty(app.getTools())) {
            try {
                sb.append("【工具配置】你可以使用以下工具：\n");
                sb.append(app.getTools()).append("\n");
            } catch (Exception e) {
            }
        }

        // 注入技能配置
        if (StringUtils.isNotEmpty(app.getSkillCodes())) {
            sb.append("【技能】你具备以下专业技能：\n");
            sb.append(app.getSkillCodes()).append("\n");
        }

        // 注入知识库配置
        if (StringUtils.isNotEmpty(app.getKnowledgeIds())) {
            sb.append("【知识库】你可以检索以下知识库获取信息：").append(app.getKnowledgeIds()).append("\n");
        }

        // 记忆提示词
        if (ver != null && StringUtils.isNotEmpty(ver.getMemoryPrompt())) {
            sb.append("【记忆规则】").append(ver.getMemoryPrompt()).append("\n");
        }

        // FEATURE013 T2.3: 长期记忆注入 (按 userId + appCode + 当前 query 检索)
        if (agentMemoryService != null && userId != null && !userId.isEmpty()) {
            try {
                String memoryFragment = agentMemoryService.injectForPrompt(
                        userId, app.getAppCode(), userMessage == null ? "" : userMessage);
                if (memoryFragment != null && !memoryFragment.isEmpty()) {
                    sb.append("\n").append(memoryFragment).append("\n");
                }
            } catch (Exception e) {
                log.warn("[AgentRuntime] 记忆注入失败 userId={}: {}", userId, e.getMessage());
            }
        }

        return sb.toString();
    }

    /**
     * 裁剪历史消息，保留最近 windowSize 条（不含 system prompt）
     */
    private void trimHistory(List<ChatMessage> history, int windowSize) {
        if (history.size() <= windowSize) {
            return;
        }

        // 保留第一条 system prompt，裁剪后面的
        if (history.size() > 0 && "system".equals(history.get(0).role)) {
            // 保留 system + 最近 windowSize-1 条
            while (history.size() > windowSize) {
                history.remove(1);
            }
        } else {
            while (history.size() > windowSize) {
                history.remove(0);
            }
        }
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private AgentConversation buildErrorConversation(String instanceCode, String userId, String userName,
                                                     String userMessage, String errorMsg, long startTime) {
        AgentConversation conversation = new AgentConversation();
        conversation.setConversationCode("error_" + RandomUtil.uuidShort(12));
        conversation.setInstanceCode(instanceCode);
        conversation.setUserId(userId);
        conversation.setUserName(userName);
        conversation.setUserMessage(userMessage);
        conversation.setAssistantMessage(errorMsg);
        conversation.setLatencyMs((int) (System.currentTimeMillis() - startTime));
        conversation.setStatus("FAILED");
        // error_msg 是窄列: 失败原因常带上游整段报文, 直接写会把 insert 顶成
        // MysqlDataTruncation, 于是"调用失败"变成"接口 500", 真相反而丢了。
        conversation.setErrorMsg(truncate(errorMsg, 200));
        conversationService.saveEntity(conversation);
        return conversation;
    }

    /**
     * CONTAINER 模式: 通过 HTTP 调用远程 Hermes Agent 容器
     */
    private AgentConversation executeViaContainer(String instanceCode, String userId, String userName,
                                                  String userMessage, long startTime) {
        try {
            AgentExecutor executor = agentExecutorFactory.getExecutor(instanceCode);
            String reply = executor.execute(instanceCode, userId, userName, userMessage);

            AgentConversation conversation = new AgentConversation();
            conversation.setConversationCode(instanceCode + "_" + System.currentTimeMillis() / 60000);
            conversation.setInstanceCode(instanceCode);
            conversation.setUserId(userId);
            conversation.setUserName(userName);
            conversation.setUserMessage(userMessage);
            conversation.setAssistantMessage(reply);
            conversation.setLatencyMs((int) (System.currentTimeMillis() - startTime));
            conversation.setStatus("SUCCESS");
            conversationService.saveEntity(conversation);
            return conversation;
        } catch (Exception e) {
            log.error("Container agent execution failed", e);
            return buildErrorConversation(instanceCode, userId, userName, userMessage,
                    "容器Agent执行失败: " + e.getMessage(), startTime);
        }
    }

    private ChatDto toDto(AgentConversation c) {
        if (c == null) {
            return null;
        }

        ChatDto dto = new ChatDto();
        BeanUtils.copyProperties(c, dto);
        dto.setGmtCreate(c.getGmtCreate() != null ? c.getGmtCreate().format(DF) : null);
        return dto;
    }

    /**
     * 简单消息对象
     */
    private static class ChatMessage {
        String role;
        String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
