package com.zifang.z.agent.engine.agent.memory;

import com.zifang.z.agent.engine.agent.memory.model.MemoryEntry;
import com.zifang.z.agent.engine.agent.memory.store.InMemorySessionMemoryStore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MemoryManagerTest {

    @Test
    public void testShortTerm_lruEviction() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(3);
        MemoryManager mm = new MemoryManager(store, null);
        mm.append("s1", "msg1", MemoryEntry.MemoryType.SHORT, null);
        mm.append("s1", "msg2", MemoryEntry.MemoryType.SHORT, null);
        mm.append("s1", "msg3", MemoryEntry.MemoryType.SHORT, null);
        mm.append("s1", "msg4", MemoryEntry.MemoryType.SHORT, null); // 超过 max=3，msg1 应被淘汰

        List<MemoryEntry> history = mm.getConversationHistory("s1", 10);
        assertTrue(history.stream().anyMatch(e -> e.getContent().equals("msg4")));
        assertFalse(history.stream().anyMatch(e -> e.getContent().equals("msg1")));
    }

    @Test
    public void testLongTerm_notEvicted() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(1);
        MemoryManager mm = new MemoryManager(store, store);
        mm.append("s1", "short1", MemoryEntry.MemoryType.SHORT, null);
        mm.append("s1", "short2", MemoryEntry.MemoryType.SHORT, null);
        mm.saveToLongTerm("s1", "long1", null);
        mm.saveToLongTerm("s1", "long2", null);

        List<MemoryEntry> history = mm.getConversationHistory("s1", 10);
        assertEquals(2, history.stream().filter(e -> e.getType() == MemoryEntry.MemoryType.LONG).count());
    }

    /**
     * 2026-10-04 补：同一毫秒里追加的多条记忆必须都留住。
     *
     * <p>这条是被 testShouldDecompose / testBuildMemoryPrompt 两条红项逼出来的：
     * 原来 store 的 map key 是 {@code sessionId + "_" + timestamp}，timestamp 是
     * {@code currentTimeMillis()}，毫秒级不唯一 ⇒ 同一毫秒追加的条目互相覆盖。
     * 实测追加 7 条只剩 1 条，于是 shouldDecompose 永远 false、buildMemoryPrompt
     * 只喂给 LLM 最后一条。
     *
     * <p>本条把"毫秒内"这个前提<b>写死</b>：显式把每条的 timestamp 设成同一个值
     * （setTimestamp 是公开的），这样不依赖测试机快慢，每次跑都在撞同一条路径。
     */
    @Test
    public void test同毫秒内追加的多条记忆都要留住() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(100);
        MemoryManager mm = new MemoryManager(store, null);
        long sameMillis = 1700000000000L;
        for (int i = 0; i < 7; i++) {
            MemoryEntry e = new MemoryEntry("s1", "msg" + i, MemoryEntry.MemoryType.SHORT, null);
            e.setTimestamp(sameMillis);          // 强制同毫秒
            store.save(e);
        }
        assertEquals("同毫秒追加的 7 条被覆盖成了 " + store.size("s1") + " 条 —— "
                + "key 不能拿 currentTimeMillis 当身份，它不是唯一标识", 7, store.size("s1"));
        String prompt = mm.buildMemoryPrompt("s1", 10);
        for (int i = 0; i < 7; i++) {
            assertTrue("msg" + i + " 应在对话历史里, got: " + prompt, prompt.contains("msg" + i));
        }
    }

    /** 反过来:同一个对象被重新 save 一次仍是一条（原地更新语义不能被 id 改动破坏）。 */
    @Test
    public void test同一个条目重复保存不产生重复() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(100);
        MemoryEntry e = new MemoryEntry("s1", "hello", MemoryEntry.MemoryType.SHORT, null);
        store.save(e);
        store.save(e);
        assertEquals("同一对象重复 save 应覆盖而不是多出一条", 1, store.size("s1"));
    }

    @Test
    public void testShouldDecompose() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(100);
        MemoryManager mm = new MemoryManager(store, null);

        assertFalse(mm.shouldDecompose("s1", 5));
        for (int i = 0; i < 7; i++) {
            mm.append("s1", "msg" + i, MemoryEntry.MemoryType.SHORT, null);
        }
        assertTrue(mm.shouldDecompose("s1", 5));
    }

    @Test
    public void testClearSession() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(100);
        MemoryManager mm = new MemoryManager(store, store);

        mm.append("s1", "msg1", MemoryEntry.MemoryType.SHORT, null);
        mm.saveToLongTerm("s1", "long1", null);
        assertFalse(mm.getConversationHistory("s1", 10).isEmpty());

        mm.clearSession("s1");
        assertTrue(mm.getConversationHistory("s1", 10).isEmpty());
    }

    @Test
    public void testBuildMemoryPrompt() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(100);
        MemoryManager mm = new MemoryManager(store, null);

        mm.append("s1", "hello", MemoryEntry.MemoryType.SHORT, null);
        mm.append("s1", "world", MemoryEntry.MemoryType.SHORT, null);

        String prompt = mm.buildMemoryPrompt("s1", 10);
        assertNotNull(prompt);
        assertTrue(prompt.contains("[对话历史]"));
        assertTrue(prompt.contains("hello"));
        assertTrue(prompt.contains("world"));
    }

    @Test
    public void testBuildMemoryPrompt_empty() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(100);
        MemoryManager mm = new MemoryManager(store, null);

        String prompt = mm.buildMemoryPrompt("nonexistent", 10);
        assertEquals("", prompt);
    }

    @Test
    public void testMultiSession_isolation() {
        InMemorySessionMemoryStore store = new InMemorySessionMemoryStore(10);
        MemoryManager mm = new MemoryManager(store, null);

        mm.append("s1", "msg1_s1", MemoryEntry.MemoryType.SHORT, null);
        mm.append("s2", "msg1_s2", MemoryEntry.MemoryType.SHORT, null);

        List<MemoryEntry> historyS1 = mm.getConversationHistory("s1", 10);
        List<MemoryEntry> historyS2 = mm.getConversationHistory("s2", 10);

        assertEquals(1, historyS1.size());
        assertEquals(1, historyS2.size());
        assertEquals("msg1_s1", historyS1.get(0).getContent());
        assertEquals("msg1_s2", historyS2.get(0).getContent());
    }
}