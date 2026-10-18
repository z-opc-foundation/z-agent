package com.zifang.z.agent.engine.agent.react;

/**
 * SubAgent — 子代理接口
 * <p>
 * 由父 Agent 通过 spawnSubAgent() 创建，拥有独立 ReAct 循环。
 * 与父共享 AgentContext（memory、config），但有独立的任务栈。
 * <p>
 * 生命周期：created → running → completed/cancelled
 */
public interface SubAgent {

    /**
     * 子代理名称（用于调试）
     */
    String getName();

    /**
     * 当前状态
     */
    State getState();

    /**
     * 任务描述
     */
    String getTask();

    /**
     * 父代理引用
     */
    SubAgent getParent();

    /**
     * 执行结果（完成前为 null）
     */
    String getResult();

    /**
     * 错误信息（失败时）
     */
    String getError();

    /**
     * 启动执行（异步）
     */
    void start();

    /**
     * 同步等待完成
     */
    void await() throws InterruptedException;

    /**
     * 强制终止
     */
    void kill();

    /**
     * 获取直接子代理
     */
    java.util.List<SubAgent> getChildren();

    // ========== 状态枚举 ==========
    enum State {
        CREATED,   // 已创建，未启动
        RUNNING,   // 运行中
        WAITING,   // 等待子代理
        COMPLETED, // 正常完成
        CANCELLED, // 被取消
        FAILED     // 执行异常
    }
}