package com.zifang.z.agent.api.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 单次运行响应 DTO.
 *
 * <p>agentName: 哪个 agent 跑的.
 * <p>output: 最终输出(用户面向的 ASSISTANT 消息).
 * <p>steps: 推理步骤轨迹(ReAct Thought/Action/Observation / Plan 各 step).
 * <p>usage: token 用量.
 * <p>finishedReason: 完成原因(completed / max_steps / tool_error / timeout).
 * <p>errorMessage: 失败信息(成功时 null).
 * <p>metadata: 任意附加元信息(runId / durationMs 等).
 */
public final class RunResponse {

    private final String agentName;
    private final String output;
    private final List<Map<String, Object>> steps;
    private final TokenUsageDto usage;
    private final String finishedReason;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    public RunResponse(String agentName, String output, List<Map<String, Object>> steps,
                       TokenUsageDto usage, String finishedReason, String errorMessage,
                       Map<String, Object> metadata) {
        this.agentName = agentName;
        this.output = output == null ? "" : output;
        this.steps = steps == null ? Collections.<Map<String, Object>>emptyList()
                : Collections.unmodifiableList(new ArrayList<Map<String, Object>>(steps));
        this.usage = usage == null ? TokenUsageDto.empty() : usage;
        this.finishedReason = finishedReason == null ? "unknown" : finishedReason;
        this.errorMessage = errorMessage;
        this.metadata = metadata == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(metadata));
    }

    public String getAgentName() { return agentName; }
    public String getOutput() { return output; }
    public List<Map<String, Object>> getSteps() { return steps; }
    public TokenUsageDto getUsage() { return usage; }
    public String getFinishedReason() { return finishedReason; }
    public String getErrorMessage() { return errorMessage; }
    public Map<String, Object> getMetadata() { return metadata; }

    public boolean isSuccess() { return errorMessage == null && "completed".equals(finishedReason); }
}