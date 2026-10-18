package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.common.enums.AgentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Claude Code 专用执行器 — FEATURE014 Day 2
 *
 * <p>Claude Code 本地 CLI 的特化：</p>
 * <ul>
 *   <li>启动命令：{@code claude serve --port {port} --dangerously-skip-permissions}
 *       （CLI 命名可能演进，参见 Anthropic 官方文档）</li>
 *   <li>Skill 落盘路径：{@code ~/.claude/skills/{skillCode}/SKILL.md}</li>
 *   <li>健康检查：{@code GET http://localhost:{port}/api/health}</li>
 *   <li>对话接口：{@code POST /api/agent/chat}（与通用约定一致）</li>
 * </ul>
 *
 * <p>注意：本类只在 Claude Code 进程已启动并通过 {@link #registerProcess} 注册后才工作；
 * 启动本身由 {@link LocalProcessAgentRuntime} 完成，
 * 本类只负责发送命令参数 + 健康检查 / 协议特化。</p>
 */
public class ClaudeCodeExecutor extends LocalProcessProxyExecutor {

    private static final Logger log = LogManager.getLogger(ClaudeCodeExecutor.class);

    public ClaudeCodeExecutor(LocalProcessAgentRuntime runtime) {
        super(runtime, AgentType.CLAUDE_CODE);
    }

    /**
     * 构造 Claude Code 启动命令（供 LocalProcessAgentRuntime 调用）。
     *
     * <p>Claude Code 的实际 CLI 形如 {@code claude serve --port 9100}；
     * 若本机安装的是较新的 {@code @anthropic-ai/claude-code} 包，
     * 二进制名可能是 {@code claude-code}，调用方应在外部探测。</p>
     */
    public static String[] buildLaunchCommand(int port) {
        return new String[]{
                "claude", "serve",
                "--port", String.valueOf(port),
                "--dangerously-skip-permissions"
        };
    }

    /**
     * 探测本机 Claude Code CLI 是否可用（{@code which claude}）。
     */
    public static boolean isCliAvailable() {
        try {
            Process p = new ProcessBuilder("which", "claude").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getType() {
        return AgentType.CLAUDE_CODE.name();
    }

    /**
     * 尝试向 Claude Code 推送一条"中心注册"消息（启动后反向回连 center）。
     * Claude Code 支持通过 HTTP 调用 {@code POST /api/agents} 注册。
     */
    public boolean registerWithCenter(String callbackUrl, String authToken) {
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            return false;
        }
        // 通过 super.execute 复用 OkHttp 通道（chat 端点即可作为通用 RPC 通道）
        // 注意：本方法目前未实际触发 Claude Code 内部注册逻辑，留作后续 Day 4 接入 SkillDistributor 时实现
        log.info("[claude-code] registerWithCenter: callbackUrl={} (pending SkillDistributor integration)", callbackUrl);
        return true;
    }
}
