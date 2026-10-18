package com.zifang.z.agent.llm.agentunit;

import com.zifang.z.agent.llm.agent.extension.AgentExtension;
import com.zifang.z.agent.llm.agent.runtime.AgentContext;
import com.zifang.z.agent.llm.agent.runtime.ReactAgent;
import com.zifang.z.agent.kernel.tool.BaseTool;
import com.zifang.z.agent.llm.agentunit.mode.AgentMode;
import com.zifang.z.agent.llm.infrastructure.llm.context.LlmStreamRequest;
import com.zifang.z.agent.llm.infrastructure.llm.engine.LlmReactEngine;
import com.zifang.z.agent.llm.infrastructure.messaging.payload.UserInputPayload;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Agent unit — the top-level agent container that wraps a ReactAgent with metadata, modes, and extensions.
 */
public class AgentUnit {

    private final String name;
    private final String llmCode;
    private final LlmStreamRequest.StreamOptions defaultStreamOptions;
    private final LlmReactEngine llmEngine;
    private final ReactAgent agent;
    private final List<AgentMode> modes;
    private final List<AgentExtension> extensions;
    private final boolean canCreateWorkers;
    private final Path workspace;
    private final String workspaceOsDir;
    private final AgentContext context;

    private AgentUnit(Builder builder) {
        this.name = builder.name;
        this.llmCode = builder.llmCode;
        this.defaultStreamOptions = builder.defaultStreamOptions;
        this.llmEngine = builder.llmEngine;
        this.modes = builder.modes;
        this.extensions = builder.extensions;
        this.canCreateWorkers = builder.canCreateWorkers;
        this.workspace = builder.workspace;
        this.workspaceOsDir = builder.workspaceOsDir;

        this.context = new AgentContext(
                builder.id != null ? builder.id : "unit-" + name,
                builder.sessionId,
                builder.currentMode != null ? builder.currentMode : "build"
        );

        this.agent = new ReactAgent(
                builder.name,
                null,
                builder.id,
                builder.sessionId,
                builder.llmCode,
                builder.defaultStreamOptions,
                builder.llmEngine,
                builder.tools
        );

        for (AgentExtension ext : extensions) {
            ext.install(agent);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public ReactAgent agent() {
        return agent;
    }

    public AgentContext context() {
        return context;
    }

    public void registerTools(List<BaseTool> tools) {
        if (tools != null) {
            for (BaseTool tool : tools) {
                agent.registerTool(tool);
            }
        }
    }

    /**
     * Chat with the agent using text and optional payload.
     */
    public String chat(String text, Map<String, Object> payload) {
        return agent.chat();
    }

    /**
     * Phase 7 扩展：UserInputPayload 重载（兼容 0815 业务调用）。
     * <p>直接把 payload 转为 Map 透传给 {@link ReactAgent}（其内部会做语义转换）。</p>
     */
    public String chat(String text, UserInputPayload payload) {
        return chat(text, payload == null ? Collections.emptyMap() : payload.toMap());
    }

    /**
     * Phase 7 扩展：覆盖 Agent 系统 prompt（透传给内嵌 ReactAgent）。
     */
    public void applySystemPrompt(String systemPrompt) {
        if (agent != null) {
            agent.applySystemPrompt(systemPrompt);
        }
    }

    /**
     * Switch to a specific mode.
     */
    public void switchMode(String modeCode) {
        if (modeCode != null) {
            context.setMode(modeCode);
        }
    }

    /**
     * Ensure tools are ready for the current mode.
     */
    public void ensureToolsReady() {
        // No-op for stub
    }

    public String getName() {
        return name;
    }

    public String getLlmCode() {
        return llmCode;
    }

    public LlmReactEngine getLlmEngine() {
        return llmEngine;
    }

    public List<AgentMode> getModes() {
        return Collections.unmodifiableList(modes);
    }

    public boolean isCanCreateWorkers() {
        return canCreateWorkers;
    }

    public Path getWorkspace() {
        return workspace;
    }

    public String getWorkspaceOsDir() {
        return workspaceOsDir;
    }

    /**
     * z-team-core 调用点快速访问sessionId。
     */
    public String sessionId() {
        return context != null ? context.sessionId() : null;
    }

    /**
     * z-team-core 调用点快速访问 unitId。
     */
    public String unitId() {
        return context != null ? context.unitId() : null;
    }

    /**
     * z-team-core 调用点快速访问当前 mode。
     */
    public String currentMode() {
        return context != null ? context.mode() : "build";
    }

    public static class Builder {
        private String name = "Agent";
        private String id;
        private String sessionId;
        private String llmCode;
        private LlmStreamRequest.StreamOptions defaultStreamOptions;
        private LlmReactEngine llmEngine;
        private List<BaseTool> tools = new ArrayList<>();
        private List<AgentMode> modes = new ArrayList<>();
        private List<AgentExtension> extensions = new ArrayList<>();
        private boolean canCreateWorkers = false;
        private Path workspace;
        private String workspaceOsDir;
        private String currentMode;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder llmCode(String llmCode) {
            this.llmCode = llmCode;
            return this;
        }

        public Builder defaultStreamOptions(LlmStreamRequest.StreamOptions options) {
            this.defaultStreamOptions = options;
            return this;
        }

        public Builder llmEngine(LlmReactEngine engine) {
            this.llmEngine = engine;
            return this;
        }

        public Builder tools(List<BaseTool> tools) {
            this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
            return this;
        }

        public Builder addMode(AgentMode mode) {
            this.modes.add(mode);
            if (this.currentMode == null && mode != null) {
                this.currentMode = mode.getCode();
            }
            return this;
        }

        public Builder extension(AgentExtension extension) {
            this.extensions.add(extension);
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(String userId) {
            return this;
        }

        public Builder userName(String userName) {
            return this;
        }

        public Builder canCreateWorkers(boolean canCreateWorkers) {
            this.canCreateWorkers = canCreateWorkers;
            return this;
        }

        public Builder workspace(Path workspace) {
            this.workspace = workspace;
            return this;
        }

        public Builder workspaceOsDir(String workspaceOsDir) {
            this.workspaceOsDir = workspaceOsDir;
            return this;
        }

        public AgentUnit build() {
            return new AgentUnit(this);
        }
    }
}
