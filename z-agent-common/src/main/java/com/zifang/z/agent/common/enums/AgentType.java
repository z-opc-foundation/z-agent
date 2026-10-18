package com.zifang.z.agent.common.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * Agent 类型枚举 — FEATURE014 多 Agent 统一管理
 *
 * <p>每种类型绑定一个默认的 skill 落盘根路径。AgentInstance 在创建时会从 agentType
 * 自动推导 skillRootPath（除非显式覆盖）。</p>
 *
 * <ul>
 *   <li>ZBOT             — z-opc 自家 z-agent-bot（JVM 内执行）</li>
 *   <li>HERMES_CONTAINER — 通过 Docker 容器运行的 Hermes Agent（兼容旧 CONTAINER 模式）</li>
 *   <li>HERMES_LOCAL     — 本地进程运行的 Hermes（hermes serve --port {port}）</li>
 *   <li>CLAUDE_CODE      — 本地 Claude Code（claude serve --port {port}，HTTP/MCP-stdio）</li>
 *   <li>OPENCODE         — 本地 OpenCode（opencode serve --port {port}）</li>
 *   <li>CUSTOM           — 用户自定义进程（bash -c "user-cmd"）</li>
 * </ul>
 */
public enum AgentType {

    /**
     * z-opc 自家 z-agent-bot，JVM 内 ReAct 执行
     */
    ZBOT("z-bot", "~/.zbot/skills", "in-process"),

    /**
     * Docker 容器里跑的 Hermes（FEATURE014 之前的 CONTAINER 模式，向后兼容）
     */
    HERMES_CONTAINER("Hermes (Docker)", "~/.hermes/skills", "container"),

    /**
     * 本地进程跑的 Hermes（hermes CLI）
     */
    HERMES_LOCAL("Hermes (Local)", "~/.hermes/skills", "local-process"),

    /**
     * 本地 Claude Code（claude CLI / daemon）
     */
    CLAUDE_CODE("Claude Code", "~/.claude/skills", "local-process"),

    /**
     * 本地 OpenCode（opencode CLI / daemon）
     */
    OPENCODE("OpenCode", "~/.opencode/skills", "local-process"),

    /**
     * 用户自定义进程
     */
    CUSTOM("Custom", "~/.custom/skills", "local-process");

    private final String displayName;
    private final String defaultSkillRootPath;
    private final String executionMode;

    AgentType(String displayName, String defaultSkillRootPath, String executionMode) {
        this.displayName = displayName;
        this.defaultSkillRootPath = defaultSkillRootPath;
        this.executionMode = executionMode;
    }

    /**
     * 兼容旧 runMode 字段的映射：
     * <ul>
     *   <li>IN_PROCESS → ZBOT</li>
     *   <li>CONTAINER  → HERMES_CONTAINER</li>
     *   <li>其他/null  → ZBOT（默认）</li>
     * </ul>
     */
    public static AgentType fromLegacyRunMode(String runMode) {
        if (StringUtils.isBlank(runMode)) {
            return ZBOT;
        }
        switch (runMode) {
            case "IN_PROCESS":
                return ZBOT;
            case "CONTAINER":
                return HERMES_CONTAINER;
            default:
                return ZBOT;
        }
    }

    /**
     * 把 agentType 字符串安全地解析为枚举（容错：未知值返回 ZBOT）。
     */
    public static AgentType parse(String code) {
        if (StringUtils.isBlank(code)) {
            return ZBOT;
        }
        String upper = code.trim().toUpperCase().replace('-', '_');
        return Arrays.stream(values())
                .filter(t -> t.name().equals(upper))
                .findFirst()
                .orElse(ZBOT);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultSkillRootPath() {
        return defaultSkillRootPath;
    }

    /**
     * 执行模式（不是 DB runMode 字段，是执行器分发的辅助分类）。
     * <ul>
     *   <li>in-process     — JVM 内 ReAct 循环</li>
     *   <li>container      — Docker 容器 HTTP 代理</li>
     *   <li>local-process  — 本地进程 HTTP 代理</li>
     * </ul>
     */
    public String getExecutionMode() {
        return executionMode;
    }
}
