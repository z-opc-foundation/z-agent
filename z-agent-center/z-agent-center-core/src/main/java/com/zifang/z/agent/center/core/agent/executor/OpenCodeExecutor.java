package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.common.enums.AgentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * OpenCode 专用执行器 — FEATURE014 Day 3
 *
 * <p>OpenCode 本地 CLI 的特化（语义标记子类，行为委托给 {@link LocalProcessProxyExecutor}）。
 * 保留为独立类是为了后续接入 OpenCode 专有的协议（如自定义 health 路径、skill 路径约定）。</p>
 *
 * <ul>
 *   <li>启动命令：{@code opencode serve --port {port}}</li>
 *   <li>Skill 落盘路径：{@code ~/.opencode/skills/{skillCode}/SKILL.md}</li>
 *   <li>健康检查：{@code GET http://localhost:{port}/health}</li>
 * </ul>
 */
public class OpenCodeExecutor extends LocalProcessProxyExecutor {

    private static final Logger log = LogManager.getLogger(OpenCodeExecutor.class);

    public OpenCodeExecutor(LocalProcessAgentRuntime runtime) {
        super(runtime, AgentType.OPENCODE);
    }

    /**
     * 构造 OpenCode 启动命令（供 LocalProcessAgentRuntime 调用）。
     */
    public static String[] buildLaunchCommand(int port) {
        return new String[]{
                "opencode", "serve",
                "--port", String.valueOf(port)
        };
    }

    /**
     * 探测本机 OpenCode CLI 是否可用（{@code which opencode}）。
     */
    public static boolean isCliAvailable() {
        try {
            Process p = new ProcessBuilder("which", "opencode").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getType() {
        return AgentType.OPENCODE.name();
    }
}
