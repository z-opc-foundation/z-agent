package com.zifang.z.agent.engine.llm.tool.core;

import com.zifang.z.agent.engine.agent.llm.tool.core.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Tool 体系单元测试：覆盖 Tool / ToolParam / ToolRegistry / NanoBotTool 接口 /
 * ToolCall / ToolCallRequest / ToolExecutor functional interface.
 */
public class ToolCoreTest {

    // ==================== Tool & ToolParam ====================

    @Test
    public void testTool_basicGettersAndSetters() {
        List<ToolParam> params = Arrays.asList(
                new ToolParam("x", "integer", true, "first"),
                new ToolParam("y", "integer", true, "second"));
        Tool t = new Tool("add", "Add two ints", params);
        assertEquals("add", t.getName());
        assertEquals("Add two ints", t.getDesc());
        assertEquals(2, t.getParams().size());
    }

    @Test
    public void testTool_toDesc_includesNameAndParams() {
        Tool t = new Tool("greet", "Greet someone",
                Collections.singletonList(new ToolParam("name", "string", true, "who to greet")));
        String desc = t.toDesc();
        assertTrue(desc.contains("greet"));
        assertTrue(desc.contains("name"));
        assertTrue(desc.contains("必填"));
    }

    @Test
    public void testTool_toDesc_optionalParamNotMarkedRequired() {
        Tool t = new Tool("echo", "Echo with optional prefix",
                Collections.singletonList(new ToolParam("prefix", "string", false, "optional prefix")));
        String desc = t.toDesc();
        assertTrue(desc.contains("prefix"));
        assertFalse("optional param must not be marked 必填", desc.contains("必填"));
    }

    @Test
    public void testToolParam_roundTrip() {
        ToolParam p = new ToolParam("count", "integer", true, "iteration count");
        assertEquals("count", p.getName());
        assertEquals("integer", p.getType());
        assertTrue(p.isRequired());
        assertEquals("iteration count", p.getDesc());

        p.setRequired(false);
        assertFalse(p.isRequired());
    }

    // ==================== ToolRegistry 单例 + 默认工具 ====================

    @Test
    public void testToolRegistry_singletonReturnsSameInstance() {
        ToolRegistry a = ToolRegistry.getInstance();
        ToolRegistry b = ToolRegistry.getInstance();
        assertSame(a, b);
    }

    @Test
    public void testToolRegistry_containsBuiltinReadFile() {
        ToolRegistry reg = ToolRegistry.getInstance();
        String result = reg.executeTool("read_file", Collections.singletonMap("path", "x.txt"));
        assertNotNull(result);
        assertTrue(result.contains("x.txt"));
    }

    @Test
    public void testToolRegistry_containsBuiltinExec() {
        ToolRegistry reg = ToolRegistry.getInstance();
        String result = reg.executeTool("exec", Collections.singletonMap("command", "ls"));
        assertTrue(result.contains("ls"));
    }

    @Test
    public void testToolRegistry_unknownToolReturnsError() {
        ToolRegistry reg = ToolRegistry.getInstance();
        String result = reg.executeTool("no_such_tool", new HashMap<>());
        assertTrue(result.contains("未找到工具") || result.contains("no_such_tool"));
    }

    @Test
    public void testToolRegistry_registerCustomTool() {
        ToolRegistry reg = ToolRegistry.getInstance();
        NanoBotTool custom = new NanoBotTool() {
            @Override
            public String getToolName() {
                return "my_test_tool";
            }

            @Override
            public String getToolDescription() {
                return "a custom test tool";
            }

            @Override
            public String execute(Map<String, Object> parameters) {
                return "custom-ok:" + parameters.get("k");
            }
        };
        reg.registerTool(custom);
        String out = reg.executeTool("my_test_tool", Collections.singletonMap("k", "v"));
        assertEquals("custom-ok:v", out);
        // cleanup
        reg.registerTool(null); // best-effort cleanup: null ignored
    }

    @Test
    public void testToolRegistry_getToolsDescription_containsFormatHint() {
        ToolRegistry reg = ToolRegistry.getInstance();
        String desc = reg.getToolsDescription();
        assertTrue(desc.contains("工具调用格式"));
        assertTrue(desc.contains("tool"));
    }

    @Test
    public void testToolRegistry_registerNullOrBlankName_isIgnored() {
        ToolRegistry reg = ToolRegistry.getInstance();
        int before = reg.executeTool("never_registered", new HashMap<>()).length();
        reg.registerTool(null);
        reg.registerTool(new NanoBotTool() {
            @Override
            public String getToolName() {
                return "";
            }

            @Override
            public String getToolDescription() {
                return "";
            }

            @Override
            public String execute(Map<String, Object> parameters) {
                return "";
            }
        });
        // 行为：null/blank 名字的工具不应被注册
        int after = reg.executeTool("", new HashMap<>()).length();
        // 仍返回错误，不抛异常
        assertTrue(after > 0);
    }

    // ==================== ToolCall / ToolCallRequest ====================

    @Test
    public void testToolCall_constructorAndGetters() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("x", 1);
        args.put("y", 2);
        ToolCall tc = new ToolCall("add", args);
        assertEquals("add", tc.getToolName());
        assertEquals(1, tc.getArgs().get("x"));
        assertEquals(2, tc.getArgs().get("y"));
    }

    @Test
    public void testToolCall_defaultConstructor() {
        ToolCall tc = new ToolCall();
        tc.setToolName("foo");
        tc.setArgs(Collections.singletonMap("k", "v"));
        assertEquals("foo", tc.getToolName());
        assertEquals("v", tc.getArgs().get("k"));
    }

    @Test
    public void testToolCallRequest_serializeArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("name", "alice");
        args.put("age", 30);
        ToolCallRequest req = new ToolCallRequest("greet", args);
        assertEquals("greet", req.getToolName());
        assertEquals(2, req.getParameters().size());
    }

    // ==================== ToolExecutor functional interface ====================

    @Test
    public void testToolExecutor_functionalInterface() {
        ToolExecutor sum = args -> {
            int a = ((Number) args.get("a")).intValue();
            int b = ((Number) args.get("b")).intValue();
            return String.valueOf(a + b);
        };
        Map<String, Object> args = new HashMap<>();
        args.put("a", 2);
        args.put("b", 3);
        assertEquals("5", sum.execute(args));
    }
}
