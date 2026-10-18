package com.zifang.z.agent.center.core.skill.adapter;

import com.zifang.z.agent.common.enums.AgentType;
import org.springframework.stereotype.Component;

/**
 * 各 AgentType 的 SkillAdapter 实现 — FEATURE014 §2.4。
 *
 * <p>全部继承 {@link AbstractSkillAdapter}，差异仅在 {@code getDefaultSkillRootPath()}
 * 这一行。每个类都注册为 Spring Bean，{@code SkillAdapterFactory} 按 agentType 分发。</p>
 *
 * <p>注意：ZBOT 的 adapter 见 {@link ZBotSkillAdapter}（专用文件，有特殊的 in-process notifyAgent 实现）。</p>
 */
public class Adapters {

    @Component
    public static class HermesContainerAdapter extends AbstractSkillAdapter {
        @Override
        public AgentType getAgentType() {
            return AgentType.HERMES_CONTAINER;
        }

        @Override
        public String getDefaultSkillRootPath() {
            return AgentType.HERMES_CONTAINER.getDefaultSkillRootPath();
        }
    }

    @Component
    public static class HermesLocalAdapter extends AbstractSkillAdapter {
        @Override
        public AgentType getAgentType() {
            return AgentType.HERMES_LOCAL;
        }

        @Override
        public String getDefaultSkillRootPath() {
            return AgentType.HERMES_LOCAL.getDefaultSkillRootPath();
        }
    }

    @Component
    public static class ClaudeCodeAdapter extends AbstractSkillAdapter {
        @Override
        public AgentType getAgentType() {
            return AgentType.CLAUDE_CODE;
        }

        @Override
        public String getDefaultSkillRootPath() {
            return AgentType.CLAUDE_CODE.getDefaultSkillRootPath();
        }
    }

    @Component
    public static class OpenCodeAdapter extends AbstractSkillAdapter {
        @Override
        public AgentType getAgentType() {
            return AgentType.OPENCODE;
        }

        @Override
        public String getDefaultSkillRootPath() {
            return AgentType.OPENCODE.getDefaultSkillRootPath();
        }
    }

    @Component
    public static class CustomAdapter extends AbstractSkillAdapter {
        @Override
        public AgentType getAgentType() {
            return AgentType.CUSTOM;
        }

        @Override
        public String getDefaultSkillRootPath() {
            return AgentType.CUSTOM.getDefaultSkillRootPath();
        }
    }
}
