package com.zifang.z.agent.center.core.skill.adapter;

import com.zifang.z.agent.common.enums.AgentType;
import org.springframework.stereotype.Component;

/**
 * FEATURE014: z-bot (JVM 内) 的 SkillAdapter 实现
 *
 * <p>z-bot 是 z-opc 自家 Agent, JVM 内 ReAct 执行, 落盘到 {@code ~/.zbot/skills/}。
 * 落盘后无需 HTTP 通知 (因为是同 JVM), 仅记录日志即可.</p>
 */
@Component
public class ZBotSkillAdapter extends AbstractSkillAdapter {

    @Override
    public AgentType getAgentType() {
        return AgentType.ZBOT;
    }

    @Override
    public String getDefaultSkillRootPath() {
        return AgentType.ZBOT.getDefaultSkillRootPath();
    }

    @Override
    public boolean notifyAgent(String callbackUrl, java.util.List<String> skillCodes) {
        // z-bot 同 JVM, 不需要 HTTP 回调; 落盘后 ReActAgentFactory 启动时会自动扫描
        log.info("[ZBotSkillAdapter] notify (in-process): skillCodes={}", skillCodes);
        return true;
    }
}