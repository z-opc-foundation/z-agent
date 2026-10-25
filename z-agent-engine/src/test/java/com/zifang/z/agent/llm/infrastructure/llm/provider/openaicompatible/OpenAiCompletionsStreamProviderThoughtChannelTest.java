package com.zifang.z.agent.llm.infrastructure.llm.provider.openaicompatible;

import com.sun.net.httpserver.HttpServer;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonObject;
import com.zifang.z.agent.llm.infrastructure.llm.provider.define.LlmStreamProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OpenAiCompletionsStreamProvider} 的「思考 / 正文分通道」行为锁定。
 *
 * <p><b>为什么锁它</b>（2026-10-25 生产实测）：老协议把上游思考以无闭合的 {@code "[REASONING:]"}
 * 前缀塞进正文流，而上游 content / reasoning_content <b>交替</b>下发 ⇒ 思考块会插进 JSON 的键名中间
 * （实测原文：{@code {"dispa[REASONING:]。tches":[{"target":"智能助手",…}]}}）⇒ 标记不携带边界、
 * 事后无法还原 ⇒ 下游 JSON 解析必失败（{@code IllegalArgumentException: Invalid key}）⇒ 群聊零回复。
 *
 * <p><b>断言口径</b>：<b>无论上游是否交替下发 content/reasoning_content，下游收到的正文流都必须是完整 JSON</b>
 * （不含思考碎片、不被切开）。
 *
 * <p><b>本类只驱动公开 API</b>（{@code provider.stream(...)} + 真实 SSE 服务），且只引用
 * <b>老协议就存在</b>的 API ⇒ 同一份测试在「老实现（思考与正文合并成一条流）」下编译得过、
 * 跑起来<b>红</b>；在新实现下<b>绿</b>。
 *
 * @see LlmStreamProviderLegacyCompatTest 兼容面（default 丢弃 + legacyInlineReasoning 兼容层）
 */
class OpenAiCompletionsStreamProviderThoughtChannelTest {

    /** 生产实测的派单 JSON（会被上游切成多段下发）。 */
    private static final String DISPATCH_JSON =
            "{\"dispatches\":[{\"target\":\"智能助手\",\"instruction\":\"做一下\"}],\"notify_creator\":false}";

    // ── 判定标准：正文流必须是完整 JSON ──

    @Test
    @DisplayName("上游交替下发 content/reasoning_content ⇒ 正文流仍是完整 JSON")
    void alternatingContentAndReasoningKeepsJsonIntact() throws Exception {
        // 上游把 JSON 切碎，并在**键名中间**交替下发思考（生产实测形态）
        List<String> chunks = new ArrayList<>();
        chunks.add(content("{\"dispatches\":[{\"tar"));
        chunks.add(reasoning("让我想想该派给谁"));
        chunks.add(content("get\":\"智能助手\",\"instruction\":\"做一下\"}]"));
        chunks.add(reasoning("再确认一下 notify_creator"));
        chunks.add(content(",\"notify_creator\":false}"));

        Recorder rec = streamThroughSse(chunks);

        assertEquals(DISPATCH_JSON, rec.text(),
                "正文流必须是完整 JSON：思考不得插进键名/值中间");
        assertFalse(rec.text().contains("[REASONING:]"),
                "正文流不得再出现 legacy 行内推理标记");
        assertFalse(rec.text().contains("让我想想"),
                "正文流不得混入思考碎片");

        JsonObject parsed = JsonUtil.parseObject(rec.text());
        assertNotNull(parsed);
        assertTrue(parsed.containsKey("dispatches"), "JSON 必须可解析且含 dispatches 键");
    }

    @Test
    @DisplayName("同一段 JSON 全部走 content（上游不交替）⇒ 正文流同样完整")
    void plainContentOnlyKeepsJsonIntact() throws Exception {
        List<String> chunks = new ArrayList<>();
        chunks.add(content("{\"dispatches\":[{\"target\":"));
        chunks.add(content("\"智能助手\",\"instruction\":\"做一下\"}],\"notify_creator\":false}"));

        Recorder rec = streamThroughSse(chunks);

        assertEquals(DISPATCH_JSON, rec.text());
        assertNotNull(JsonUtil.parseObject(rec.text()));
    }

    @Test
    @DisplayName("上游把思考以裸标签塞进 content（MiniMax-M2.7 形态）⇒ 思考走思考通道，正文仍是完整 JSON")
    void bareThinkTagInsideContentGoesToThoughtChannel() throws Exception {
        List<String> chunks = new ArrayList<>();
        chunks.add(content("<think>先算一下人数</think>{\"dispatches\":[{\"target\":\"智能助手\"}]}"));
        chunks.add(reasoning("还要确认参数"));

        Recorder rec = streamThroughSse(chunks);

        assertEquals("{\"dispatches\":[{\"target\":\"智能助手\"}]}", rec.text(),
                "裸思考标签不得留在正文流里");
        assertFalse(rec.text().contains("先算一下人数"));
    }

