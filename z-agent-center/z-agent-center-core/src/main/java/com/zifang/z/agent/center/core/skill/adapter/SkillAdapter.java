package com.zifang.z.agent.center.core.skill.adapter;

import com.zifang.z.agent.common.enums.AgentType;

import java.util.List;

/**
 * FEATURE014: Skill 适配器接口
 *
 * <p>每种 Agent 类型 (z-bot / Claude Code / OpenCode / Hermes / Custom)
 * 对应一个 SkillAdapter 实现, 负责把 SKILL.md 内容写到该 Agent 的本地落盘路径,
 * 并通知 Agent 重新加载。</p>
 *
 * <p>设计要点:
 * <ul>
 *   <li>writeSkill / deleteSkill: 写文件 (落盘到 instance.skill_root_path)</li>
 *   <li>notifyAgent: HTTP POST 通知 Agent 增量加载 (PUSH 模式)</li>
 *   <li>healthCheck: GET /health 探活 (用于心跳)</li>
 *   <li>按 instanceCode 隔离, 避免多实例共享同一目录写冲突</li>
 * </ul>
 */
public interface SkillAdapter {

    /**
     * 当前 Adapter 适配的 Agent 类型
     */
    AgentType getAgentType();

    /**
     * 默认 Skill 落盘根路径 (~/.zbot/skills, ~/.claude/skills 等)
     */
    String getDefaultSkillRootPath();

    /**
     * 写单个 skill 到 Agent 本地 (落盘 SKILL.md)
     *
     * @param instanceCode Agent 实例编码 (用于目录隔离)
     * @param skillCode    技能编码
     * @param content      SKILL.md 全文
     * @return 实际落盘的文件绝对路径
     */
    String writeSkill(String instanceCode, String skillCode, String content);

    /**
     * 删除单个 skill (含整个 skill 目录)
     *
     * @return true 表示删除成功, false 表示失败
     */
    boolean deleteSkill(String instanceCode, String skillCode);

    /**
     * 通知 Agent 重新加载 skill (PUSH 模式)
     *
     * @param callbackUrl Agent 暴露的回调 URL (instance.callback_url)
     * @param skillCodes  变更的 skill 编码列表
     * @return true 表示通知成功 (HTTP 2xx), false 表示失败
     */
    boolean notifyAgent(String callbackUrl, List<String> skillCodes);

    /**
     * 健康检查 (HTTP GET {callbackUrl}/health)
     */
    boolean healthCheck(String callbackUrl);
}