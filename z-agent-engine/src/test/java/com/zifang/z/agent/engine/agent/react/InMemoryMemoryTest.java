package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.model.ChatMessage;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * InMemoryMemory 独立单元测试（不依赖 ReActAgent 间接验证）。
 */
public class InMemoryMemoryTest {

    @Test
    public void testDefaultCapacity_is100() {
        InMemoryMemory m = new InMemoryMemory();
        for (int i = 0; i < 150; i++) m.add(Msg.user("m" + i));
        assertEquals(100, m.size());
    }

    @Test
    public void testCustomCapacity() {
        InMemoryMemory m = new InMemoryMemory(5);
        for (int i = 0; i < 10; i++) m.add(Msg.user("m" + i));
        assertEquals(5, m.size());
        // 保留最新 5 条
        List<Msg> msgs = m.getConversationMessages();
        assertEquals("m5", msgs.get(0).getContent());
        assertEquals("m9", msgs.get(4).getContent());
    }

    @Test
    public void testSystemPrompt_separateFromConversation() {
        InMemoryMemory m = new InMemoryMemory();
        m.addSystemPrompt("You are helpful");
        m.add(Msg.user("hi"));
        m.add(Msg.assistant("hello"));

        // conversation messages 不含 system
        List<Msg> conv = m.getConversationMessages();
        assertEquals(2, conv.size());
        assertEquals("user", conv.get(0).getRole());
        assertEquals("assistant", conv.get(1).getRole());

        // getMessages 包含 system
        List<Msg> all = m.getMessages();
        assertEquals(3, all.size());
        assertEquals("system", all.get(0).getRole());
    }

    @Test
    public void testAddSystemPrompt_nullOrEmpty_isIgnored() {
        InMemoryMemory m = new InMemoryMemory();
        m.addSystemPrompt(null);
        m.addSystemPrompt("");
        assertTrue(m.isEmpty());
    }

    @Test
    public void testClear_keepsSystemPrompt() {
        InMemoryMemory m = new InMemoryMemory();
        m.addSystemPrompt("sys");
        m.add(Msg.user("hi"));
        m.add(Msg.assistant("hello"));
        m.clear();
        assertEquals(0, m.size());
        // getMessages 仍含 system
        assertEquals(1, m.getMessages().size());
        assertEquals("system", m.getMessages().get(0).getRole());
    }

    @Test
    public void testClearAll_clearsEverything() {
        InMemoryMemory m = new InMemoryMemory();
        m.addSystemPrompt("sys");
        m.add(Msg.user("hi"));
        m.clearAll();
        assertTrue(m.isEmpty());
        assertEquals(0, m.getMessages().size());
    }

    @Test
    public void testGetLastMessages_returnsMostRecentN() {
        InMemoryMemory m = new InMemoryMemory();
        m.add(Msg.user("a"));
        m.add(Msg.user("b"));
        m.add(Msg.user("c"));
        m.add(Msg.user("d"));

        List<Msg> last2 = m.getLastMessages(2);
        assertEquals(2, last2.size());
        assertEquals("c", last2.get(0).getContent());
        assertEquals("d", last2.get(1).getContent());
    }

    @Test
    public void testGetLastMessages_zeroOrNegative_returnsEmpty() {
        InMemoryMemory m = new InMemoryMemory();
        m.add(Msg.user("a"));
        assertTrue(m.getLastMessages(0).isEmpty());
        assertTrue(m.getLastMessages(-5).isEmpty());
    }

    @Test
    public void testGetLastMessages_largerThanSize_returnsAll() {
        InMemoryMemory m = new InMemoryMemory();
        m.add(Msg.user("a"));
        m.add(Msg.user("b"));
        List<Msg> last10 = m.getLastMessages(10);
        assertEquals(2, last10.size());
    }

    @Test
    public void testToChatMessages_convertsRolesCorrectly() {
        InMemoryMemory m = new InMemoryMemory();
        m.addSystemPrompt("sys");
        m.add(Msg.user("hi"));
        m.add(Msg.assistant("hello"));
        m.add(Msg.toolResult("call_1", "echo", "result"));

        List<ChatMessage> chatMsgs = m.toChatMessages();
        assertEquals(4, chatMsgs.size());
        assertEquals("system", chatMsgs.get(0).getRole());
        assertEquals("user", chatMsgs.get(1).getRole());
        assertEquals("assistant", chatMsgs.get(2).getRole());
        assertEquals("tool", chatMsgs.get(3).getRole());
        // tool result must carry toolCallId
        assertEquals("call_1", chatMsgs.get(3).getToolCallId());
        assertEquals("echo", chatMsgs.get(3).getName());
    }

    @Test
    public void testCreateWithSystemPrompt_factory() {
        InMemoryMemory m = InMemoryMemory.createWithSystemPrompt("sys prompt");
        assertEquals(1, m.getMessages().size());
        assertEquals("system", m.getMessages().get(0).getRole());
    }

    @Test
    public void testAddNull_isIgnored() {
        InMemoryMemory m = new InMemoryMemory();
        m.add(null);
        assertTrue(m.isEmpty());
    }
}
