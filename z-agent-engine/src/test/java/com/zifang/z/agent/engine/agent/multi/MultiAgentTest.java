package com.zifang.z.agent.engine.agent.multi;

import org.junit.Test;

import static org.junit.Assert.*;

public class MultiAgentTest {

    @Test
    public void testInMemoryMessageQueue_sendAndPoll() {
        InMemoryMessageQueue mq = new InMemoryMessageQueue();
        AgentMessage msg = AgentMessage.create("worker1", "supervisor",
                AgentMessage.MessageType.REQUEST, "需要指导");
        mq.send(msg);
        AgentMessage received = mq.poll();
        assertNotNull(received);
        assertEquals("worker1", received.getFromAgent());
        assertEquals("需要指导", received.getContent());
    }

    @Test
    public void testInMemoryMessageQueue_broadcast() {
        InMemoryMessageQueue mq = new InMemoryMessageQueue();
        AgentMessage broadcast = AgentMessage.create("supervisor", null,
                AgentMessage.MessageType.NOTIFICATION, "所有人注意");
        mq.send(broadcast);
        AgentMessage received = mq.poll();
        assertNotNull(received);
        assertNull(received.getToAgent()); // null 表示广播
    }

    @Test
    public void testAgentMessage_create() {
        AgentMessage msg = AgentMessage.create("a", "b", AgentMessage.MessageType.RESPONSE, "result");
        assertNotNull(msg.getMessageId());
        assertNotNull(msg.getConversationId());
        assertEquals("a", msg.getFromAgent());
        assertEquals("b", msg.getToAgent());
        assertEquals(AgentMessage.MessageType.RESPONSE, msg.getType());
        assertEquals("result", msg.getContent());
        assertTrue(msg.getTimestamp() > 0);
    }

    @Test
    public void testMessageQueue_clear() {
        InMemoryMessageQueue mq = new InMemoryMessageQueue();
        mq.send(AgentMessage.create("w1", "s1", AgentMessage.MessageType.REQUEST, "x"));
        assertFalse(mq.getMessages("s1").isEmpty());
        mq.clear("s1");
        assertTrue(mq.getMessages("s1").isEmpty());
    }
}