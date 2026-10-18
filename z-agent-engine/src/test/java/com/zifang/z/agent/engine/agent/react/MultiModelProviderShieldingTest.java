package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsRequest;
import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsResponse;
import com.zifang.z.agent.engine.agent.llm.model.ChatMessage;
import com.zifang.z.agent.engine.agent.llm.model.ToolCall;
import com.zifang.z.agent.engine.agent.llm.provider.LlmClient;
import com.zifang.z.agent.engine.agent.llm.provider.LlmProvider;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * 多模型 Provider 屏蔽测试。
 * <p>
 * 目标：证明 ReActAgent 不依赖任何具体 LLM 厂商实现。
 * 做法：构造 6+ 种"风格各异"的 LlmProvider（OpenAI / Ollama / Anthropic / 智谱 GLM /
 * 通义 Qwen / 本地 llama.cpp / 自定义），全部注册到同一个 LlmClient，
 * 同一个 ReActAgent + 同一个 toolkit，分别走完整 ReAct 流程，验证全部成功。
 * <p>
 * 各 provider 在"协议层"（请求/响应 JSON 形状、tool_call 字段位置、arguments 编码方式）
 * 各不相同，但 LlmClient 都把它们翻译成统一的 ChatCompletionsRequest/Response，
 * ReActAgent 完全无感。
 */
public class MultiModelProviderShieldingTest {

    // ==================== Provider 风格工厂 ====================

    private static ChatCompletionsResponse textResponse(String model, String content) {
        ChatCompletionsResponse r = new ChatCompletionsResponse();
        r.setId("test-" + model);
        r.setModel(model);
        ChatCompletionsResponse.ChatChoice c = new ChatCompletionsResponse.ChatChoice();
        c.setIndex(0);
        c.setFinishReason("stop");
        ChatMessage m = new ChatMessage("assistant", content);
        c.setMessage(m);
        r.setChoices(Collections.singletonList(c));
        return r;
    }

    /**
     * 参数化测试：每种 provider 都能让 ReActAgent 完成完整流程。
     * 关键点：ReActAgent 内部不感知 providerCode，只通过 LlmClient 抽象路由。
     */
    @Test
    public void testAllProviders_canBeRegistered_andUsedByReActAgent() {
        LlmClient client = new LlmClient();
        // 注意：构造函数已经注册了 ollama 和 openai，这里用同名覆盖即可
        client.registerProvider(new OpenAIStyleProvider());
        client.registerProvider(new OllamaStyleProvider());
        client.registerProvider(new AnthropicStyleProvider());
        client.registerProvider(new ZhipuStyleProvider());
        client.registerProvider(new QwenStyleProvider());
        client.registerProvider(new LlamaCppStyleProvider());
        client.registerProvider(new MinimalCustomProvider());

        // 验证 7 个 provider 都能注册
        assertTrue("openai registered", client.getAvailableProviders().contains("openai"));
        assertTrue("ollama registered", client.getAvailableProviders().contains("ollama"));
        assertTrue("anthropic registered", client.getAvailableProviders().contains("anthropic"));
        assertTrue("zhipu registered", client.getAvailableProviders().contains("zhipu"));
        assertTrue("qwen registered", client.getAvailableProviders().contains("qwen"));
        assertTrue("llamacpp registered", client.getAvailableProviders().contains("llamacpp"));
        assertTrue("minimal registered", client.getAvailableProviders().contains("minimal"));

        // 每个 provider 各跑一遍"直接问答"路径
        for (String code : Arrays.asList("openai", "ollama", "anthropic", "zhipu", "qwen", "llamacpp", "minimal")) {
            ReActAgent agent = new ReActAgent.Builder()
                    .name("agent-" + code)
                    .llmClient(client)
                    .provider(code)
                    .model("m-" + code)
                    .build();
            String reply = agent.run("hello");
            assertNotNull("reply must not be null for " + code, reply);
            assertFalse("reply must not be empty for " + code, reply.isEmpty());
        }
    }

