package com.zifang.z.agent.center.core.agent.plugin.builtin;

import com.zifang.z.agent.center.core.agent.plugin.executor.BeanPluginExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * FEATURE013 T2.1: 内置插件 #2 — 当前时间
 *
 * <pre>
 * 输入: { "format": "iso" | "epoch" | "human" }  (default: "iso")
 * 输出: { "timestamp": "..." }
 * </pre>
 */
@Component("currentTimePluginExecutor")
public class CurrentTimePluginExecutor implements BeanPluginExecutor {

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        String format = String.valueOf(inputs.getOrDefault("format", "iso")).toLowerCase();
        Map<String, Object> out = new HashMap<>();
        Instant now = Instant.now();
        switch (format) {
            case "epoch":
                out.put("timestamp", String.valueOf(now.getEpochSecond()));
                break;
            case "human":
                out.put("timestamp", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .format(LocalDateTime.ofInstant(now, ZoneId.systemDefault())));
                break;
            case "iso":
            default:
                out.put("timestamp", now.toString());
                break;
        }
        return out;
    }
}
