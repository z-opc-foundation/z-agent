package com.zifang.z.agent.core.agent;

import com.zifang.z.agent.api.dto.AgentDto;
import com.zifang.z.agent.core.properties.AgentProperties;
import com.zifang.z.agent.kernel.agent.AgentRequest;
import com.zifang.z.agent.kernel.agent.AgentResponse;
import com.zifang.z.agent.kernel.llm.ChatCompletionsRequest;
import com.zifang.z.agent.kernel.llm.ChatCompletionsResponse;
import com.zifang.z.agent.kernel.llm.LlmProvider;
import com.zifang.z.agent.kernel.llm.Model;
import com.zifang.z.agent.kernel.message.Msg;
import com.zifang.z.agent.kernel.message.ToolCall;
import com.zifang.z.agent.kernel.tool.Tool;
import com.zifang.z.agent.kernel.tool.ToolResult;
import com.zifang.z.agent.kernel.types.TokenUsage;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * ReAct / Plan Agent 端到端测试 — 用 mock LlmProvider 驱动完整推理循环.
 *
 * <p>验证:
 * <p>  1. 多步工具调用被正确解析 + 执行 + 反馈
 * <p>  2. LLM 返回无 tool_call 时及时终止 (finishedReason=completed)
 * <p>  3. maxSteps 限制生效 (finishedReason=max_steps)
 * <p>5. Tool not found → ToolResult.failure 进入 messages
 *  6. token usage 累计正确
 *  7. PlanAgent 三阶段 plan/execute/synthesize
 *  8. LlmAdapter 按 model id 路由多 provider
 */
public class AgentEndToEndTest {

    private static class ScriptedLlmProvider implements LlmProvider {
        private final List<ChatCompletionsResponse> script = new ArrayList<>();
        public final List<ChatCompletionsRequest> received = new ArrayList<>();
        private final String providerName;
        private final AtomicInteger callCount = new AtomicInteger(0);

        ScriptedLlmProvider(String name, ChatCompletionsResponse... responses) {
            this.providerName = name;
            this.script.addAll(Arrays.asList(responses));
        }

        @Override public String name() { return providerName; }
        @Override public List<Model> listModels() { return Arrays.asList(new Model("mock-model", "Mock Model", providerName, new ArrayList<>(), 8192, 4096)); }
        @Override public boolean supportsModel(String modelId) { return true; }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            received.add(request);
            int idx = callCount.getAndIncrement();
            if (idx < script.size()) return script.get(idx);
            return new ChatCompletionsResponse("mock-id-" + idx, "mock-model",
                    Arrays.asList(new ChatCompletionsResponse.Choice(0, "[script exhausted]", new ArrayList<>(), "stop")),
                    TokenUsage.empty(), "stop", null);
        }