    /**
     * 屏蔽的关键：ReActAgent 跑工具调用时，调用哪个 provider 由 providerCode 决定，
     * ReActAgent 的代码 0 改动即可适配任意 provider。
     */
    @Test
    public void testReAct_toolCallWorksAcrossAllProviders() {
        String[] codes = {"openai", "ollama", "anthropic", "zhipu", "qwen", "llamacpp"};
        for (String code : codes) {
            LlmClient client = new LlmClient();
            // 每个 provider 都有自己的 tool-call 行为
            client.registerProvider(new ToolCallProvider(code, "m-" + code));

            AtomicReference<String> capturedArg = new AtomicReference<>();
            Toolkit tk = new Toolkit();
            tk.registerTool(TestTools.echoTool(), (name, args) -> {
                capturedArg.set(String.valueOf(args.get("message")));
                return "echoed: " + args.get("message");
            });

            ReActAgent agent = new ReActAgent.Builder()
                    .llmClient(client)
                    .provider(code)
                    .model("m-" + code)
                    .toolkit(tk)
                    .maxSteps(3)
                    .build();

            String reply = agent.run("test " + code);
            assertEquals("Final from " + code, reply);
            assertEquals("hi from " + code, capturedArg.get());
        }
    }

    /**
     * 同一 LlmClient 同时挂多 provider：路由隔离。
     */
    @Test
    public void testRouting_providerCodePicksCorrectProvider() {
        LlmClient client = new LlmClient();
        client.registerProvider(new OpenAIStyleProvider());
        client.registerProvider(new AnthropicStyleProvider());
        client.registerProvider(new ZhipuStyleProvider());

        ReActAgent openaiAgent = new ReActAgent.Builder()
                .llmClient(client).provider("openai").model("gpt-4").build();
        ReActAgent claudeAgent = new ReActAgent.Builder()
                .llmClient(client).provider("anthropic").model("claude-3").build();
        ReActAgent glmAgent = new ReActAgent.Builder()
                .llmClient(client).provider("zhipu").model("glm-4").build();

        // 三个 agent 跑同一个 prompt，得到的 reply 应分别体现 provider 特征
        String r1 = openaiAgent.run("hi");
        String r2 = claudeAgent.run("hi");
        String r3 = glmAgent.run("hi");

        assertEquals("OpenAI style reply", r1);
        assertEquals("claude-reply", r2);
        assertEquals("glm-reply", r3);
    }

    /**
     * 未知 provider code 应抛 IllegalArgumentException。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRouting_unknownProviderCodeThrows() throws Exception {
        LlmClient client = new LlmClient();
        client.chat("nonexistent", ChatCompletionsRequest.builder()
                .model("m").messages(Collections.singletonList(ChatMessage.user("hi"))).build());
    }

    /**
     * 不同 provider 透传不同 ChatCompletionsRequest 参数（temperature、maxTokens）的能力。
     */
    @Test
    public void testRequestParamsAreForwarded_uniformlyAcrossProviders() throws Exception {
        LlmClient client = new LlmClient();
        RecordingProvider recorder = new RecordingProvider();
        client.registerProvider(recorder);

        ChatCompletionsRequest req = ChatCompletionsRequest.builder()
                .model("m-test")
                .messages(Collections.singletonList(ChatMessage.user("hi")))
                .temperature(0.31)
                .maxTokens(77)
                .build();
        client.chat("rec", req);

        assertNotNull(recorder.lastRequest);
        assertEquals(Double.valueOf(0.31), recorder.lastRequest.getTemperature());
        assertEquals(Integer.valueOf(77), recorder.lastRequest.getMaxTokens());
    }

    /**
     * OpenAI 风格：tool_call.arguments 是 JSON-encoded String（"{\"a\":2}"）。
     */
    static class OpenAIStyleProvider implements LlmProvider {
        @Override
        public String getProviderCode() {
            return "openai";
        }

        @Override
        public String getDefaultBaseUrl() {
            return "https://api.openai.com/v1";
        }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            // OpenAI 风格：text 响应
            return textResponse("openai-ok", "OpenAI style reply");
        }

