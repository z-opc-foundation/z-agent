package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.provider.LlmClient;
import com.zifang.z.agent.engine.agent.llm.tool.core.Tool;
import com.zifang.z.agent.engine.agent.llm.tool.core.ToolParam;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/**
 * 真实链路 ReAct 端到端测试：跑本地 ollama (qwen3:8b)，验证 ReActAgent 端到端工作。
 * 跳过条件：未设置 RUN_REAL_OLLAMA=1。
 */
public class ReActAgentRealE2ETest {

    private static boolean shouldRun() {
        return "1".equals(System.getenv("RUN_REAL_OLLAMA"));
    }

    private LlmClient client() {
        LlmClient c = new LlmClient();
        c.configureProvider("ollama", "http://localhost:11434", null);
        return c;
    }

    @Test
    public void realE2E_userLookup_email() throws Exception {
        // 门控：本用例要真连 localhost:11434 上的 ollama，没有该服务时必然红。
        // 类注释里写了跳过条件、shouldRun() 也早就写好了，但一直没被调用，
        // 于是没装 ollama 的机器（包括 CI）每次都拿到一条红项。
        // Assume 未满足时整条跳过，计为 skipped 而不是 failed。
        org.junit.Assume.assumeTrue("需要本地 ollama：请设 RUN_REAL_OLLAMA=1", shouldRun());

        LlmClient c = client();

        Tool lookup = new Tool("user_lookup",
                "Look up internal user record by user id",
                Arrays.asList(new ToolParam("user_id", "string", true, "the unique user id")));

        AtomicReference<String> capturedUserId = new AtomicReference<>();
        Toolkit.ToolExecutor executor = (toolName, args) -> {
            capturedUserId.set(String.valueOf(args.get("user_id")));
            return "{\"email\":\"[email protected]\",\"role\":\"admin\"}";
        };

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(lookup, executor);

        ReActAgent agent = new ReActAgent.Builder()
                .name("assistant")
                .llmClient(c)
                .provider("ollama")

                .model("qwen2.5:7b-instruct")
                .toolkit(toolkit)
                .maxSteps(4)
                .temperature(0.1)
                .build();

        long start = System.currentTimeMillis();
        String reply = agent.run("调用 user_lookup 工具查 u-9921 的邮箱，不要用文字描述，必须真正调用工具。");
        long cost = System.currentTimeMillis() - start;

        System.out.println("[E2E] reply=" + reply);
        System.out.println("[E2E] captured user_id=" + capturedUserId.get());
        System.out.println("[E2E] LLM call count=" + (c instanceof LlmClient
                ? c.getClass().getSimpleName() : "?"));
        System.out.println("[E2E] cost ms=" + cost);

        // 工具必须被真实调用（不靠模型文字假装）
        if (capturedUserId.get() == null) {
            System.out.println("[E2E] FAIL: tool was NOT called. Model replied: " + reply);
        }
        assertEquals("u-9921", capturedUserId.get());
    }
}