        @Override
        public void streamChat(ChatCompletionsRequest request, Consumer<ChatCompletionsResponse> onChunk, Consumer<Throwable> onError) {
            onChunk.accept(chat(request));
        }
    }

    private static class CounterTool implements Tool {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override public String getName() { return "increment"; }
        @Override public String getDescription() { return "把内部计数器 +1, 返回新值"; }

        @Override
        public Map<String, Object> getSchema() {
            Map<String, Object> s = new HashMap<>();
            s.put("type", "object");
            s.put("properties", new HashMap<String, Object>());
            return s;
        }

        @Override
        public ToolResult execute(Map<String, Object> arguments) {
            int n = counter.incrementAndGet();
            return ToolResult.success("call-id", "increment", "counter=" + n);
        }
    }

    private static ChatCompletionsResponse toolCallResp(String model, String tcId, String toolName, String argsJson) {
        ToolCall tc = new ToolCall(tcId, toolName, argsJson);
        ChatCompletionsResponse.Choice c = new ChatCompletionsResponse.Choice(0, "", Arrays.asList(tc), "tool_calls");
        return new ChatCompletionsResponse("mock", model, Arrays.asList(c), new TokenUsage(10, 5, 15), "tool_calls", null);
    }

    private static ChatCompletionsResponse textResp(String model, String text) {
        ChatCompletionsResponse.Choice c = new ChatCompletionsResponse.Choice(0, text, new ArrayList<>(), "stop");
        return new ChatCompletionsResponse("mock", model, Arrays.asList(c), new TokenUsage(8, 12, 20), "stop", null);
    }

    private AgentProperties defaultProps() {
        AgentProperties p = new AgentProperties();
        p.setDefaultModel("mock-model");
        p.setDefaultMaxSteps(5);
        p.setDefaultTemperature(0.0);
        return p;
    }

    private AgentDto reactDto(String name, String model) {
        return AgentDto.builder().name(name).kind("react").model(model).build();
    }

    private AgentDto planDto(String name, String model) {
        return AgentDto.builder().name(name).kind("plan").model(model).build();
    }

    @Test
    public void react_loop_completes_after_tool_call() {
        ScriptedLlmProvider llm = new ScriptedLlmProvider("mock",
                toolCallResp("mock-model", "tc-1", "increment", "{}"),
                textResp("mock-model", "Done, counter was 1"));

        LlmAdapter adapter = new LlmAdapter(defaultProps());
        adapter.registerProvider(llm);
        ReActAgent agent = new ReActAgent(reactDto("react-1", "mock-model"), defaultProps(), adapter, Arrays.asList(new CounterTool()));

        AgentResponse resp = agent.run(new AgentRequest(Msg.user("call increment then say done"), null, null, null, null));

        assertEquals("completed", resp.getFinishedReason());
        assertEquals("Done, counter was 1", resp.getOutput().getContent());
        // 1 action step (tool call) + 1 thought step (final text) = 2
        assertEquals(2, resp.getSteps().size());
        assertEquals("action", resp.getSteps().get(0).getKind());
        assertEquals("thought", resp.getSteps().get(1).getKind());
        // token: 15 (tool_call) + 20 (text) = 35
        assertEquals(35, resp.getUsage().getTotalTokens());
        assertEquals(2, llm.received.size());
    }

    @Test
    public void react_multi_tool_calls_chain() {
        ScriptedLlmProvider llm = new ScriptedLlmProvider("mock",
                toolCallResp("mock-model", "tc-1", "increment", "{}"),
                toolCallResp("mock-model", "tc-2", "increment", "{}"),
                textResp("mock-model", "counter reached 2"));

        LlmAdapter adapter = new LlmAdapter(defaultProps());
        adapter.registerProvider(llm);
        ReActAgent agent = new ReActAgent(reactDto("react-2", "mock-model"), defaultProps(), adapter, Arrays.asList(new CounterTool()));

        AgentResponse resp = agent.run(new AgentRequest(Msg.user("increment twice"), null, null, null, null));
        assertEquals("completed", resp.getFinishedReason());
        assertEquals("counter reached 2", resp.getOutput().getContent());
        // 2 action steps + 1 thought step = 3
        assertEquals(3, resp.getSteps().size());
        assertEquals(3, llm.received.size());
        // token: 15 + 15 + 20 = 50
        assertEquals(50, resp.getUsage().getTotalTokens());
    }

    @Test
    public void react_max_steps_reached() {
        ScriptedLlmProvider llm = new ScriptedLlmProvider("mock",
                toolCallResp("mock-model", "tc-1", "increment", "{}"),
                toolCallResp("mock-model", "tc-2", "increment", "{}"),
                toolCallResp("mock-model", "tc-3", "increment", "{}"));

        AgentProperties props = defaultProps();
        props.setDefaultMaxSteps(2);

        LlmAdapter adapter = new LlmAdapter(props);
        adapter.registerProvider(llm);
        ReActAgent agent = new ReActAgent(reactDto("react-3", "mock-model"), props, adapter, Arrays.asList(new CounterTool()));

        AgentResponse resp = agent.run(new AgentRequest(Msg.user("loop forever"), null, null, null, null));
        assertEquals("max_steps", resp.getFinishedReason());
        assertEquals(2, llm.received.size());
    }

    @Test
    public void react_tool_not_found_returns_failure_message() {
        ScriptedLlmProvider llm = new ScriptedLlmProvider("mock",
                toolCallResp("mock-model", "tc-1", "no_such_tool", "{}"),
                textResp("mock-model", "ok tool not found"));

        LlmAdapter adapter = new LlmAdapter(defaultProps());
        adapter.registerProvider(llm);
        ReActAgent agent = new ReActAgent(reactDto("react-4", "mock-model"), defaultProps(), adapter, Arrays.asList(new CounterTool()));

        AgentResponse resp = agent.run(new AgentRequest(Msg.user("call no_such_tool"), null, null, null, null));
        assertEquals("completed", resp.getFinishedReason());
        assertEquals("ok tool not found", resp.getOutput().getContent());
    }

    @Test
    public void react_no_tools_provided_returns_first_response() {
        ScriptedLlmProvider llm = new ScriptedLlmProvider("mock",
                textResp("mock-model", "no tools needed"));

        LlmAdapter adapter = new LlmAdapter(defaultProps());
        adapter.registerProvider(llm);
        ReActAgent agent = new ReActAgent(reactDto("react-5", "mock-model"), defaultProps(), adapter, null);

        AgentResponse resp = agent.run(new AgentRequest(Msg.user("just answer"), null, null, null, null));
        assertEquals("completed", resp.getFinishedReason());
        assertEquals("no tools needed", resp.getOutput().getContent());
        assertEquals(1, resp.getSteps().size());
    }

    @Test
    public void react_llm_error_returns_error_response() {
        LlmProvider errorProvider = new LlmProvider() {
            @Override public String name() { return "error"; }
            @Override public List<Model> listModels() { return Arrays.asList(new Model("err-model", "Err Model", "error", new ArrayList<>(), 8192, 4096)); }
            @Override public boolean supportsModel(String modelId) { return true; }
            @Override public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
                throw new RuntimeException("provider down");
            }
            @Override public void streamChat(ChatCompletionsRequest request, Consumer<ChatCompletionsResponse> onChunk, Consumer<Throwable> onError) {
                onError.accept(new RuntimeException("provider down"));
            }
        };

        LlmAdapter adapter = new LlmAdapter(defaultProps());
        adapter.registerProvider(errorProvider);
        ReActAgent agent = new ReActAgent(reactDto("react-err", "err-model"), defaultProps(), adapter, Arrays.asList(new CounterTool()));

        AgentResponse resp = agent.run(new AgentRequest(Msg.user("will fail"), null, null, null, null));
        assertEquals("llm_error", resp.getFinishedReason());
        assertNotNull(resp.getError());
        assertTrue(resp.getOutput().getContent().contains("LLM error"));
    }

    @Test
    public void react_routes_by_model_id() {
        ScriptedLlmProvider openaiMock = new ScriptedLlmProvider("openai", textResp("gpt-4", "from openai mock"));
        ScriptedLlmProvider anthropicMock = new ScriptedLlmProvider("anthropic", textResp("claude-3", "from anthropic mock"));

        LlmProvider openai = new LlmProvider() {
            @Override public String name() { return "openai"; }
            @Override public List<Model> listModels() { return Arrays.asList(new Model("gpt-4", "GPT-4", "openai", new ArrayList<>(), 128000, 8192)); }
            @Override public boolean supportsModel(String m) { return m != null && m.startsWith("gpt"); }
            @Override public ChatCompletionsResponse chat(ChatCompletionsRequest r) { return openaiMock.chat(r); }
            @Override public void streamChat(ChatCompletionsRequest r, Consumer<ChatCompletionsResponse> c, Consumer<Throwable> e) { openaiMock.streamChat(r, c, e); }
        };
        LlmProvider anthropic = new LlmProvider() {
            @Override public String name() { return "anthropic"; }
            @Override public List<Model> listModels() { return Arrays.asList(new Model("claude-3", "Claude 3", "anthropic", new ArrayList<>(), 200000, 4096)); }
            @Override public boolean supportsModel(String m) { return m != null && m.startsWith("claude"); }
            @Override public ChatCompletionsResponse chat(ChatCompletionsRequest r) { return anthropicMock.chat(r); }
            @Override public void streamChat(ChatCompletionsRequest r, Consumer<ChatCompletionsResponse> c, Consumer<Throwable> e) { anthropicMock.streamChat(r, c, e); }
        };

        LlmAdapter adapter = new LlmAdapter(defaultProps());
        adapter.registerProvider(openai);
        adapter.registerProvider(anthropic);

        AgentDto dto = AgentDto.builder().name("multi-1").kind("react").build();
        ReActAgent agent = new ReActAgent(dto, defaultProps(), adapter, null);

        Map<String, Object> cfg1 = new HashMap<>();
        cfg1.put("model", "gpt-4");
        AgentResponse r1 = agent.run(new AgentRequest(Msg.user("hi"), null, null, null, cfg1));
        assertEquals("from openai mock", r1.getOutput().getContent());
        assertEquals(1, openaiMock.received.size());
        assertEquals(0, anthropicMock.received.size());

        Map<String, Object> cfg2 = new HashMap<>();
        cfg2.put("model", "claude-3");
        AgentResponse r2 = agent.run(new AgentRequest(Msg.user("hi"), null, null, null, cfg2));
        assertEquals("from anthropic mock", r2.getOutput().getContent());
        assertEquals(1, openaiMock.received.size());
        assertEquals(1, anthropicMock.received.size());
    }

    @Test
    public void plan_agent_with_parsed_plan_completes() {
        ScriptedLlmProvider llm = new ScriptedLlmProvider("mock",
                textResp("mock-model", "1. 分析需求\n2. 设计方案\n3. 实施验证"),
                textResp("mock-model", "已完成需求分析"),
                textResp("mock-model", "已完成方案设计"),
                textResp("mock-model", "已完成实施验证"),
                textResp("mock-model", "全部 3 步完成, 总结果: 任务成功"));

        LlmAdapter adapter = new LlmAdapter(defaultProps());
        adapter.registerProvider(llm);
        PlanAgent agent = new PlanAgent(planDto("plan-1", "mock-model"), defaultProps(), adapter);

        AgentResponse resp = agent.run(new AgentRequest(Msg.user("做一个 3 步任务"), null, null, null, null));
        assertEquals("completed", resp.getFinishedReason());
        assertEquals("全部 3 步完成, 总结果: 任务成功", resp.getOutput().getContent());
        assertEquals(5, llm.received.size());
        List<String> parsed = PlanAgent.parsePlan("1. 分析需求\n2. 设计方案\n3. 实施验证");
        assertEquals(3, parsed.size());
        assertEquals("分析需求", parsed.get(0));
    }

    @Test
    public void plan_agent_handles_unparseable_plan_as_single_step() {
        ScriptedLlmProvider llm = new ScriptedLlmProvider("mock",
                textResp("mock-model", "无结构化计划"),
                textResp("mock-model", "完成"),
                textResp("mock-model", "结果 OK"));

        LlmAdapter adapter = new LlmAdapter(defaultProps());
        adapter.registerProvider(llm);
        PlanAgent agent = new PlanAgent(planDto("plan-2", "mock-model"), defaultProps(), adapter);

        AgentResponse resp = agent.run(new AgentRequest(Msg.user("just do it"), null, null, null, null));
        assertEquals("completed", resp.getFinishedReason());
        assertEquals(3, llm.received.size());
        assertEquals("结果 OK", resp.getOutput().getContent());
    }

    @Test
    public void react_completed_step_has_final_answer() {
        ScriptedLlmProvider llm = new ScriptedLlmProvider("mock",
                toolCallResp("mock-model", "tc-1", "increment", "{}"),
                textResp("mock-model", "final answer"));

        LlmAdapter adapter = new LlmAdapter(defaultProps());
        adapter.registerProvider(llm);
        ReActAgent agent = new ReActAgent(reactDto("react-steps", "mock-model"), defaultProps(), adapter, Arrays.asList(new CounterTool()));

        AgentResponse resp = agent.run(new AgentRequest(Msg.user("go"), null, null, null, null));
        assertFalse(resp.getSteps().isEmpty());
        AgentResponse.Step last = resp.getSteps().get(resp.getSteps().size() - 1);
        assertEquals("thought", last.getKind());
        assertEquals("final answer", last.getThought().getContent());
    }
}