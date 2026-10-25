package com.zifang.z.agent.llm.infrastructure.llm.provider.openaicompatible;

import com.zifang.z.agent.llm.infrastructure.llm.provider.define.LlmStreamProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link OpenAiCompletionsStreamProvider} 单块解析器（{@code handleChunk} / {@code flushTail}）的单元锁定。
 *
 * <p>与 {@link OpenAiCompletionsStreamProviderThoughtChannelTest} 的分工：那边走**真实 SSE** 验端到端，
 * 这边直接驱动解析器，锁「一个增量块 ⇒ 正文/思考各归其位」的细粒度规则（无需起网络）。
 */
class OpenAiCompletionsStreamProviderParserTest {

    @Test
    @DisplayName("reasoning_content ⇒ 思考通道；content ⇒ 正文通道")
    void deltaSplitsIntoTwoChannels() {
        Recorder rec = new Recorder();
        OpenAiCompletionsStreamProvider provider = newProvider();
        OpenAiCompletionsStreamProvider.StreamState state = new OpenAiCompletionsStreamProvider.StreamState();

        provider.handleChunk("{\"choices\":[{\"delta\":{\"content\":\"{\\\"a\\\":1}\"}}]}",
                rec.callbacks(), state);
        provider.handleChunk("{\"choices\":[{\"delta\":{\"reasoning_content\":\"想一下\"}}]}",
                rec.callbacks(), state);

        assertEquals("{\"a\":1}", rec.text());
        assertEquals("想一下", rec.thought());
    }

    @Test
    @DisplayName("裸思考标签（<think>…</think>）在同一个增量块里也能正确分流")
    void bareThinkTagInsideOneChunk() {
        Recorder rec = new Recorder();
        OpenAiCompletionsStreamProvider provider = newProvider();
        OpenAiCompletionsStreamProvider.StreamState state = new OpenAiCompletionsStreamProvider.StreamState();

        provider.handleChunk("{\"choices\":[{\"delta\":{\"content\":\"<think>先算</think>答案\"}}]}",
                rec.callbacks(), state);

        assertEquals("答案", rec.text());
        assertEquals("先算", rec.thought());
    }

    @Test
    @DisplayName("尾部残片：思考态残片归思考、正文态残片归正文（flushTail）")
    void flushTailRoutesRemainder() {
        Recorder rec = new Recorder();
        OpenAiCompletionsStreamProvider provider = newProvider();
        OpenAiCompletionsStreamProvider.StreamState state = new OpenAiCompletionsStreamProvider.StreamState();

        // 正文态残片：末尾是「可能是标签前缀」的碎片 ⇒ 增量里先扣住，流结束时 flush 回正文
        provider.handleChunk("{\"choices\":[{\"delta\":{\"content\":\"正文<thi\"}}]}",
                rec.callbacks(), state);
        assertEquals("正文", rec.text(), "增量里只吐已确认的正文");
        assertEquals("", rec.thought());

        provider.flushTail(rec.callbacks(), state);
        assertEquals("正文<thi", rec.text(), "流结束的碎片归位到正文，不得丢");
        assertEquals("", rec.thought());

        // 思考态残片：思考块未闭合，流结束时剩余算思考
        Recorder rec2 = new Recorder();
        OpenAiCompletionsStreamProvider.StreamState state2 = new OpenAiCompletionsStreamProvider.StreamState();
        provider.handleChunk("{\"choices\":[{\"delta\":{\"content\":\"<think>想了一半\"}}]}",
                rec2.callbacks(), state2);
        provider.flushTail(rec2.callbacks(), state2);
        assertEquals("", rec2.text(), "思考不得外泄到正文");
        assertEquals("想了一半", rec2.thought());
    }

    @Test
    @DisplayName("accumulated（onComplete 的 fullText）只含正文")
    void accumulatedHoldsPlainTextOnly() {
        Recorder rec = new Recorder();
        OpenAiCompletionsStreamProvider provider = newProvider();
        OpenAiCompletionsStreamProvider.StreamState state = new OpenAiCompletionsStreamProvider.StreamState();

        provider.handleChunk("{\"choices\":[{\"delta\":{\"content\":\"{\\\"dispatches\\\":[\"}}]}",
                rec.callbacks(), state);
        provider.handleChunk("{\"choices\":[{\"delta\":{\"reasoning_content\":\"插进来\"}}]}",
                rec.callbacks(), state);
        provider.handleChunk("{\"choices\":[{\"delta\":{\"content\":\"]}\"}}]}",
                rec.callbacks(), state);

        assertEquals("{\"dispatches\":[]}", state.accumulated.toString());
        assertFalse(state.accumulated.toString().contains("[REASONING:]"));
        assertEquals("{\"dispatches\":[]}", rec.text());
        assertEquals("插进来", rec.thought());
    }

    // ── helpers ──

    private static OpenAiCompletionsStreamProvider newProvider() {
        return new OpenAiCompletionsStreamProvider("http://127.0.0.1:1", "test-key-not-a-secret", "test-model");
    }

    private static final class Recorder {
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder thought = new StringBuilder();

        String text() {
            return text.toString();
        }

        String thought() {
            return thought.toString();
        }

        LlmStreamProvider.StreamCallbacks callbacks() {
            return new LlmStreamProvider.StreamCallbacks() {
                @Override
                public void onToken(String token) {
                    text.append(token);
                }

                @Override
                public void onThought(String th) {
                    thought.append(th);
                }

                @Override
                public void onComplete(String fullText) {
                }

                @Override
                public void onError(Throwable error) {
                }
            };
        }
    }
}
