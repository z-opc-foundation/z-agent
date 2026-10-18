package com.zifang.z.agent.engine.agent.react;

import com.zifang.z.agent.engine.agent.llm.tool.core.Tool;
import com.zifang.z.agent.engine.agent.llm.tool.core.ToolParam;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ReAct 测试用的工具工厂 + 工具执行追踪。
 * 把"工具定义"和"工具执行记录"打包成单例，便于多测试复用。
 */
public final class TestTools {

    private TestTools() {
    }

    /**
     * 简单加法工具。
     */
    public static Tool addTool() {
        return new Tool("add", "Add two integers and return the sum.",
                Arrays.asList(
                        new ToolParam("a", "integer", true, "first addend"),
                        new ToolParam("b", "integer", true, "second addend")));
    }

    /**
     * 用户查询工具。
     */
    public static Tool userLookupTool() {
        return new Tool("user_lookup", "Look up an internal user record by user id.",
                Collections.singletonList(
                        new ToolParam("user_id", "string", true, "the unique user id")));
    }

    /**
     * 字符串回声工具（无参）。
     */
    public static Tool echoTool() {
        return new Tool("echo", "Echo back a given string.",
                Collections.singletonList(
                        new ToolParam("message", "string", true, "the message to echo")));
    }

    /**
     * add 工具的测试执行器：把 string 参数解析为 int，返回 a + b，记录入参。
     */
    public static Toolkit.ToolExecutor addExecutor(AtomicInteger callCount, AtomicReference<Map<String, Object>> lastArgs) {
        return (name, args) -> {
            callCount.incrementAndGet();
            lastArgs.set(args);
            int a = Integer.parseInt(String.valueOf(args.get("a")));
            int b = Integer.parseInt(String.valueOf(args.get("b")));
            return a + b;
        };
    }

    /**
     * 通用字符串返回执行器。
     */
    public static Toolkit.ToolExecutor fixedExecutor(String returnValue) {
        return (name, args) -> returnValue;
    }

    /**
     * 抛异常的 executor，模拟工具内部错误。
     */
    public static Toolkit.ToolExecutor throwingExecutor(String errorMessage) {
        return (name, args) -> {
            throw new RuntimeException(errorMessage);
        };
    }

    /**
     * 倒序拼接所有参数为字符串。
     */
    public static Toolkit.ToolExecutor stringArgsExecutor() {
        return (name, args) -> {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Map.Entry<String, Object> e : args.entrySet()) {
                if (!first) {
                    sb.append(",");
                }

                sb.append(e.getKey()).append("=").append(e.getValue());
                first = false;
            }
            sb.append("]");
            return sb.toString();
        };
    }
}
