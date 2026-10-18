package com.zifang.z.agent.engine.agent.observe;

import com.zifang.z.agent.engine.agent.observe.model.AgentSpan;
import com.zifang.z.agent.engine.agent.observe.model.AgentTrace;

/**
 * 追踪器接口。引擎定义，平台层实现。
 */
public interface AgentTracer {
    /**
     * 开始追踪
     */
    AgentTrace beginTrace(String sessionId, String agentName, String userGoal);

    /**
     * 开始一个 span
     */
    AgentSpan beginSpan(AgentSpan parent, String name);

    /**
     * 结束 span
     */
    void endSpan(AgentSpan span, String status, String errorMsg);

    /**
     * 结束追踪
     */
    void endTrace(AgentTrace trace, String status);

    /**
     * 获取当前 trace（线程本地）
     */
    AgentTrace currentTrace();
}