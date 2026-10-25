package com.zifang.z.agent.llm.infrastructure.llm.provider.openaicompatible;

import java.util.Locale;

/**
 * 上游模型「把思考过程混进 {@code content}」的兼容层。
 *
 * <p><b>问题（2026-10-22 生产实测）</b>：部分模型（实测 MiniMax-M2.7）不提供独立的
 * {@code reasoning_content} 字段，而是把思考过程以 <b>裸标签</b>的形式塞进正文：
 * <pre>{@code {"content":"<think>We need comply exactly two Chinese chars.</think>\n\n收到"}}</pre>
 * ⇒ 这段思考被当作<b>正文</b>推给前端 ⇒ <b>主群直接显示思考过程</b>（用户可见的泄漏）。
 *
 * <p><b>为什么不能靠出口剥除</b>：正文是<b>流式</b>推的，出口剥除只能修「落库/展示」，
 * 修不了「已经推给前端的增量」⇒ 必须在<b>解析 content 的那一刻</b>把思考分流走。
 *
 * <p><b>本类做且只做一件事</b>：把一段 content 增量切成
 * {@link Split#getText() 正文}与 {@link Split#getReasoning() 思考}两段。
 * <b>不负责协议</b>：思考怎么往外送由调用方决定
 * （z-agent 侧统一走
 * {@link com.zifang.z.agent.llm.infrastructure.llm.provider.define.LlmStreamProvider.StreamCallbacks#onThought(String)}
 * 独立通道；老的 {@code [REASONING:]} 行内标记协议已废弃，只在
 * {@code LlmStreamProvider.legacyInlineReasoning(...)} 兼容层里保留），
 * 这样「标签式思考」与「独立 reasoning_content 字段」走同一条路。
 *
 * <p><b>支持的标签变体</b>（大小写不敏感）：{@code <think>}、{@code <thinking>}、{@code <reasoning>}
 * 及其对应闭标签。闭标签在<b>未处于思考态</b>时出现 ⇒ 视为噪声丢弃（标签本身不输出）。
 *
 * <p><b>流式要点</b>：
 * <ol>
 *   <li><b>跨增量</b>：标签可能被切在两个增量之间（{@code "<thi"} + {@code "nk>"}）⇒
 *       尾部「可能是标签前缀」的残片会被<b>缓冲</b>，等下一段到达再判定；</li>
 *   <li><b>未闭合</b>：流结束时仍处于思考态 ⇒ 剩余内容算<b>思考</b>（不作为正文外泄），
 *       并由 {@link #flush()} 交还调用方决定如何处置；</li>
 *   <li><b>不误伤</b>：正文里的 {@code "<"}、{@code "a < b"}、HTML 片段（{@code <div>}）原样保留。</li>
 * </ol>
 *
 * <p><b>线程安全</b>：本类<b>有状态</b>（跨增量维持思考态与残片缓冲）⇒ 每个流/会话必须用独立实例，
 * 不可作为共享单例。
 */
public final class UpstreamThinkingTagSplitter {

    /** 已知的思考开标签（大小写不敏感匹配）。 */
    private static final String[] OPEN_TAGS = {"<think>", "<thinking>", "<reasoning>"};

    /** 已知的思考闭标签（大小写不敏感匹配）。 */
    private static final String[] CLOSE_TAGS = {"</think>", "</thinking>", "</reasoning>"};

    /** 最长标签长度（用于残片缓冲的上界）。 */
    private static final int MAX_TAG_LEN = 10;

    /** 是否处于思考态。 */
    private boolean inThinking;

    /** 尾部残片：可能是被切开的标签前缀，等待下一段拼接后再判定。 */
    private final StringBuilder pending = new StringBuilder();

    /**
     * 一段增量的切分结果。
     */
    public static final class Split {
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder reasoning = new StringBuilder();

        public String getText() {
            return text.toString();
        }

        public String getReasoning() {
            return reasoning.toString();
        }

        public boolean hasText() {
            return text.length() > 0;
        }

        public boolean hasReasoning() {
            return reasoning.length() > 0;
        }
    }

