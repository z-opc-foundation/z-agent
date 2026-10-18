package com.zifang.z.agent.center.core.agent.plugin.builtin;

import com.zifang.z.agent.center.core.agent.plugin.executor.BeanPluginExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * FEATURE013 T2.1: 内置插件 #1 — Echo 回显
 *
 * <p>把入参原样返回，常用于调试插件链路.
 * <pre>
 * 输入: { "text": "hello" }
 * 输出: { "echo": "hello" }
 * </pre>
 */
@Component("echoPluginExecutor")
public class EchoPluginExecutor implements BeanPluginExecutor {

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        Map<String, Object> out = new HashMap<>();
        out.put("echo", String.valueOf(inputs.get("text")));
        return out;
    }
}
