package com.zifang.z.agent.center.core.bot.hook;

import java.util.Map;

/**
 * z-bot Hook SPI — FEATURE014 Wishlist 5 (L3 进化层)
 *
 * <p>center 侧的可观察性 hook：每次事件触发，dispatcher 串行调用所有注册的 hook。</p>
 *
 * <p>实现类应用 {@code @Component} 标注，自动被 {@link BotHookDispatcher} 注入。</p>
 *
 * <p>事件类型与触发点：</p>
 * <ul>
 *   <li>{@code preChat}     — center 收到 instance chat 请求时（AgentChatController）</li>
 *   <li>{@code postChat}    — chat 返回时</li>
 *   <li>{@code preSkill}    — Skill 分发（writeSkill）前</li>
 *   <li>{@code postSkill}   — Skill 分发后</li>
 *   <li>{@code onFeedback}  — 用户提交反馈后</li>
 *   <li>{@code onError}     — 任意上述事件抛异常</li>
 * </ul>
 */
public interface BotHook {

    /**
     * Hook 名字（短，便于日志 / 审计表）
     */
    String name();

    /**
     * 触发点匹配（事件类型命中即调用；不区分 pre/post 的 hook 也可实现 matchAll=true）
     */
    boolean matches(String eventType);

    /**
     * 触发。
     *
     * @param eventType preChat / postChat / preSkill / postSkill / onFeedback / onError
     * @param context   事件 payload（含 instanceCode / userId / message / result / error 等）
     * @throws Exception 抛异常会被 dispatcher 捕获并记录到 z_bot_hook_event（result=FAIL）
     */
    void fire(String eventType, Map<String, Object> context) throws Exception;
}
