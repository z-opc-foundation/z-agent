package com.zifang.z.agent.llm.infrastructure.llm.provider.openaicompatible;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UpstreamThinkingTagSplitter} 的行为锁定。
 *
 * <p><b>为什么锁它</b>（2026-10-22 生产实测）：MiniMax-M2.7 这类模型不提供独立的
 * {@code reasoning_content}，而是把思考以裸标签塞进正文
 * （{@code "<think>We need comply…</think>\n\n收到"}）⇒ 思考被当正文推给前端
 * ⇒ <b>主群直接显示思考过程</b>。本类把这类内容分流到思考通道。
 *
 * <p>断言口径：<b>正文段绝不含标签、绝不含思考内容</b>；
 * 思考段绝不含标签本身。
 */
class UpstreamThinkingTagSplitterTest {

    // ── 无标签：原样透传 ──

    @Test
    @DisplayName("无标签内容原样进正文")
    void plainText() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split out = s.split("收到");
        assertEquals("收到", out.getText());
        assertEquals("", out.getReasoning());
    }

    @Test
    @DisplayName("正文里的裸 < 与 HTML 片段不误伤")
    void doesNotDamageAngleBrackets() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split out = s.split("a < b 且 x <div>y</div>");
        assertEquals("a < b 且 x <div>y</div>", out.getText());
        assertEquals("", out.getReasoning());
    }

    // ── 同段内开闭（MiniMax-M2.7 真实形态）──

    @Test
    @DisplayName("同一段内 <think>…</think>：思考进思考段，正文只剩正文")
    void thinkAndTextInSameChunk() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split out =
                s.split("<think>We need comply exactly two Chinese chars.</think>\n\n收到");
        assertEquals("\n\n收到", out.getText());
        assertEquals("We need comply exactly two Chinese chars.", out.getReasoning());
    }

    @Test
    @DisplayName("只有闭标签（未开）时标签被丢弃，内容留正文")
    void strayClosingTag() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split out = s.split("</think>正文");
        assertEquals("正文", out.getText());
        assertEquals("", out.getReasoning());
    }

    // ── 跨增量（标签被切开）──

    @Test
    @DisplayName("开标签跨增量：<thi + nk> 被识别为开标签")
    void openTagSplitAcrossChunks() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split a = s.split("<thi");
        assertEquals("", a.getText(), "残片不得当正文吐出去");
        assertEquals("", a.getReasoning());

        UpstreamThinkingTagSplitter.Split b = s.split("nk>思考中");
        assertEquals("", b.getText());
        assertEquals("思考中", b.getReasoning());
    }

    @Test
    @DisplayName("闭标签跨增量：</think> 被切开也不漏思考")
    void closeTagSplitAcrossChunks() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        // 思考内容随**本次增量**返回（不是攒到最后一次）
        UpstreamThinkingTagSplitter.Split a = s.split("<think>思考");
        assertEquals("", a.getText());
        assertEquals("思考", a.getReasoning());

        UpstreamThinkingTagSplitter.Split b = s.split("</think>正文");
        assertEquals("正文", b.getText());
        assertEquals("", b.getReasoning());
    }

    @Test
    @DisplayName("闭标签被切成 </thi + nk> 也不漏")
    void closeTagSplitMidway() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split a0 = s.split("<think>思考");
        assertEquals("思考", a0.getReasoning());

        UpstreamThinkingTagSplitter.Split a = s.split("</thi");
        assertEquals("", a.getText());
        assertEquals("", a.getReasoning(), "残片不得当思考吐出去");

        UpstreamThinkingTagSplitter.Split b = s.split("nk>正文");
        assertEquals("正文", b.getText());
        assertEquals("", b.getReasoning());
    }

    // ── 未闭合 ──

    @Test
    @DisplayName("未闭合的思考块：全程算思考，不作为正文外泄")
    void unclosedThinkingBlock() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split a = s.split("<think>模型被截断");
        assertEquals("", a.getText(), "未闭合的思考不得进正文");
        assertEquals("模型被截断", a.getReasoning());
        assertTrue(s.isInThinking());

        // flush 只交还**残片**（思考本体已随各次增量返回，不在这里重复）
        UpstreamThinkingTagSplitter.Split tail = s.flush();
        assertEquals("", tail.getText());
        assertEquals("", tail.getReasoning());
    }

    @Test
    @DisplayName("残片在流结束时按当前状态归位")
    void flushReturnsResidue() {
        UpstreamThinkingTagSplitter s1 = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split a1 = s1.split("abc<");
        assertEquals("abc", a1.getText());
        assertEquals("<", s1.flush().getText(), "文本态的残片归正文");

        UpstreamThinkingTagSplitter s2 = new UpstreamThinkingTagSplitter();
        s2.split("<think>思考<");
        assertEquals("<", s2.flush().getReasoning(), "思考态的残片归思考");
    }

    // ── 变体与大小写 ──

    @Test
    @DisplayName("thinking / reasoning 变体同样识别")
    void tagVariants() {
        UpstreamThinkingTagSplitter s1 = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split o1 = s1.split("<thinking>abc</thinking>正文");
        assertEquals("正文", o1.getText());
        assertEquals("abc", o1.getReasoning());

        UpstreamThinkingTagSplitter s2 = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split o2 = s2.split("<reasoning>abc</reasoning>正文");
        assertEquals("正文", o2.getText());
        assertEquals("abc", o2.getReasoning());
    }

    @Test
    @DisplayName("大小写不敏感")
    void caseInsensitive() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split out = s.split("<THINK>abc</THINK>正文");
        assertEquals("正文", out.getText());
        assertEquals("abc", out.getReasoning());
    }

    // ── 边界 ──

    @Test
    @DisplayName("空输入不炸")
    void emptyInput() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        assertFalse(s.split(null).hasText());
        assertFalse(s.split("").hasText());
        assertFalse(s.flush().hasText());
    }

    @Test
    @DisplayName("连续多个思考块都能切开")
    void multipleBlocks() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split out = s.split("<think>一</think>A<think>二</think>B");
        assertEquals("AB", out.getText());
        assertEquals("一二", out.getReasoning());
    }

    @Test
    @DisplayName("思考段里含 < 也不误伤")
    void angleBracketInsideThinking() {
        UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
        UpstreamThinkingTagSplitter.Split out = s.split("<think>比较 a < b</think>结论");
        assertEquals("结论", out.getText());
        assertEquals("比较 a < b", out.getReasoning());
    }

    // ── 2026-10-28 生产事故回归：MAX_TAG_LEN 短于 </thinking> ⇒ 流式分块下闭标签永远匹配不上 ──

    /**
     * 事故复现：glm-4-flash 按 1~3 字符/块下发，回复为
     * {@code <thinking>…</thinking>{json}}。旧实现 {@code MAX_TAG_LEN=10} 盖不住
     * 11 字符的 {@code </thinking>} —— 残片缓冲被钳到 9 时前导 {@code '<'} 被冲进
     * 已出流的内容里，残片变成 {@code "/thinking"} ⇒ 闭标签永远匹配不上 ⇒
     * {@code inThinking} 永真 ⇒ 闭标签后的全部正文被当思考 ⇒ 观察者"零内容完成"。
     * 本用例锁定：任意分块粒度下正文/思考都必须完整归位。
     */
    @Test
    @DisplayName("回归：11 字符闭标签 </thinking> 在 1~11 字符分块下都能正确闭合")
    void streamingCloseTagSurvivesArbitraryChunking() {
        String thinking = "该消息@了测试员，要求回复收到，属于driving_action。";
        String body = "{\"dispatches\":[],\"notify_creator\":false}";
        String full = "<thinking>" + thinking + "</thinking>\n" + body;
        for (int step = 1; step <= 11; step++) {
            UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
            StringBuilder text = new StringBuilder();
            StringBuilder reasoning = new StringBuilder();
            for (int i = 0; i < full.length(); i += step) {
                UpstreamThinkingTagSplitter.Split out =
                        s.split(full.substring(i, Math.min(full.length(), i + step)));
                text.append(out.getText());
                reasoning.append(out.getReasoning());
            }
            UpstreamThinkingTagSplitter.Split tail = s.flush();
            text.append(tail.getText());
            reasoning.append(tail.getReasoning());
            assertEquals("\n" + body, text.toString(), "step=" + step + " 时正文丢失/被污染");
            assertEquals(thinking, reasoning.toString(), "step=" + step + " 时思考段不完整");
            assertFalse(s.isInThinking(), "step=" + step + " 流结束后仍处于思考态");
        }
    }

    @Test
    @DisplayName("回归：12 字符闭标签 </reasoning> 同样能正确闭合")
    void streamingLongestCloseTagSurvivesChunking() {
        String full = "<reasoning>思考</reasoning>正文";
        for (int step = 1; step <= 12; step++) {
            UpstreamThinkingTagSplitter s = new UpstreamThinkingTagSplitter();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < full.length(); i += step) {
                text.append(s.split(full.substring(i, Math.min(full.length(), i + step))).getText());
            }
            text.append(s.flush().getText());
            assertEquals("正文", text.toString(), "step=" + step + " 时正文丢失/被污染");
        }
    }
}
