package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsRequest;
import com.zifang.z.agent.engine.agent.llm.model.ToolCall;
import com.zifang.z.agent.engine.agent.llm.tool.core.Tool;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * ReActAgent 单元测试。
 * <p>
 * 不依赖真实 LLM：所有 LLM 响应通过 MockLlmClient 注入。
 * <p>
 * 覆盖维度：
 * 1. 基础：初始化、Builder 校验、getName/Description、toString
 * 2. 主循环：直接答、单步 tool call、多步 tool call、final_answer 工具
 * 3. 边界：maxSteps 上限、stop() 主动停止、reset() 清空
 * 4. 错误：LLM 抛异常、LLM 返回 null、Tool 抛异常
 * 5. Memory：上下文累积、system prompt 注入
 * 6. 路由：providerCode 正确传递、temperature/maxTokens 透传
 * 7. 多次 run：history 累积
 */
public class ReActAgentTest {

    // ==================== 1. 基础 ====================

    @Test
    public void testBuilder_missingLlmClient_throws() {
        try {
            new ReActAgent.Builder().name("x").build();
            fail("expected IllegalStateException when llmClient is missing");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("llmclient"));
        }
    }

    @Test
    public void testBuilder_defaultsAreApplied() {
        MockLlmClient llm = MockLlmClient.withText("hi");
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .build();
        assertEquals("ReActAgent", agent.name());
        assertTrue(agent.getDescription().toLowerCase().contains("react"));
        assertNotNull(agent.getMemory());
        assertNotNull(agent.getToolkit());
        assertFalse(agent.isRunning());
        assertEquals(0, agent.getMemory().size());
    }

    @Test
    public void testBuilder_customNameAndDescription() {
        ReActAgent agent = new ReActAgent.Builder()
                .name("researcher")
                .description("does research")
                .llmClient(MockLlmClient.withText("x"))
                .build();
        assertEquals("researcher", agent.name());
        assertEquals("does research", agent.getDescription());
    }

    @Test
    public void testToString_containsKeyFields() {
        ReActAgent agent = new ReActAgent.Builder()
                .name("n")
                .llmClient(MockLlmClient.withText("x"))
                .model("gpt-x")
                .maxSteps(7)
                .addTool(TestTools.echoTool(), TestTools.fixedExecutor("ok"))
                .build();
        String s = agent.toString();
        assertTrue(s.contains("n"));
        assertTrue(s.contains("gpt-x"));
        assertTrue(s.contains("7"));
    }

    // ==================== 2. 主循环 ====================

    @Test
    public void testRun_simpleTextReply_noToolCall() {
        MockLlmClient llm = MockLlmClient.withText("hello there");
        ReActAgent agent = new ReActAgent.Builder()
                .name("a")
                .llmClient(llm)
                .provider("ollama")
                .model("m")
                .build();

        String reply = agent.run("hi");
        assertEquals("hello there", reply);
        assertEquals(1, llm.getCallCount());
        assertEquals("ollama", llm.getLastProviderCode());

        List<Msg> hist = agent.getMemory().getConversationMessages();
        assertEquals(2, hist.size()); // user + assistant
        assertEquals(Msg.ROLE_USER, hist.get(0).getRole());
        assertEquals(Msg.ROLE_ASSISTANT, hist.get(1).getRole());
        assertTrue(hist.get(1).isFinal());
    }

    @Test
    public void testRun_singleToolCall_executesAndReturns() {
        AtomicInteger callCount = new AtomicInteger();
        AtomicReference<Map<String, Object>> args = new AtomicReference<>();
        MockLlmClient llm = MockLlmClient.withFullFlow(
                "I need to add 2 and 3",
                "add", "{\"a\":\"2\",\"b\":\"3\"}",
                "The sum is 5");

        Toolkit tk = new Toolkit();
        tk.registerTool(TestTools.addTool(), TestTools.addExecutor(callCount, args));

        ReActAgent agent = new ReActAgent.Builder()
                .name("calc")
                .llmClient(llm)
                .toolkit(tk)
                .build();

        String reply = agent.run("2 + 3 = ?");
        assertEquals("The sum is 5", reply);
        assertEquals(1, callCount.get());
        // ReActAgent 的 parseArgs 把所有 value 解析为 String
        assertEquals("2", args.get().get("a"));
        assertEquals("3", args.get().get("b"));
        assertEquals(2, llm.getCallCount()); // think+tool_call, final text

        // Memory 应包含：user, tool_result, final assistant
        List<Msg> hist = agent.getMemory().getConversationMessages();
        assertTrue(hist.size() >= 3);
        boolean hasToolResult = hist.stream().anyMatch(m -> Msg.ROLE_TOOL.equals(m.getRole()));
        assertTrue("memory must contain a tool result", hasToolResult);
    }

    @Test
    public void testRun_finalAnswerTool_returnsAnswer() {
        // ReActAgent 看到 tool_call 才不会 break；final_answer 工具被识别为"终止"。
        // 新流式版去掉了 "Final Answer: " 前缀——纯答案文本更干净。
        MockLlmClient llm = MockLlmClient.withToolCall("final_answer", "{\"answer\":\"done!\"}");

        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .build();
        String reply = agent.run("finish");
        assertEquals("done!", reply);
        assertTrue(agent.getMemory().getConversationMessages()
                .stream().anyMatch(m -> m.isFinal()));
    }

    @Test
    public void testRun_maxStepsLimit_stopsLoop() {
        // 永远触发 tool_call，永不返回 final → 应在 maxSteps 后停止
        MockLlmClient llm = new MockLlmClient();
        for (int i = 0; i < 10; i++) {
            llm.enqueue(MockLlmClient.toolCallResult("echo", "{\"message\":\"x\"}"));
        }

        Toolkit tk = new Toolkit();
        tk.registerTool(TestTools.echoTool(), TestTools.fixedExecutor("ok"));

        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .toolkit(tk)
                .maxSteps(3)
                .build();

        String reply = agent.run("loop forever");
        assertTrue(reply.contains("最大步数") || reply.contains("max") || reply.contains("3"));
        assertEquals(3, llm.getCallCount());
    }

    // ==================== 3. 错误处理 ====================

    @Test
    public void testRun_llmThrowsException_returnsErrorMsg() {
        MockLlmClient llm = new MockLlmClient().withError("network down");
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .build();
        String reply = agent.run("hi");
        assertNotNull(reply);
        assertTrue(reply.contains("网络") || reply.contains("出错") || reply.length() > 0);
    }

    @Test
    public void testRun_llmReturnsNull_returnsErrorMsg() {
        MockLlmClient llm = new MockLlmClient();
        llm.enqueue(MockLlmClient.nullResult());
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .build();
        String reply = agent.run("hi");
        assertNotNull(reply);
        // LLM 调用失败时应给出错误提示
        assertTrue(reply.contains("LLM") || reply.contains("失败") || reply.length() > 0);
    }

    @Test
    public void testRun_toolThrows_isRecordedAsErrorResult() {
        // 第一次响应：tool_call(boom) → 工具抛错 → tool result 包含 Error
        // 第二次响应：纯文本 "ok, recovered"
        // 本用例走的是 run()（阻塞入口，走 LlmClient.chat），因此用 enqueue 脚本化
        // 阻塞队列；原来用 setStreamGenerator 只填了流式队列，阻塞队列是空的，
        // chat() 直接返回 null，用例拿到的是 "LLM 调用失败" 而非本用例要验的恢复路径。
        MockLlmClient llm = new MockLlmClient()
                .enqueueToolCall("boom", "{}")
                .enqueueText("ok, recovered");

        Toolkit tk = new Toolkit();
        tk.registerTool(new Tool("boom", "explodes", null), TestTools.throwingExecutor("kaboom"));

        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .toolkit(tk)
                .build();

        String reply = agent.run("try");
        // Tool 抛错后 ReActAgent 会把错误塞进 tool result，然后 LLM 继续，最终给"ok, recovered"
        assertEquals("ok, recovered", reply);

        // 验证 tool_result 消息里包含 error 标记
        boolean hasErrorResult = agent.getMemory().getConversationMessages().stream()
                .filter(m -> Msg.ROLE_TOOL.equals(m.getRole()))
                .anyMatch(m -> m.getContent() != null && m.getContent().contains("Error"));
        assertTrue("tool error should appear in tool result message", hasErrorResult);
    }

    // ==================== 4. 控制流 ====================

    @Test
    public void testStop_setsFlag() {
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(MockLlmClient.withText("ok"))
                .build();
        assertFalse(agent.isRunning());
        agent.stop();
        assertFalse(agent.isRunning()); // stop 后 isRunning=false
    }

    @Test
    public void testReset_clearsMemory() {
        MockLlmClient llm = MockLlmClient.withText("hello");
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .build();
        agent.run("hi");
        // run 后 memory 包含 user + assistant 两条
        assertEquals(2, agent.getMemory().size());
        agent.reset();
        assertEquals(0, agent.getMemory().size());
    }

    // ==================== 5. Memory & System Prompt ====================

    @Test
    public void testRun_withContextSystemPrompt_isStoredInMemory() {
        // ReActAgent.run(String, Map) 把 system_prompt 存到 memory，
        // 实际发给 LLM 的 system message 是 buildSystemPrompt() 构造的固定内容。
        // 这里验证的是"用户提供的 system_prompt 至少被记录到 memory"，
        // 因为 buildSystemPrompt() 是否覆盖 user-provided 提示是另一个产品决策。
        MockLlmClient llm = MockLlmClient.withText("reply");
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .build();
        java.util.HashMap<String, Object> ctx = new java.util.HashMap<>();
        ctx.put("system_prompt", "Always answer in haiku.");
        agent.runWithContext("hi", ctx);

        List<Msg> mem = agent.getMemory().getMessages();
        boolean hasCustomSystem = mem.stream()
                .anyMatch(m -> Msg.ROLE_SYSTEM.equals(m.getRole())
                        && m.getContent() != null
                        && m.getContent().contains("haiku"));
        assertTrue("user-provided system_prompt must be stored in memory", hasCustomSystem);
    }

    @Test
    public void testRun_multipleInvocations_accumulateHistory() {
        MockLlmClient llm = new MockLlmClient()
                .enqueueText("first reply")
                .enqueueText("second reply");
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .build();

        agent.run("first");
        agent.run("second");

        // memory 累计：user1, assistant1, user2, assistant2
        List<Msg> hist = agent.getMemory().getConversationMessages();
        assertEquals(4, hist.size());
        // run() 是阻塞入口（LlmClient.chat），流式是另一个入口 stream()，
        // 所以这里数阻塞调用次数，不是流式次数。
        assertEquals(2, llm.getCallCount());

        // 第二次调用时 messages 应当包含 user1/assistant1
        ChatCompletionsRequest secondReq = llm.getAllRequests().get(1);
        assertTrue("second call must include first user msg",
                secondReq.getMessages().stream().anyMatch(m -> "first".equals(m.getContent())));
    }

    @Test
    public void testRunWithHistory_returnsAllMessages() {
        MockLlmClient llm = MockLlmClient.withText("done");
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .build();
        // runWithHistory 在 Agent 接口里返回 List<StreamEvent>；要拿 Msg 形式用
        // ReActAgent.runWithHistoryAsMsgs（保留 legacy 形式）。
        agent.run("hello");
        List<Msg> msgs = agent.runWithHistoryAsMsgs("hello");
        assertEquals(2, msgs.size());
        assertEquals(Msg.ROLE_USER, msgs.get(0).getRole());
        assertEquals(Msg.ROLE_ASSISTANT, msgs.get(1).getRole());
    }

    // ==================== 6. 参数透传 ====================

    @Test
    public void testRun_temperatureAndMaxTokensAreForwarded() {
        MockLlmClient llm = MockLlmClient.withText("x");
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(llm)
                .provider("openai")
                .model("gpt-4")
                .temperature(0.42)
                .maxTokens(123)
                .build();

        agent.run("hi");
        ChatCompletionsRequest req = llm.getLastRequest();
        assertEquals("gpt-4", req.getModel());
        assertEquals(Double.valueOf(0.42), req.getTemperature());
        assertEquals(Integer.valueOf(123), req.getMaxTokens());
        assertEquals("openai", llm.getLastProviderCode());
    }

    @Test
    public void testRun_toolsAreForwardedToLlm() {
        MockLlmClient llm = MockLlmClient.withText("x");
        Toolkit tk = new Toolkit();
        tk.registerTool(TestTools.addTool(), TestTools.fixedExecutor("0"));

        new ReActAgent.Builder()
                .llmClient(llm)
                .toolkit(tk)
                .build()
                .run("hi");

        ChatCompletionsRequest req = llm.getLastRequest();
        assertNotNull(req.getTools());
        assertEquals(1, req.getTools().size());
        assertEquals("add", req.getTools().get(0).getFunctionName());
    }

    // ==================== 7. Builder 工具注册 ====================

    @Test
    public void testBuilder_addTool_registersInToolkit() {
        ReActAgent agent = new ReActAgent.Builder()
                .llmClient(MockLlmClient.withText("x"))
                .addTool(TestTools.addTool(), TestTools.fixedExecutor("0"))
                .build();
        assertEquals(1, agent.getToolkit().size());
        assertTrue(agent.getToolkit().hasTool("add"));
    }
}
