package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.tool.core.Tool;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Toolkit 独立单元测试（不依赖 ReActAgent）。
 * ReActAgent 对 Toolkit 的间接覆盖在 ReActAgentTest 里。
 */
public class ToolkitTest {

    @Test
    public void testRegisterAndRetrieve() {
        Toolkit tk = new Toolkit();
        Tool t = TestTools.addTool();
        tk.registerTool(t);
        assertEquals(1, tk.size());
        assertSame(t, tk.getTool("add"));
        assertTrue(tk.hasTool("add"));
        assertFalse(tk.hasTool("nope"));
    }

    @Test
    public void testRegister_nullTool_isIgnored() {
        Toolkit tk = new Toolkit();
        tk.registerTool(null);
        assertEquals(0, tk.size());
    }

    @Test
    public void testRegister_toolWithoutName_isIgnored() {
        Toolkit tk = new Toolkit();
        tk.registerTool(new Tool(null, "no name", null));
        assertEquals(0, tk.size());
    }

    @Test
    public void testRemoveTool() {
        Toolkit tk = new Toolkit();
        tk.registerTool(TestTools.addTool());
        assertEquals(1, tk.size());
        tk.removeTool("add");
        assertEquals(0, tk.size());
        assertFalse(tk.hasTool("add"));
    }

    @Test
    public void testClear() {
        Toolkit tk = new Toolkit();
        tk.registerTool(TestTools.addTool());
        tk.registerTool(TestTools.echoTool());
        assertEquals(2, tk.size());
        tk.clear();
        assertEquals(0, tk.size());
    }

    @Test
    public void testGetToolNames() {
        Toolkit tk = new Toolkit();
        tk.registerTool(TestTools.addTool());
        tk.registerTool(TestTools.echoTool());
        assertEquals(2, tk.getToolNames().size());
        assertTrue(tk.getToolNames().contains("add"));
        assertTrue(tk.getToolNames().contains("echo"));
    }

    @Test
    public void testExecute_successResult() {
        Toolkit tk = new Toolkit();
        AtomicInteger called = new AtomicInteger();
        tk.registerTool(TestTools.echoTool(), (name, args) -> {
            called.incrementAndGet();
            return "echoed: " + args.get("message");
        });
        Toolkit.ToolResult r = tk.execute("echo", Collections.singletonMap("message", "hi"));
        assertTrue(r.isSuccess());
        assertEquals(1, called.get());
        assertEquals("echoed: hi", r.getResultAsString());
    }

    @Test
    public void testExecute_unknownTool_returnsError() {
        Toolkit tk = new Toolkit();
        Toolkit.ToolResult r = tk.execute("nope", new HashMap<>());
        assertFalse(r.isSuccess());
        assertNotNull(r.getError());
        assertTrue(r.getError().contains("nope"));
    }

    @Test
    public void testExecute_executorThrows_returnsErrorResult() {
        Toolkit tk = new Toolkit();
        tk.registerTool(TestTools.echoTool(), (name, args) -> {
            throw new RuntimeException("boom");
        });
        Toolkit.ToolResult r = tk.execute("echo", Collections.singletonMap("message", "x"));
        assertFalse(r.isSuccess());
        assertEquals("boom", r.getError());
        assertNull(r.getResult());
        assertEquals("boom", r.getResultAsString()); // error fallback
    }

    @Test
    public void testExecute_nullExecutor_returnsError() {
        Toolkit tk = new Toolkit();
        // 不带 executor 注册
        tk.registerTool(TestTools.echoTool());
        Toolkit.ToolResult r = tk.execute("echo", Collections.singletonMap("message", "x"));
        assertFalse(r.isSuccess());
        assertTrue(r.getError().contains("executor"));
    }

    @Test
    public void testGetToolsDescription_includesAllTools() {
        Toolkit tk = new Toolkit();
        tk.registerTool(TestTools.addTool());
        tk.registerTool(TestTools.echoTool());
        String desc = tk.getToolsDescription();
        assertTrue(desc.contains("add"));
        assertTrue(desc.contains("echo"));
    }

    @Test
    public void testBuilder_createsToolkitWithTools() {
        Toolkit tk = new Toolkit.Builder()
                .addTool(TestTools.addTool(), TestTools.fixedExecutor("0"))
                .addTool(TestTools.echoTool())
                .build();
        assertEquals(2, tk.size());
        assertTrue(tk.hasTool("add"));
        assertTrue(tk.hasTool("echo"));
        assertNotNull(tk.getExecutor("add"));
        assertNull(tk.getExecutor("echo")); // registered without executor
    }

    @Test
    public void testGetAllTools() {
        Toolkit tk = new Toolkit();
        Tool t1 = TestTools.addTool();
        Tool t2 = TestTools.echoTool();
        tk.registerTool(t1);
        tk.registerTool(t2);
        assertEquals(2, tk.getAllTools().size());
    }
}
