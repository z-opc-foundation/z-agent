package com.zifang.z.agent.engine.agent.multi;

import com.zifang.z.agent.engine.agent.react.Agent;
import com.zifang.z.agent.engine.agent.react.ReActAgent;
import com.zifang.z.agent.engine.agent.react.StreamEvent;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker Agent（工作 Agent）。
 * 从 MessageQueue 接收 Supervisor 的 DELEGATION 消息, 执行任务并回复 (带 inReplyTo)。
 *
 * <p>设计:
 * <ul>
 *   <li>用 {@link #processMessages()} 主动拉取, 由 supervisor 负责调度 (线程池 or 一次性)</li>
 *   <li>回复的 RESPONSE 消息携带 {@code inReplyTo} = 原 DELEGATION 的 messageId, 方便 supervisor 关联</li>
 *   <li>不耦合 supervisor 内部状态, 可以独立 stream() 用于测试</li>
 * </ul>
 */
public class WorkerAgent implements Agent {

    private final String name;
    private final ReActAgent delegate;
    private final MessageQueue messageQueue;
    private final Blackboard blackboard;
    private final AtomicInteger processedCount = new AtomicInteger(0);

    public WorkerAgent(String name, ReActAgent delegate, MessageQueue messageQueue) {
        this(name, delegate, messageQueue, null);
    }

    public WorkerAgent(String name, ReActAgent delegate, MessageQueue messageQueue, Blackboard blackboard) {
        this.name = Objects.requireNonNull(name, "name");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.messageQueue = Objects.requireNonNull(messageQueue, "messageQueue");
        this.blackboard = blackboard;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Worker 主动从自己的入队里取一条消息 (targetAgent == this.name) 并处理。
     * 这是 worker 的主工作循环, 由 supervisor 调度 (一次性 or 后台线程)。
     */
    public void processMessages() {
        AgentMessage msg = messageQueue.takeFor(this.name);
        if (msg == null) {
            return;
        }
        if (msg.getType() == AgentMessage.MessageType.DELEGATION) {
            processDelegation(msg);
        }
    }

    private void processDelegation(AgentMessage msg) {
        String task = msg.getContent();
        String supervisorName = msg.getFromAgent();
        String conversationId = msg.getConversationId();
        String inReplyTo = msg.getMessageId();

        String result;
        try {
            // 把上下文塞进 blackboard (worker 可读 supervisor 写的 user_id/tenant_code 等)
            if (blackboard != null) {
                blackboard.put("current_task", task);
                blackboard.put("current_supervisor", supervisorName);
            }
            // Worker 实际干活
            result = delegate.run(task);
        } catch (Exception e) {
            // 失败: 发 ERROR 类型, 仍带 inReplyTo
            AgentMessage error = AgentMessage.builder()
                    .fromAgent(name)
                    .toAgent(supervisorName)
                    .type(AgentMessage.MessageType.ERROR)
                    .content(e.getMessage() == null ? e.getClass().getName() : e.getMessage())
                    .conversationId(conversationId)
                    .inReplyTo(inReplyTo)
                    .build();
            messageQueue.send(error);
            processedCount.incrementAndGet();
            return;
        }

        // 成功: 发 RESPONSE 消息, 关键 — 带 inReplyTo
        AgentMessage response = AgentMessage.builder()
                .fromAgent(name)
                .toAgent(supervisorName)
                .type(AgentMessage.MessageType.RESPONSE)
                .content(result == null ? "" : result)
                .conversationId(conversationId)
                .inReplyTo(inReplyTo)
                .build();
        messageQueue.send(response);
        processedCount.incrementAndGet();
    }

    /**
     * Worker 也支持直接 stream (用于独立使用, 测试, 或单步调用)。
     */
    @Override
    public Flux<StreamEvent> stream(String input) {
        return delegate.stream(input);
    }

    public int processedCount() {
        return processedCount.get();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }
}
