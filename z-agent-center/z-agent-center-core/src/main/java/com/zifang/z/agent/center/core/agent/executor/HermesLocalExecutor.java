package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.common.enums.AgentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Hermes 本地模式专用执行器 — FEATURE014 Day 3
 *
 * <p>Hermes CLI 本地运行（区别于 {@link HermesProxyExecutor} 的 Docker 容器模式）。
 * 行为委托给 {@link LocalProcessProxyExecutor}，保留为独立类便于未来扩展 Hermes
 * 专有的协议 / 路径约定。</p>
 *
 * <ul>
 *   <li>启动命令：{@code hermes serve --port {port}}</li>
 *   <li>Skill 落盘路径：{@code ~/.hermes/skills/{skillCode}/SKILL.md}</li>
 *   <li>健康检查：{@code GET http://localhost:{port}/health}</li>
 * </ul>
 */
public class HermesLocalExecutor extends LocalProcessProxyExecutor {

    private static final Logger log = LogManager.getLogger(HermesLocalExecutor.class);

    public HermesLocalExecutor(LocalProcessAgentRuntime runtime) {
        super(runtime, AgentType.HERMES_LOCAL);
    }

    /**
     * 构造 Hermes 本地启动命令（供 LocalProcessAgentRuntime 调用）。
     */
    public static String[] buildLaunchCommand(int port) {
        return new String[]{
                "hermes", "serve",
                "--port", String.valueOf(port)
        };
    }

    /**
     * 探测本机 Hermes CLI 是否可用（{@code which hermes}）。
     */
    public static boolean isCliAvailable() {
        try {
            Process p = new ProcessBuilder("which", "hermes").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getType() {
        return AgentType.HERMES_LOCAL.name();
    }
}
