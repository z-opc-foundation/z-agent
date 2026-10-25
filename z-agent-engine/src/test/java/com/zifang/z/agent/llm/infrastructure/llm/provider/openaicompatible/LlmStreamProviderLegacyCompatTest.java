package com.zifang.z.agent.llm.infrastructure.llm.provider.openaicompatible;

import com.zifang.z.agent.llm.infrastructure.llm.provider.define.LlmStreamProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 「思考 / 正文分通道」的<b>兼容面</b>行为锁定。
 *
 * <p>协议形态：
 * <ul>
 *   <li>{@link LlmStreamProvider.StreamCallbacks#onThought(String)} 是 <b>default 方法</b>
 *       ⇒ 既有实现方（只实现 onToken/onComplete/onError）<b>零改动即可编译</b>；</li>
 *   <li>default 实现 = <b>丢弃</b>思考 ⇒ 既有消费方拿到的正文流恒为纯正文（JSON 可解析）；</li>
 *   <li>{@link LlmStreamProvider#legacyInlineReasoning(StreamCallbacks)} 兼容层
 *       ⇒ 确实依赖老协议（正文里带无闭合 {@code "[REASONING:]"} 标记）的消费方可原样拿回老形态。</li>
 * </ul>
 */
class LlmStreamProviderLegacyCompatTest {

    @Test
    @DisplayName("default onThought = 丢弃思考：既有消费方正文流保持纯正文")
    void defaultOnThoughtDropsReasoning() {
        final StringBuilder text = new StringBuilder();
        LlmStreamProvider.StreamCallbacks cb = new LlmStreamProvider.StreamCallbacks() {
            @Override
            public void onToken(String token) {
                text.append(token);
            }

            @Override
            public void onComplete(String fullText) {
            }

            @Override
            public void onError(Throwable error) {
            }
        };

        cb.onToken("{\"dispatches\":[]}");
        cb.onThought("内部思考");

        assertEquals("{\"dispatches\":[]}", text.toString(),
                "默认不得把思考混进正文流");
    }

    @Test
    @DisplayName("兼容层 legacyInlineReasoning ⇒ 老协议形态原样回归")
    void legacyCompatLayerReplaysOldShape() {
        final StringBuilder text = new StringBuilder();
        final StringBuilder completed = new StringBuilder();
        final Throwable[] err = {null};

        LlmStreamProvider.StreamCallbacks raw = new LlmStreamProvider.StreamCallbacks() {
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

        LlmStreamProvider.StreamCallbacks compat = LlmStreamProvider.legacyInlineReasoning(raw);
        assertNull(err[0]);

        compat.onToken("{\"dispatches\":[]}");
        compat.onThought("内部思考");
        compat.onComplete("{\"dispatches\":[]}");

        assertEquals("{\"dispatches\":[]}[REASONING:]内部思考", text.toString(),
                "兼容层必须复刻老协议形态");
        assertEquals("{\"dispatches\":[]}", completed.toString(),
                "onComplete 的 fullText 不受兼容层影响");
        assertNull(err[0]);
    }

    @Test
    @DisplayName("兼容层对 null 委托返回 null（调用方自行判空）")
    void legacyCompatLayerOnNullDelegate() {
        assertNull(LlmStreamProvider.legacyInlineReasoning(null));
    }

    @Test
    @DisplayName("legacy 前缀常量与老协议逐字一致")
    void legacyPrefixConstant() {
        assertEquals("[REASONING:]", LlmStreamProvider.LEGACY_INLINE_REASONING_PREFIX);
    }
}
