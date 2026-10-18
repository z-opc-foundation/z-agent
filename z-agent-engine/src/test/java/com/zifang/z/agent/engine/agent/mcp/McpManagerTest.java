package com.zifang.z.agent.engine.agent.mcp;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * McpManager 单元测试。
 * <p>
 * 当前 McpManager 是空类（占位实现），测试仅做：实例化、可作为共享对象传递、
 * 反射验证它的形态。这是为后续 MCP 实现预留的契约测试。
 */
public class McpManagerTest {

    @Test
    public void testMcpManager_canBeInstantiated() throws Exception {
        Constructor<McpManager> ctor = McpManager.class.getDeclaredConstructor();
        assertTrue(Modifier.isPublic(ctor.getModifiers()));
        McpManager mgr = ctor.newInstance();
        assertNotNull(mgr);
    }

    @Test
    public void testMcpManager_isSharedSafelyAcrossThreads() throws Exception {
        // 模拟"agent 持有 mcpManager，多线程访问同一实例"
        McpManager shared = new McpManager();
        AtomicReference<McpManager> refA = new AtomicReference<>();
        AtomicReference<McpManager> refB = new AtomicReference<>();
        Thread t1 = new Thread(() -> refA.set(shared));
        Thread t2 = new Thread(() -> refB.set(shared));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertSame(shared, refA.get());
        assertSame(shared, refB.get());
    }

    @Test
    public void testMcpManager_classShape_isPlaceHolderForFuture() {
        // 当 McpManager 真正实现 registerServer / callTool / listTools 时，本测试提醒维护者更新。
        // 现阶段：McpManager 应至少有 1 个 public 构造器（占位契约）。
        Constructor<?>[] ctors = McpManager.class.getDeclaredConstructors();
        assertEquals(1, ctors.length);
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
    }
}
