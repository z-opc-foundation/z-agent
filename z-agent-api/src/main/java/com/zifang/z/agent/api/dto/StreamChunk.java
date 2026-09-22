package com.zifang.z.agent.api.dto;

/**
 * 流式增量 DTO. SSE 输出 / StreamChunk 包装.
 *
 * <p>type: chunk 类型(step / output / usage / error / done).
 * <p>step: 当 type=step 时的 step 内容.
 * <p>output: 当 type=output 时的增量输出字符串.
 * <p>usage: 当 type=usage 时的 token 用量.
 * <p>errorMessage: 当 type=error 时的错误信息.
 */
public final class StreamChunk {

    public static final String TYPE_STEP = "step";
    public static final String TYPE_OUTPUT = "output";
    public static final String TYPE_USAGE = "usage";
    public static final String TYPE_ERROR = "error";
    public static final String TYPE_DONE = "done";

    private final String type;
    private final Object step;
    private final String output;
    private final TokenUsageDto usage;
    private final String errorMessage;
    private final String agentName;

    private StreamChunk(String type, Object step, String output, TokenUsageDto usage,
                        String errorMessage, String agentName) {
        this.type = type;
        this.step = step;
        this.output = output;
        this.usage = usage;
        this.errorMessage = errorMessage;
        this.agentName = agentName;
    }

    public static StreamChunk step(String agentName, Object stepData) {
        return new StreamChunk(TYPE_STEP, stepData, null, null, null, agentName);
    }
    public static StreamChunk output(String agentName, String delta) {
        return new StreamChunk(TYPE_OUTPUT, null, delta, null, null, agentName);
    }
    public static StreamChunk usage(String agentName, TokenUsageDto usage) {
        return new StreamChunk(TYPE_USAGE, null, null, usage, null, agentName);
    }
    public static StreamChunk error(String agentName, String error) {
        return new StreamChunk(TYPE_ERROR, null, null, null, error, agentName);
    }
    public static StreamChunk done(String agentName) {
        return new StreamChunk(TYPE_DONE, null, null, null, null, agentName);
    }

    public String getType() { return type; }
    public Object getStep() { return step; }
    public String getOutput() { return output; }
    public TokenUsageDto getUsage() { return usage; }
    public String getErrorMessage() { return errorMessage; }
    public String getAgentName() { return agentName; }
}