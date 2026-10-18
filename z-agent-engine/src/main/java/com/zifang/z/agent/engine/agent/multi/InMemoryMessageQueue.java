package com.zifang.z.agent.engine.agent.multi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 基于 JDK 容器和 BlockingQueue 的内存消息队列
 */
public class InMemoryMessageQueue implements MessageQueue {

    private final Map<String, BlockingQueue<AgentMessage>> queues = new ConcurrentHashMap<>();
    private final BlockingQueue<AgentMessage> broadcastQueue = new LinkedBlockingQueue<>();

    private BlockingQueue<AgentMessage> getQueue(String agentName) {
        return queues.computeIfAbsent(agentName, k -> new LinkedBlockingQueue<>());
    }

    @Override
    public void send(AgentMessage message) {
        if (message.getToAgent() == null) {
            broadcastQueue.offer(message);
        } else {
            getQueue(message.getToAgent()).offer(message);
        }
    }

    @Override
    public AgentMessage take() {
        try {
            return broadcastQueue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public AgentMessage takeFor(String agentName) {
        try {
            return getQueue(agentName).take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public AgentMessage poll() {
        AgentMessage msg = broadcastQueue.poll();
        if (msg != null) {
            return msg;
        }
        for (BlockingQueue<AgentMessage> q : queues.values()) {
            msg = q.poll();
            if (msg != null) {
                return msg;
            }
        }
        return null;
    }

    @Override
    public AgentMessage pollFor(String agentName) {
        BlockingQueue<AgentMessage> q = queues.get(agentName);
        if (q == null) {
            return null;
        }
        return q.poll();
    }

    @Override
    public List<AgentMessage> getMessages(String agentName) {
        List<AgentMessage> result = new ArrayList<>();
        BlockingQueue<AgentMessage> q = queues.get(agentName);
        if (q != null) {
            q.drainTo(result);
        }
        return result;
    }

    @Override
    public void clear(String agentName) {
        queues.remove(agentName);
    }
}