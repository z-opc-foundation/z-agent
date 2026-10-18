package com.zifang.z.agent.engine.agent.stream;

import reactor.core.publisher.Flux;

/**
 * 流式 Agent 接口。
 * 与 Agent 接口的区别：
 * - 返回 Flux<AgentEvent>（更丰富的事件类型）
 * - 支持 STREAMING_TEXT 逐字流式输出
 * - 支持 METADATA_EVENT 元数据推送
 */
public interface StreamingAgent {
    String name();

    /**
     * 启动流式执行。
     * 返回 Flux<AgentEvent>，订阅后开始执行。
     * 事件顺序（大致）：
     * 1. AgentStatus(RUNNING)
     * 2. StreamingText (thought 逐字)
     * 3. StepStart
     * 4. ToolCallRequest
     * 5. ToolResult
     * 6. StreamingText (final 逐字)
     * 7. Done
     * 8. AgentStatus(IDLE)
     */
    Flux<AgentEvent> streamEvents(String input);

    void stop();

    void reset();
}