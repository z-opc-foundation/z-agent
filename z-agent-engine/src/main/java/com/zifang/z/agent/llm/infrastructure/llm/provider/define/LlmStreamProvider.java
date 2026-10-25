package com.zifang.z.agent.llm.infrastructure.llm.provider.define;

import com.zifang.z.agent.llm.infrastructure.llm.events.LlmProviderStreamEvents;

/**
 * LLM stream provider — abstracts the streaming protocol for different LLM backends.
 *
 * <p><b>正文 / 思考分通道（2026-10-25）</b>：上游模型的思考过程（{@code delta.reasoning_content}，
 * 或上游把思考以裸标签塞进 {@code delta.content}）走 {@link StreamCallbacks#onThought(String)}，
 * <b>不再</b>混进 {@link StreamCallbacks#onToken(String)} 的正文流。
 *
 * <p><b>为什么必须分</b>（生产实测）：老协议把思考以无闭合的 {@code "[REASONING:]"} 前缀塞进正文流，
 * 而上游 content / reasoning_content <b>交替</b>下发 ⇒ 思考块会插进 JSON 的<b>键名/值中间</b>
 * （实测原文：{@code {"dispa[REASONING:]。tches":[{"target":"智能助手",…}]}}）⇒
 * 标记不携带边界，事后<b>无法</b>从合并流里还原 ⇒ 下游 JSON 解析必失败
 * （{@code IllegalArgumentException: Invalid key}）⇒ 群聊零回复。
 */
public interface LlmStreamProvider {

    /**
     * legacy 行内推理标记前缀（<b>无闭合</b>）——仅供兼容层使用，见 {@link #legacyInlineReasoning(StreamCallbacks)}。
     *
     * <p>新代码<b>不要</b>再往正文流里拼这个前缀：它是老协议的化石，
     * 留着只会让「思考插进 JSON 键名中间」的老坑复发。
     */
    String LEGACY_INLINE_REASONING_PREFIX = "[REASONING:]";

    /**
     * Start a stream call.
     */
    void stream(String prompt, String systemPrompt, StreamCallbacks callbacks);

    /**
     * Stop a running stream.
     */
    void stop();

    /**
     * Stream callbacks.
     */
    interface StreamCallbacks {
        void onToken(String token);

        void onComplete(String fullText);

        void onError(Throwable error);

        /**
         * 独立推理通道：上游模型的思考增量。
         *
         * <p><b>默认实现 = 丢弃</b> ⇒ 默认消费方拿到的正文流是<b>纯正文</b>（可被 JSON 解析），
         * 且既有实现方（只实现 {@code onToken/onComplete/onError}）<b>无需改动即可编译</b>。
         *
         * <p>需要思考内容的消费方覆写本方法即可；
         * 需要 legacy 合并流（正文里带无闭合的 {@code "[REASONING:]"} 标记）的消费方，
         * 用 {@link LlmStreamProvider#legacyInlineReasoning(StreamCallbacks)} 兼容层包一层。
         */
        default void onThought(String thought) {
            // 默认丢弃：正文流保持纯正文（下游 JSON 解析不再被思考碎片打断）
        }
    }

    /**
     * 兼容层：把独立推理通道<b>回灌</b>成 legacy 行内标记（正文流里带无闭合的 {@code "[REASONING:]"}）。
     *
     * <p>给「确实依赖老协议」的消费方用：它包一层后，思考又会以
     * {@code onToken("[REASONING:]" + thought)} 的形式回到正文流，
     * 行为与 2026-10-25 之前<b>逐字一致</b>。
     *
     * <p>典型用法（消费方侧）：
     * <pre>{@code
     * provider.stream(prompt, sys, LlmStreamProvider.legacyInlineReasoning(myCallbacks));
     * }</pre>
     *
     * <p><b>注意</b>：走这条路的正文流<b>不可</b>直接喂 JSON 解析器（老坑原样存在），
     * 仅供「正文 + 思考混排展示」的老消费方过渡使用。
     *
     * @param delegate 真正的消费方回调（思考会被回灌进它的 {@code onToken}）
     * @return 包装后的回调
     */
    static StreamCallbacks legacyInlineReasoning(StreamCallbacks delegate) {
        if (delegate == null) {
            return null;
        }
        return new StreamCallbacks() {
            @Override
            public void onToken(String token) {
                delegate.onToken(token);
            }

            @Override
            public void onComplete(String fullText) {
                delegate.onComplete(fullText);
            }

            @Override
            public void onError(Throwable error) {
                delegate.onError(error);
            }

            @Override
            public void onThought(String thought) {
                if (thought != null && !thought.isEmpty()) {
                    delegate.onToken(LEGACY_INLINE_REASONING_PREFIX + thought);
                }
            }
        };
    }
}