    @Test
    @DisplayName("思考通道收到完整思考（与正文互不掺杂）")
    void thoughtChannelCarriesReasoning() throws Exception {
        List<String> chunks = new ArrayList<>();
        chunks.add(reasoning("先读题"));
        chunks.add(content("{\"dispatches\":[]}"));
        chunks.add(reasoning("再作答"));

        Recorder rec = streamThroughSse(chunks);

        assertEquals("{\"dispatches\":[]}", rec.text());
        assertEquals("先读题再作答", rec.thought(),
                "思考应完整落在独立通道，不被正文流切碎");
    }

    @Test
    @DisplayName("既有消费方（只实现 onToken/onComplete/onError）零改动：正文流干净、思考默认丢弃")
    void legacyConsumerKeepsCompilingAndGetsCleanText() throws Exception {
        List<String> chunks = new ArrayList<>();
        chunks.add(content("{\"dispatches\":[]}"));
        chunks.add(reasoning("内部思考"));

        // 注意：本回调类**只**实现老协议三个方法（模拟 z-team 等既有消费方），
        // 编译期即可证明「既有实现方零改动可编译」
        final StringBuilder text = new StringBuilder();
        final StringBuilder completed = new StringBuilder();
        final Throwable[] err = {null};

        LlmStreamProvider.StreamCallbacks legacyCallbacks = new LlmStreamProvider.StreamCallbacks() {
            @Override
            public void onToken(String token) {
                text.append(token);
            }

            @Override
            public void onComplete(String fullText) {
                completed.append(fullText);
            }

            @Override
            public void onError(Throwable error) {
                err[0] = error;
            }
        };

        runProvider(chunks, legacyCallbacks);

        assertNull(err[0], "SSE 流不应报错");
        assertEquals("{\"dispatches\":[]}", text.toString(),
                "既有消费方拿到的正文流必须是纯正文");
        assertEquals("{\"dispatches\":[]}", completed.toString(),
                "onComplete 的 fullText 同样只含正文");
    }

    // ── 基础设施 ──

    /** 正文 / 思考两个通道的记录器。 */
    private static final class Recorder {
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder thought = new StringBuilder();

        String text() {
            return text.toString();
        }

        String thought() {
            return thought.toString();
        }
    }

    private static Recorder streamThroughSse(List<String> chunks) throws Exception {
        final Recorder rec = new Recorder();
        runProvider(chunks, new LlmStreamProvider.StreamCallbacks() {
            @Override
            public void onToken(String token) {
                rec.text.append(token);
            }

            // 故意**不加** @Override：老协议接口里没有 onThought，本类仍要能编译
            // （老实现下该方法永不被调用 ⇒ 思考会混进正文流 ⇒ 断言红）
            public void onThought(String thought) {
                rec.thought.append(thought);
            }

            @Override
            public void onComplete(String fullText) {
            }

            @Override
            public void onError(Throwable error) {
                throw new IllegalStateException("SSE 流失败", error);
            }
        });
        return rec;
    }

    /**
     * 起一个只吐 SSE 的本地服务，把 {@code chunks} 当作增量块依次下发，然后关闭流。
     */
    private static void runProvider(List<String> chunks, LlmStreamProvider.StreamCallbacks callbacks)
            throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            // 请求体必须读完，否则客户端写 body 时可能收到 broken pipe
            try (InputStream in = exchange.getRequestBody()) {
                ByteArrayOutputStream sink = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) {
                    sink.write(buf, 0, n);
                }
            } catch (IOException ignore) {
                // 忽略：只关心响应侧
            }
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=utf-8");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                for (String chunk : chunks) {
                    os.write(("data: " + chunk + "\n\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
                os.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException ignore) {
                // 忽略：客户端可能已断开
            }
            exchange.close();
        });
        server.start();
        try {
            OpenAiCompletionsStreamProvider provider = new OpenAiCompletionsStreamProvider(
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "test-key-not-a-secret",
                    "test-model");
            provider.stream("prompt", "system", callbacks);
        } finally {
            server.stop(0);
        }
    }

    private static String content(String text) {
        return "{\"choices\":[{\"delta\":{\"content\":" + jsonString(text) + "}}]}";
    }

    private static String reasoning(String text) {
        return "{\"choices\":[{\"delta\":{\"reasoning_content\":" + jsonString(text) + "}}]}";
    }

    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
