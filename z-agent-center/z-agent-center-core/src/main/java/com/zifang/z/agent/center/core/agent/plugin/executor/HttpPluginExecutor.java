package com.zifang.z.agent.center.core.agent.plugin.executor;

import com.zifang.util.json.JsonUtil;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * FEATURE013 T2.1: Plugin Executor — HTTP 模式
 *
 * <p>把 inputs 当作 JSON body POST 到 execute_config.url,
 * 把响应 JSON 当作出参. 也支持 query params (GET) 与 headers.
 *
 * <p>execute_config JSON 字段约定:
 * <pre>
 * {
 *   "url":    "https://api.example.com/...",
 *   "method": "GET" | "POST" | "PUT" | "DELETE" | "PATCH" | "HEAD",
 *   "headers": { "X-Token": "..." } (可选, 也可被 auth 模块注入)
 *   "queryParams": ["key1","key2"] (GET 时把这些 inputs 字段当 query param)
 * }
 * </pre>
 */
@Component
public class HttpPluginExecutor {

    private static final Logger log = LogManager.getLogger(HttpPluginExecutor.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(Map<String, Object> executeConfig,
                                       Map<String, Object> inputs,
                                       Integer timeoutMs) throws Exception {
        String url = (String) executeConfig.get("url");
        String method = String.valueOf(executeConfig.getOrDefault("method", "POST")).toUpperCase();
        Map<String, Object> headers = (Map<String, Object>) executeConfig.getOrDefault("headers", new HashMap<>());
        Object queryParamsObj = executeConfig.get("queryParams");

        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("execute_config.url 不能为空");
        }

        Request.Builder rb = new Request.Builder().url(url);
        for (Map.Entry<String, Object> h : headers.entrySet()) {
            rb.addHeader(h.getKey(), String.valueOf(h.getValue()));
        }

        Request request;
        if ("GET".equals(method) || "HEAD".equals(method)) {
            String finalUrl = appendQueryParams(url, inputs, queryParamsObj);
            rb.url(finalUrl);
            request = rb.method(method, null).build();
        } else {
            String bodyJson = JsonUtil.toJsonExcludingNulls(inputs != null ? inputs : new HashMap<>());
            request = rb.method(method, RequestBody.create(bodyJson, JSON)).build();
        }

        long startMs = System.currentTimeMillis();
        try (Response response = httpClient.newCall(request).execute()) {
            long elapsed = System.currentTimeMillis() - startMs;
            int status = response.code();
            ResponseBody body = response.body();
            String text = body != null ? body.string() : "";
            Map<String, Object> result = new HashMap<>();
            result.put("statusCode", status);
            result.put("responseTimeMs", elapsed);
            try {
                Object parsed = JsonUtil.toPlainValue(JsonUtil.parse(text));
                result.put("body", parsed);
            } catch (Exception jsonErr) {
                result.put("body", text);
            }
            Map<String, String> respHeaders = new HashMap<>();
            for (String name : response.headers().names()) {
                respHeaders.put(name, response.header(name));
            }
            result.put("headers", respHeaders);
            if (status >= 400) {
                throw new RuntimeException("HTTP " + status + ": " + text);
            }
            return result;
        } catch (Exception e) {
            log.warn("[HttpPluginExecutor] 执行失败 url={} method={}: {}", url, method, e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private String appendQueryParams(String url, Map<String, Object> inputs, Object queryParamsObj) {
        if (queryParamsObj == null || inputs == null || inputs.isEmpty()) {
            return url;
        }
        List<String> keys;
        if (queryParamsObj instanceof List) {
            keys = (List<String>) queryParamsObj;
        } else {
            return url;
        }
        StringBuilder sb = new StringBuilder(url);
        boolean first = !url.contains("?");
        for (String key : keys) {
            Object val = inputs.get(key);
            if (val == null) {
                continue;
            }
            try {
                sb.append(first ? '?' : '&').append(key).append('=')
                        .append(java.net.URLEncoder.encode(String.valueOf(val), StandardCharsets.UTF_8.name()));
            } catch (java.io.UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 encoding not supported", e);
            }
            first = false;
        }
        return sb.toString();
    }
}
