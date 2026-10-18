package com.zifang.z.agent.center.core.agent.flow;

import java.util.Collections;
import java.util.Map;

/**
 * 流程节点接口 - 所有节点类型需实现此接口
 */
public interface FlowNode {

    /**
     * 内置节点类型常量
     */
    static final String TYPE_START = "start";
    static final String TYPE_END = "end";
    static final String TYPE_AGENT = "agent";           // Agent节点
    static final String TYPE_SUB_FLOW = "sub_flow";     // 子流程
    static final String TYPE_CONDITION = "condition";   // 条件分支
    static final String TYPE_PARALLEL = "parallel";     // 并行执行
    static final String TYPE_LOOP = "loop";              // 循环
    static final String TYPE_SCRIPT = "script";         // 脚本
    static final String TYPE_HTTP = "http";              // HTTP调用
    static final String TYPE_TRANSFORM = "transform";   // 数据转换/模板
    static final String TYPE_LLM = "llm";                // LLM调用
    static final String TYPE_HUMAN_INPUT = "human_input"; // FEATURE013 T3.5: Human-in-the-Loop 节点

    /**
     * 节点类型标识
     */
    String getType();

    /**
     * FEATURE013 A8: 节点级超时 (毫秒). 默认 60s.
     * FlowEngine 会在节点执行时按此超时控制, 长 LLM 调用可单独配.
     */
    default long timeoutMs() {
        return 60_000L;
    }

    /**
     * 执行节点
     *
     * @param inputs  前置节点的输出
     * @param context 流程上下文
     * @return 节点输出
     */
    Object execute(Map<String, Object> inputs, FlowContext context);

    /**
     * 获取节点配置
     */
    default Map<String, Object> getConfig() {
        return Collections.emptyMap();
    }

    /**
     * 验证配置
     */
    default boolean validate() {
        return true;
    }
}