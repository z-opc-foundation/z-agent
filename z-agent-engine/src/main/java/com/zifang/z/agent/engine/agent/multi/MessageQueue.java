package com.zifang.z.agent.engine.agent.multi;

import java.util.List;

/**
 * Agent 消息队列接口。
 * 用于 Supervisor 和 Worker 之间的异步消息传递。
 * 引擎定义接口，平台层实现（可用 JDK BlockQueue/Disruptor/RocketMQ）
 */
public interface MessageQueue {

    /**
     * 发送消息
     */
    void send(AgentMessage message);

    /**
     * 接收消息 (阻塞), 从广播队列拉。
     */
    AgentMessage take();

    /**
     * 接收发给指定 agent 的消息 (阻塞)。Worker 用此拉取自己的入队。
     */
    AgentMessage takeFor(String agentName);

    /**
     * 尝试接收 (不阻塞), 任意队列都算。
     */
    AgentMessage poll();

    /**
     * 尝试接收发给指定 agent 的消息 (不阻塞)。
     */
    AgentMessage pollFor(String agentName);

    /**
     * 获取指定 agent 的待处理消息
     */
    List<AgentMessage> getMessages(String agentName);

    /**
     * 清除指定 agent 的消息
     */
    void clear(String agentName);
}