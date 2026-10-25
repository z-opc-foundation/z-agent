package com.zifang.z.agent.llm.infrastructure.llm.engine;

import com.zifang.z.agent.llm.infrastructure.events.Message;
import com.zifang.z.agent.llm.infrastructure.llm.context.LlmStreamRequest;
import com.zifang.z.agent.llm.infrastructure.llm.provider.define.LlmStreamProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LlmReactEngine} 的「思考 / 正文分通道」漏斗行为锁定。
 *
 * <p>漏斗链：provider → {@code StreamCallbacks.onToken/onThought} → {@code StreamHandler.onText/onThought}。
 *
 * <p><b>关键口径</b>：老协议下思考以 {@code "[REASONING:]思考"} 混进 {@code onToken} ⇒
 * {@link LlmReactEngine#chat(List, LlmStreamRequest)} 返回的字符串里带着思考碎片
 * ⇒ 下游（z-team 群聊派单）直接拿它解析 JSON 必失败。
 * 现在思考走 {@code onThought} ⇒ 返回值恒为纯正文。
 */
class LlmReactEngineThoughtChannelTest {

    private static final String DISPATCH_JSON = "{\"dispatches\":[{\"target\":\"智能助手\"}]}";

    @Test
    @DisplayName("chat() 返回纯正文：思考不进返回值（老协议下会带 [REASONING:] 碎片）")
    void chatReturnsPlainTextOnly() {
        LlmReactEngine engine = new LlmReactEngine("test", new FakeProvider());

        String out = engine.chat(Collections.singletonList(Message.user("派单")), null);

        assertEquals(DISPATCH_JSON, out, "chat() 返回值必须是纯正文（可直接 JSON 解析）");
        assertFalse(out.contains("[REASONING:]"), "返回值不得含 legacy 行内推理标记");
        assertFalse(out.contains("让我想想"), "返回值不得混入思考碎片");
    }

    @Test
    @DisplayName("stream()：正文走 onText，思考走 onThought（两条通道互不掺杂）")
    void streamSeparatesTextAndThought() {
        LlmReactEngine engine = new LlmReactEngine("test", new FakeProvider());

        final StringBuilder text = new StringBuilder();
        final StringBuilder thought = new StringBuilder();
        final boolean[] completed = {false};

        engine.stream(Collections.singletonList(Message.user("派单")), null,
                new LlmReactEngine.StreamHandler() {
                    @Override
                    public void onText(String t) {
                        text.append(t);
                    }

                    @Override
                    public void onThought(String th) {
                        thought.append(th);
                    }

                    @Override
                    public void onComplete() {
                        completed[0] = true;
                    }

                    @Override
                    public void onError(Throwable t) {
                    }
                });

        assertEquals(DISPATCH_JSON, text.toString(), "正文流必须是纯正文");
        assertEquals("让我想想", thought.toString(), "思考必须走独立通道");
        assertTrue(completed[0]);
    }

    @Test
    @DisplayName("既有 StreamHandler 实现方（不实现 onThought）零改动：正文流依然干净")
    void legacyStreamHandlerKeepsCleanText() {
        LlmReactEngine engine = new LlmReactEngine("test", new FakeProvider());

        final StringBuilder text = new StringBuilder();
        engine.stream(Collections.singletonList(Message.user("派单")), null,
                new LlmReactEngine.StreamHandler() {
                    @Override
                    public void onText(String t) {
                        text.append(t);
                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable t) {
                    }
                });

        assertEquals(DISPATCH_JSON, text.toString(),
                "既有实现方（未实现 onThought）拿到的正文流同样是纯正文");
    }

    /**
     * 假 provider：模拟上游「交替下发 content / reasoning_content」。
     */
    private static final class FakeProvider implements LlmStreamProvider {

        /** 按上游到达顺序排的动作。 */
        private final List<Step> script = new ArrayList<>();

        FakeProvider() {
            script.add(Step.text("{\"dispatches\":[{\"tar"));
            script.add(Step.thought("让我想想"));
            script.add(Step.text("get\":\"智能助手\"}]}"));
        }

        @Override
        public void stream(String prompt, String systemPrompt, StreamCallbacks callbacks) {
            for (Step step : script) {
                if (step.thought) {
                    callbacks.onThought(step.text);
                } else {
                    callbacks.onToken(step.text);
                }
            }
            callbacks.onComplete(DISPATCH_JSON);
        }

        @Override
        public void stop() {
        }
    }

    /** 一个上游增量（正文 or 思考）。 */
    private static final class Step {
        final boolean thought;
        final String text;

        private Step(boolean thought, String text) {
            this.thought = thought;
            this.text = text;
        }

        static Step text(String text) {
            return new Step(false, text);
        }

        static Step thought(String text) {
            return new Step(true, text);
        }
    }
}
