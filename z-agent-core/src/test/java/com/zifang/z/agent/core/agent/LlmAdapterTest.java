package com.zifang.z.agent.core.agent;

import com.zifang.z.agent.api.dto.AgentDto;
import com.zifang.z.agent.core.properties.AgentProperties;
import com.zifang.z.agent.kernel.llm.ChatCompletionsRequest;
import com.zifang.z.agent.kernel.llm.ChatCompletionsResponse;
import com.zifang.z.agent.kernel.llm.LlmProvider;
import com.zifang.z.agent.kernel.llm.Model;
import com.zifang.z.agent.kernel.message.Msg;
import com.zifang.z.agent.kernel.message.MessageType;
import com.zifang.z.agent.kernel.message.ToolCall;
import com.zifang.z.agent.kernel.tool.Tool;
import com.zifang.z.agent.kernel.tool.ToolResult;
import com.zifang.z.agent.kernel.types.MessageRole;
import com.zifang.z.agent.kernel.types.TokenUsage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LlmAdapterTest {

    private static LlmProvider openAiFake() {
        return new LlmProvider() {
            @Override public String name() { return "openai"; }
            @Override public List<Model> listModels() {
                return Collections.singletonList(new Model("gpt-4o-mini", "gpt-4o-mini", "openai", Collections.<Model.Capability>emptyList(), 128000L, 16384L));
            }
            @Override public boolean supportsModel(String modelId) {
                return modelId != null && modelId.startsWith("gpt-");
            }
            @Override public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
                return new ChatCompletionsResponse("test", request.getModel(),
                        Collections.singletonList(
                                new ChatCompletionsResponse.Choice(0, "ok",
                                        Collections.<ToolCall>emptyList(), "stop")),
                        TokenUsage.empty(), "stop", Collections.emptyMap());
            }
            @Override public void streamChat(ChatCompletionsRequest request,
                                             java.util.function.Consumer<ChatCompletionsResponse> onChunk,
                                             java.util.function.Consumer<Throwable> onError) {
                onChunk.accept(chat(request));
            }
        };
    }

    private static LlmProvider anthropicFake() {
        return new LlmProvider() {
            @Override public String name() { return "anthropic"; }
            @Override public List<Model> listModels() {
                return Collections.singletonList(new Model("claude-sonnet-4", "claude-sonnet-4", "anthropic", Collections.<Model.Capability>emptyList(), 200000L, 8192L));
            }
            @Override public boolean supportsModel(String modelId) {
                return modelId != null && modelId.startsWith("claude-");
            }
            @Override public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
                return new ChatCompletionsResponse("anthropic", request.getModel(),
                        Collections.singletonList(
                                new ChatCompletionsResponse.Choice(0, "anthropic-ok",
                                        Collections.<ToolCall>emptyList(), "stop")),
                        TokenUsage.empty(), "stop", Collections.emptyMap());
            }
            @Override public void streamChat(ChatCompletionsRequest request,
                                             java.util.function.Consumer<ChatCompletionsResponse> onChunk,
                                             java.util.function.Consumer<Throwable> onError) {
                onChunk.accept(chat(request));
            }
        };
    }

    private static Tool stubTool() {
        return new Tool() {
            @Override public String getName() { return "echo"; }
            @Override public String getDescription() { return "echo tool"; }
            @Override public Map<String, Object> getSchema() { return new HashMap<String, Object>(); }
            @Override public ToolResult execute(Map<String, Object> arguments) {
                return ToolResult.success("1", "echo", "echoed:" + arguments.get("text"));
            }
        };
    }

    @Test
    public void resolve_routes_by_model_prefix() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        adapter.registerProvider(openAiFake());
        adapter.registerProvider(anthropicFake());
        assertEquals("openai", adapter.resolve("gpt-4o-mini").name());
        assertEquals("anthropic", adapter.resolve("claude-sonnet-4").name());
    }

    @Test
    public void resolve_unknown_throws() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        adapter.registerProvider(openAiFake());
        try {
            adapter.resolve("unknown-model");
        } catch (RuntimeException expected) {
            return;
        }
        throw new AssertionError("expected exception");
    }

    @Test
    public void chat_routes_to_correct_provider() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        adapter.registerProvider(openAiFake());
        adapter.registerProvider(anthropicFake());
        ChatCompletionsRequest req = new ChatCompletionsRequest("claude-sonnet-4",
                Collections.singletonList(Msg.user("hi")));
        ChatCompletionsResponse resp = adapter.chat(req);
        assertEquals("anthropic-ok", resp.getChoices().get(0).getContent());
    }

    @Test
    public void parse_arguments_handles_empty() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        assertTrue(adapter.parseArguments(null).isEmpty());
        assertTrue(adapter.parseArguments("").isEmpty());
    }

    @Test
    public void parse_arguments_handles_json() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        Map<String, Object> args = adapter.parseArguments("{\"a\":1,\"b\":\"x\"}");
        assertEquals(1, args.get("a"));
        assertEquals("x", args.get("b"));
    }

    @Test
    public void find_tool_returns_match() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        Tool t = stubTool();
        List<Tool> tools = new ArrayList<Tool>();
        tools.add(t);
        assertEquals(t, adapter.findTool(tools, "echo"));
        assertEquals(null, adapter.findTool(tools, "missing"));
        assertEquals(null, adapter.findTool(null, "echo"));
    }

    @Test
    public void tool_result_msg_creates_tool_role_msg() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        Msg m = adapter.toolResultMsg("call-1", ToolResult.success("call-1", "echo", "ok"));
        assertEquals(MessageRole.TOOL, m.getRole());
        assertEquals("call-1", m.getToolCallId());
        assertEquals("ok", m.getContent());
    }

    @Test
    public void tool_result_msg_marks_error() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        Msg m = adapter.toolResultMsg("c", ToolResult.failure("c", "x", "boom"));
        assertTrue(m.getContent().startsWith("[ERROR]"));
    }

    @Test
    public void add_usage_sums() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        TokenUsage a = new TokenUsage(10, 20, 30);
        TokenUsage b = new TokenUsage(5, 5, 10);
        TokenUsage sum = adapter.addUsage(a, b);
        assertEquals(15, sum.getPromptTokens());
        assertEquals(25, sum.getCompletionTokens());
        assertEquals(40, sum.getTotalTokens());
    }

    @Test
    public void provider_count_and_list_models() {
        LlmAdapter adapter = new LlmAdapter(new AgentProperties());
        assertEquals(0, adapter.providerCount());
        adapter.registerProvider(openAiFake());
        adapter.registerProvider(anthropicFake());
        assertEquals(2, adapter.providerCount());
        assertEquals(2, adapter.listModels().size());
        assertTrue(adapter.listModels().contains("gpt-4o-mini"));
        assertTrue(adapter.listModels().contains("claude-sonnet-4"));
    }
}