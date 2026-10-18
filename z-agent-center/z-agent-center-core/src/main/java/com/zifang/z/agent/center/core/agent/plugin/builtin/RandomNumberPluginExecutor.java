package com.zifang.z.agent.center.core.agent.plugin.builtin;

import com.zifang.z.agent.center.core.agent.plugin.executor.BeanPluginExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * FEATURE013 T2.1: 内置插件 #3 — 随机数生成
 *
 * <pre>
 * 输入: { "min": 1, "max": 100 }
 * 输出: { "value": 42 }
 * </pre>
 */
@Component("randomNumberPluginExecutor")
public class RandomNumberPluginExecutor implements BeanPluginExecutor {

    private static long toLong(Object o, long defaultVal) {
        if (o == null) {
            return defaultVal;
        }

        if (o instanceof Number) {
            return ((Number) o).longValue();
        }

        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return defaultVal;
        }
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        Map<String, Object> out = new HashMap<>();
        long min = toLong(inputs.get("min"), 0L);
        long max = toLong(inputs.get("max"), 100L);
        if (max < min) {
            throw new IllegalArgumentException("max < min: " + max + " < " + min);
        }
        long value = ThreadLocalRandom.current().nextLong(min, max + 1);
        out.put("value", value);
        return out;
    }
}
