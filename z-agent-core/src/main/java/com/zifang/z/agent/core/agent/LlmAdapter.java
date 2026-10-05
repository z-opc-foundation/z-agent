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
 * <p>resolve 的优先级:
 * <ol>
 *   <li><b>显式映射</b> {@code z.agent.providers.<model>=<provider bean 名>}——
 *       {@link AgentProperties#getProviders()}。映射到的 bean 没注册就<b>直接报错</b>，
 *       不回落。</li>
 *   <li>回落 {@code supportsModel} 扫描，<b>按注册顺序</b>。</li>
 * </ol>
 *
 * <p>为什么第 2 步必须按注册顺序：此前直接遍历 {@code ConcurrentHashMap.values()}，
 * 而它的迭代序由 <b>bean 名的 {@code String.hashCode()}</b> 决定，与注册顺序无关。
 * 两个 provider 都 {@code supportsModel} 同一个 model（OpenAI 兼容代理与官方
 * openai 共存是常态）时，赢家不可控、任何配置都改不动，且没有任何日志。
 * 实测注册 {@code b,a} 与 {@code a,b} 都由 {@code a} 胜出，{@code m1,m2} 由
 * {@code m1}、{@code azure-openai,openai} 由 {@code openai} 胜出——就是哈希序。
 * 现在按注册顺序扫描，并在多命中时打 WARN 点名冲突者。</p>
 */
public class LlmAdapter {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(LlmAdapter.class);

    private final AgentProperties props;
    private final Map<String, LlmProvider> providers = new ConcurrentHashMap<String, LlmProvider>();
    /** 注册顺序；resolve 的回落扫描按它走，不用 ConcurrentHashMap 的哈希序。 */
    private final List<String> registrationOrder =
            new java.util.concurrent.CopyOnWriteArrayList<String>();
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmAdapter(AgentProperties props) {
        this.props = props == null ? new AgentProperties() : props;
    }

    public void registerProvider(LlmProvider provider) {
        if (provider == null) return;
        registerProvider(provider.name(), provider);
    }

    public void registerProvider(String name, LlmProvider provider) {
        if (provider == null) return;
        if (providers.put(name, provider) == null) {
            registrationOrder.add(name);   // 只有首次注册才占一个位置
        }
    }

    public LlmProvider resolve(String modelId) {
        if (modelId == null) modelId = props.getDefaultModel();

        // ① 显式映射优先
        String mapped = mappedProviderName(modelId);
        if (mapped != null) {
            LlmProvider p = providers.get(mapped);
            if (p == null) {
                throw new AgentException(AgentException.RUN_FAILED,
                        "z.agent.providers[" + modelId + "] 指定了 provider '" + mapped
                                + "'，但没有注册同名的 LlmProvider。已注册: " + registrationOrder
                                + "。映射表是显式配置，不回落 supportsModel 扫描"
                                + "（回落会让配置静默失效，正是本改动要修的）。");
            }
            return p;
        }

        // ② 回落 supportsModel 扫描，按注册顺序
        LlmProvider hit = null;
        List<String> conflicts = new ArrayList<String>();
        for (String name : registrationOrder) {
            LlmProvider p = providers.get(name);
            if (p == null || !p.supportsModel(modelId)) continue;
            if (hit == null) {
                hit = p;
            }
            conflicts.add(name);
        }
        if (hit == null) {
            throw new AgentException(AgentException.RUN_FAILED,
                    "no provider supports model: " + modelId
                            + "（已注册: " + registrationOrder + "）");
        }
        if (conflicts.size() > 1) {
            log.warn("model '{}' 同时被 {} 个 provider 声明支持 {}；按注册顺序取 {}。"
                            + "要精确指定，配 z.agent.providers.{} = <provider bean 名>",
                    modelId, conflicts.size(), conflicts, hit.name(), modelId);
        }
        return hit;
    }

    private String mappedProviderName(String modelId) {
        Map<String, String> table = props.getProviders();
        if (table == null || table.isEmpty()) return null;
        String v = table.get(modelId);
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
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
        // 按注册顺序，返回给调用方的模型列表顺序才是稳定的
        for (String name : registrationOrder) {
            LlmProvider p = providers.get(name);
            if (p == null) continue;
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