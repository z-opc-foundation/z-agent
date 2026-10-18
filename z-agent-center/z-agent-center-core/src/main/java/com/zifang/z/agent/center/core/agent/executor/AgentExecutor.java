package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.center.core.agent.flow.FlowContext;

/**
 * Agent执行器接口
 * 支持两种模式:
 * - IN_PROCESS: 内存中执行ReAct循环
 * - CONTAINER: 调用远程Hermes容器
 */
public interface AgentExecutor {

    /**
     * 执行对话
     *
     * @param instanceCode 实例编码
     * @param userMessage  用户消息
     * @param userId       用户ID
     * @param userName     用户名
     * @return 回复内容
     */
    String execute(String instanceCode, String userMessage, String userId, String userName);

    /**
     * 带流程上下文执行（用于流程编排）
     *
     * @param instanceCode 实例编码
     * @param userMessage  用户消息
     * @param context      流程上下文
     * @return 回复内容或结构化结果
     */
    default Object executeWithContext(String instanceCode, String userMessage, FlowContext context) {
        // 默认实现: 忽略上下文
        return execute(instanceCode, userMessage, context.getUserId(), context.getUserName());
    }

    /**
     * 获取执行器类型
     */
    String getType();

    /**
     * 健康检查
     */
    default boolean isHealthy() {
        return true;
    }
}