    /**
     * 切分一段 content 增量。
     *
     * @param delta 上游推来的 content 增量（可空）
     * @return 切分结果（正文段与思考段，可能其一为空）
     */
    public Split split(String delta) {
        Split out = new Split();
        if (delta == null || delta.isEmpty()) {
            return out;
        }
        String buf = pending.length() > 0 ? pending + delta : delta;
        pending.setLength(0);

        int i = 0;
        int n = buf.length();
        while (i < n) {
            if (inThinking) {
                int close = indexOfTag(buf, i, CLOSE_TAGS);
                if (close < 0) {
                    // 可能是闭标签被切开 ⇒ 尾部残片缓冲，其余算思考
                    int keep = trailingPartialTagLength(buf, i, CLOSE_TAGS);
                    out.reasoning.append(buf, i, n - keep);
                    if (keep > 0) {
                        pending.append(buf, n - keep, n);
                    }
                    return out;
                }
                out.reasoning.append(buf, i, close);
                inThinking = false;
                i = close + tagLengthAt(buf, close, CLOSE_TAGS);
            } else {
                int open = indexOfTag(buf, i, OPEN_TAGS);
                int close = indexOfTag(buf, i, CLOSE_TAGS);
                if (open < 0 && close < 0) {
                    int keep = trailingPartialTagLength(buf, i, OPEN_TAGS);
                    if (keep > 0) {
                        int closeKeep = trailingPartialTagLength(buf, i, CLOSE_TAGS);
                        keep = Math.max(keep, closeKeep);
                    }
                    out.text.append(buf, i, n - keep);
                    if (keep > 0) {
                        pending.append(buf, n - keep, n);
                    }
                    return out;
                }
                // 先出现的那个标签决定走向
                int idx;
                boolean isOpen;
                if (open < 0) {
                    idx = close;
                    isOpen = false;
                } else if (close < 0) {
                    idx = open;
                    isOpen = true;
                } else {
                    idx = Math.min(open, close);
                    isOpen = open < close;
                }
                out.text.append(buf, i, idx);
                if (isOpen) {
                    inThinking = true;
                    i = idx + tagLengthAt(buf, idx, OPEN_TAGS);
                } else {
                    // 文本态遇到闭标签 = 模型只发了闭标签 ⇒ 丢弃标签本身，内容留正文
                    i = idx + tagLengthAt(buf, idx, CLOSE_TAGS);
                }
            }
        }
        return out;
    }

    /**
     * 流结束时的收尾：把残片缓冲区交还。
     *
     * <p>残片不是完整标签（否则在 {@link #split} 里已被消费）⇒ 按当前状态归位：
     * 思考态 ⇒ 算思考；文本态 ⇒ 算正文。
     *
     * @return 收尾时补出的一段（正文段与思考段，可能其一为空）
     */
    public Split flush() {
        Split out = new Split();
        if (pending.length() == 0) {
            return out;
        }
        String rest = pending.toString();
        pending.setLength(0);
        if (inThinking) {
            out.reasoning.append(rest);
        } else {
            out.text.append(rest);
        }
        return out;
    }

    /**
     * 当前是否处于思考态（流结束时若仍为 true，说明上游的思考块没有闭合）。
     */
    public boolean isInThinking() {
        return inThinking;
    }

    /**
     * 从 {@code from} 起找第一个能匹配任一标签的位置；找不到返回 -1。
     */
    private static int indexOfTag(String s, int from, String[] tags) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) != '<') {
                continue;
            }
            if (tagLengthAt(s, i, tags) > 0) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@code s} 在 {@code pos} 处能匹配到的标签长度；不匹配返回 0。
     */
    private static int tagLengthAt(String s, int pos, String[] tags) {
        for (String tag : tags) {
            if (s.regionMatches(true, pos, tag, 0, tag.length())) {
                return tag.length();
            }
        }
        return 0;
    }

    /**
     * 尾部「可能是被切开的标签前缀」的残片长度：取所有标签前缀里能匹配尾部且未构成完整标签的最大长度。
     */
    private static int trailingPartialTagLength(String s, int from, String[] tags) {
        int max = 0;
        for (String tag : tags) {
            // 完整标签不该留在这里（会被上面消费掉），只取真前缀
            for (int len = 1; len < tag.length(); len++) {
                String prefix = tag.substring(0, len);
                if (s.length() - from < len) {
                    continue;
                }
                if (s.regionMatches(true, s.length() - len, prefix, 0, len)) {
                    max = Math.max(max, len);
                }
            }
        }
        return Math.min(max, MAX_TAG_LEN - 1);
    }

    /**
     * 便于排查：把当前状态压成一行可读文本。
     */
    @Override
    public String toString() {
        return "UpstreamThinkingTagSplitter{inThinking=" + inThinking
                + ", pending=" + pending.length()
                + ", lower=" + pending.toString().toLowerCase(Locale.ROOT) + "}";
    }
}