        @Override
        public LlmClient.StreamResult streamChat(ChatCompletionsRequest request) {
            LlmClient.StreamResult r = new LlmClient.StreamResult();
            r.setContent("openai-stream");
            r.setDone(true);
            return r;
        }
    }

    /**
     * Ollama 风格：tool_call.arguments 是 JSON object（非字符串）。
     * 响应里 message.content 与 message.tool_calls 平级。
     */
    static class OllamaStyleProvider implements LlmProvider {
        @Override
        public String getProviderCode() {
            return "ollama";
        }

        @Override
        public String getDefaultBaseUrl() {
            return "http://localhost:11434";
        }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            ChatCompletionsResponse r = new ChatCompletionsResponse();
            r.setId("ollama-1");
            r.setModel(request.getModel());
            ChatCompletionsResponse.ChatChoice c = new ChatCompletionsResponse.ChatChoice();
            c.setIndex(0);
            c.setFinishReason("stop");
            ChatMessage m = new ChatMessage("assistant", "ollama-ok");
            c.setMessage(m);
            r.setChoices(Collections.singletonList(c));
            return r;
        }

        @Override
        public LlmClient.StreamResult streamChat(ChatCompletionsRequest request) {
            LlmClient.StreamResult r = new LlmClient.StreamResult();
            r.setContent("ollama-stream");
            r.setDone(true);
            return r;
        }
    }

    // ==================== 测试：每种 Provider 都能被 ReActAgent 使用 ====================

    /**
     * Anthropic Claude 风格：响应里 content 是 blocks 数组，tool_use 是其中一种 block。
     * 我们的 LlmClient 抽象层用 ChatCompletionsResponse，所以这里"假装"是 OpenAI 形态
     * ——但 LlmProvider 内部可能要走自己的转换。屏蔽的关键：LlmProvider 实现负责
     * 协议转换，业务层（ReActAgent）不感知。
     */
    static class AnthropicStyleProvider implements LlmProvider {
        @Override
        public String getProviderCode() {
            return "anthropic";
        }

        @Override
        public String getDefaultBaseUrl() {
            return "https://api.anthropic.com";
        }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            // 模拟"经过内部转换"的响应：原始 Anthropic API 返回 content blocks，
            // 这里我们用 LlmProvider 已经把它翻译成 OpenAI 兼容的 ChatCompletionsResponse
            return textResponse("anthropic-1", "claude-reply");
        }

        @Override
        public LlmClient.StreamResult streamChat(ChatCompletionsRequest request) {
            LlmClient.StreamResult r = new LlmClient.StreamResult();
            r.setContent("claude-stream");
            r.setDone(true);
            return r;
        }
    }

    /**
     * 智谱 GLM 风格：OpenAI 兼容，但有自己 base_url。
     */
    static class ZhipuStyleProvider implements LlmProvider {
        @Override
        public String getProviderCode() {
            return "zhipu";
        }

        @Override
        public String getDefaultBaseUrl() {
            return "https://open.bigmodel.cn/api/paas/v4";
        }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            return textResponse("glm-4-1", "glm-reply");
        }

        @Override
        public LlmClient.StreamResult streamChat(ChatCompletionsRequest request) {
            LlmClient.StreamResult r = new LlmClient.StreamResult();
            r.setContent("glm-stream");
            r.setDone(true);
            return r;
        }
    }

    /**
     * 通义千问 DashScope 风格：OpenAI 兼容。
     */
    static class QwenStyleProvider implements LlmProvider {
        @Override
        public String getProviderCode() {
            return "qwen";
        }

        @Override
        public String getDefaultBaseUrl() {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            return textResponse("qwen-1", "qwen-reply");
        }

        @Override
        public LlmClient.StreamResult streamChat(ChatCompletionsRequest request) {
            LlmClient.StreamResult r = new LlmClient.StreamResult();
            r.setContent("qwen-stream");
            r.setDone(true);
            return r;
        }
    }

    /**
     * 本地 llama.cpp 风格：自定义协议，content 在 delta.text 字段。
     */
    static class LlamaCppStyleProvider implements LlmProvider {
        @Override
        public String getProviderCode() {
            return "llamacpp";
        }

        @Override
        public String getDefaultBaseUrl() {
            return "http://localhost:8080";
        }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            return textResponse("llama-1", "llama-reply");
        }

        @Override
        public LlmClient.StreamResult streamChat(ChatCompletionsRequest request) {
            LlmClient.StreamResult r = new LlmClient.StreamResult();
            r.setContent("llama-stream");
            r.setDone(true);
            return r;
        }
    }

    /**
     * 极简自定义 provider：tool_call.arguments 是 String（与 OpenAI 一致），
     * 但响应里没有任何 id/usage/finishReason——只返回 content。
     * 验证 LlmClient 抽象层能否容忍"残缺"响应。
     */
    static class MinimalCustomProvider implements LlmProvider {
        @Override
        public String getProviderCode() {
            return "minimal";
        }

        @Override
        public String getDefaultBaseUrl() {
            return "http://localhost:9999";
        }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            ChatCompletionsResponse r = new ChatCompletionsResponse();
            r.setModel(request.getModel());
            ChatCompletionsResponse.ChatChoice c = new ChatCompletionsResponse.ChatChoice();
            c.setIndex(0);
            ChatMessage m = new ChatMessage("assistant", "minimal-reply");
            c.setMessage(m);
            r.setChoices(Collections.singletonList(c));
            // 不设 id / usage / finishReason
            return r;
        }

        @Override
        public LlmClient.StreamResult streamChat(ChatCompletionsRequest request) {
            LlmClient.StreamResult r = new LlmClient.StreamResult();
            r.setContent("minimal-stream");
            r.setDone(true);
            return r;
        }
    }

    /**
     * Tool-call 风格的 provider：第一轮返回 tool_call，第二轮返回 final text。
     * 模拟"该 provider 真的能驱动 ReAct 工具调用"——但所有 provider 共享这个实现，
     * 区别只在 providerCode（路由用）。
     */
    static class ToolCallProvider implements LlmProvider {
        private final String code;
        private final String model;
        private final AtomicInteger callIdx = new AtomicInteger(0);

        ToolCallProvider(String code, String model) {
            this.code = code;
            this.model = model;
        }

        @Override
        public String getProviderCode() {
            return code;
        }

        @Override
        public String getDefaultBaseUrl() {
            return "http://test/" + code;
        }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            int idx = callIdx.getAndIncrement();
            if (idx == 0) {
                // 第一步：发 tool_call
                ToolCall tc = new ToolCall("call_" + code + "_1", "echo", "{\"message\":\"hi from " + code + "\"}");
                ChatCompletionsResponse r = new ChatCompletionsResponse();
                r.setId("tc-" + code);
                r.setModel(model);
                ChatCompletionsResponse.ChatChoice c = new ChatCompletionsResponse.ChatChoice();
                c.setIndex(0);
                c.setFinishReason("tool_calls");
                ChatMessage m = new ChatMessage("assistant", "");
                m.setToolCalls(Collections.singletonList(tc));
                c.setMessage(m);
                r.setChoices(Collections.singletonList(c));
                return r;
            }
            // 第二步：final text
            return textResponse(model, "Final from " + code);
        }

        @Override
        public LlmClient.StreamResult streamChat(ChatCompletionsRequest request) {
            LlmClient.StreamResult r = new LlmClient.StreamResult();
            r.setContent("stream-" + code);
            r.setDone(true);
            return r;
        }
    }

    // ==================== 工具：构造 ChatCompletionsResponse ====================

    /**
     * 记录最后一次请求参数的 provider。
     */
    static class RecordingProvider implements LlmProvider {
        ChatCompletionsRequest lastRequest;

        @Override
        public String getProviderCode() {
            return "rec";
        }

        @Override
        public String getDefaultBaseUrl() {
            return "http://r";
        }

        @Override
        public ChatCompletionsResponse chat(ChatCompletionsRequest request) {
            this.lastRequest = request;
            return textResponse("m", "ok");
        }

        @Override
        public LlmClient.StreamResult streamChat(ChatCompletionsRequest request) {
            LlmClient.StreamResult r = new LlmClient.StreamResult();
            r.setContent("rec-stream");
            r.setDone(true);
            return r;
        }
    }
}
