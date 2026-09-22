package com.zifang.z.agent.core.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.agent.api.exception.AgentException;
import com.zifang.z.agent.core.properties.AgentProperties;
import com.zifang.z.agent.kernel.agent.Agent;
import com.zifang.z.agent.kernel.agent.AgentRequest;
import com.zifang.z.agent.kernel.agent.AgentResponse;
import com.zifang.z.agent.kernel.llm.ChatCompletionsRequest;
import com.zifang.z.agent.kernel.llm.ChatCompletionsResponse;
import com.zifang.z.agent.kernel.llm.LlmProvider;
import com.zifang.z.agent.kernel.message.Msg;
import com.zifang.z.agent.kernel.message.ToolCall;
import com.zifang.z.agent.kernel.tool.Tool;
import com.zifang.z.agent.kernel.tool.ToolResult;
import com.zifang.z.agent.kernel.types.MessageRole;
import com.zifang.z.agent.kernel.types.TokenUsage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM 提供方路由器. 把 model id 路由到对应 LlmProvider bean.
 *
 * <p>registerProvider(name, provider): 业务方注入 kernel.llm 6 provider 默认实现.
 * <p>supportsModel(model): 遍历所有 provider, 命中即返回.
 * <p>chat / streamChat: 转给对应 provider.
 */
public class LlmAdapter {

    private final AgentProperties props;
    private final Map<String, LlmProvider> providers = new ConcurrentHashMap<String, LlmProvider>();
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmAdapter(AgentProperties props) {
        this.props = props;
    }

    public void registerProvider(LlmProvider provider) {
        if (provider == null) return;
        providers.put(provider.name(), provider);
    }

    public void registerProvider(String name, LlmProvider provider) {
        if (provider == null) return;
        providers.put(name, provider);
    }

    public LlmProvider resolve(String modelId) {
        if (modelId == null) modelId = props.getDefaultModel();
        for (LlmProvider p : providers.values()) {
            if (p.supportsModel(modelId)) return p;
        }
        throw new AgentException(AgentException.RUN_FAILED,
                "no provider supports model: " + modelId);
    }

    public ChatCompletionsResponse chat(ChatCompletionsRequest req) {
        LlmProvider p = resolve(req.getModel());
        return p.chat(req);
    }

    public void streamChat(ChatCompletionsRequest req,
                           java.util.function.Consumer<ChatCompletionsResponse> onChunk,
                           java.util.function.Consumer<Throwable> onError) {
        LlmProvider p = resolve(req.getModel());
        p.streamChat(req, onChunk, onError);
    }

    public List<String> listModels() {
        List<String> all = new ArrayList<String>();
        for (LlmProvider p : providers.values()) {
            for (com.zifang.z.agent.kernel.llm.Model m : p.listModels()) {
                all.add(m.getId());
            }
        }
        return Collections.unmodifiableList(all);
    }

    public int providerCount() {
        return providers.size();
    }

    /**
     * 工具调用参数 JSON → Map 解析.
     */
    public Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return mapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new AgentException(AgentException.INVALID_REQUEST,
                    "invalid tool arguments json: " + argumentsJson, e);
        }
    }

    /**
     * 把工具调用 ToolResult 序列化成 Msg(TOOL role).
     */
    public Msg toolResultMsg(String toolCallId, ToolResult result) {
        String content = result.isError()
                ? "[ERROR] " + result.getName() + ": " + result.getContent()
                : result.getContent();
        return Msg.toolResult(toolCallId, content);
    }

    /**
     * 工具名 → Tool 查找.
     */
    public Tool findTool(List<Tool> tools, String name) {
        if (tools == null) return null;
        for (Tool t : tools) {
            if (t.getName().equals(name)) return t;
        }
        return null;
    }

    /**
     * 累计 token 用量.
     */
    public TokenUsage addUsage(TokenUsage a, TokenUsage b) {
        if (a == null) return b;
        if (b == null) return a;
        return new TokenUsage(a.getPromptTokens() + b.getPromptTokens(),
                a.getCompletionTokens() + b.getCompletionTokens(),
                a.getTotalTokens() + b.getTotalTokens());
    }
}