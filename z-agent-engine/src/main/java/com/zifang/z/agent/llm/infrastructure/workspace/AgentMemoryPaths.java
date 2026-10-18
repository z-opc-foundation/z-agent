package com.zifang.z.agent.llm.infrastructure.workspace;

/**
 * Agent memory paths — defines file system layout for agent memory/workspace.
 */
public class AgentMemoryPaths {

    private final String basePath;

    public AgentMemoryPaths(String basePath) {
        this.basePath = basePath;
    }

    /**
     * Phase 7 扩展：把任意字符串转为文件系统安全的 agent key。
     * 直接复用 {@link com.zifang.z.team.core.agent.runtime.AgentKeyUtil#safeAgentKey(String)}；
     * 本签名作为 z-agent-engine 标准点。
     */
    public static String safeAgentKey(String raw) {
        if (raw == null) {
            return "agent";
        }
        return raw.trim().replaceAll("[^a-zA-Z0-9_\\-\\.\\u4e00-\\u9fff]", "_");
    }

    public String getBasePath() {
        return basePath;
    }

    public String getConversationPath(String sessionId) {
        return basePath + "/conversations/" + sessionId;
    }

    public String getTracePath(String sessionId) {
        return basePath + "/traces/" + sessionId;
    }

    public String getMemoryPath(String sessionId) {
        return basePath + "/memory/" + sessionId;
    }
}
