package com.zifang.z.agent.center.core.agent.flow.node;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.agent.flow.FlowContext;
import com.zifang.z.agent.center.core.agent.flow.FlowNode;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP节点 - 发起HTTP请求
 * 支持GET/POST/PUT/DELETE/PATCH方法
 * 支持请求体、header、query参数配置
 */
public class HttpNode implements FlowNode {

    private static final Logger log = LogManager.getLogger(HttpNode.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final String nodeId;
    private final String url;
    private final String method;
    private final Map<String, Object> config = new HashMap<>();

    private OkHttpClient httpClient;

    /**
     * 请求头
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * URL参数
     */
    private Map<String, String> queryParams = new LinkedHashMap<>();

    /**
     * 请求体模板
     */
    private String bodyTemplate;

    /**
     * 响应映射配置
     */
    private Map<String, String> responseMapping = new HashMap<>();

    /**
     * 超时时间(ms)
     */
    private long timeoutMs = 30000;

    /**
     * 是否跟随重定向
     */
    private boolean followRedirects = true;

    public HttpNode(String nodeId, String url, String method) {
        this.nodeId = nodeId;
        this.url = url;
        this.method = method.toUpperCase();
        initDefaults();
    }

    private void initDefaults() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .followRedirects(followRedirects)
                .build();
    }

    @Override
    public String getType() {
        return TYPE_HTTP;
    }

    @Override
    public Object execute(Map<String, Object> inputs, FlowContext context) {
        log.info("HttpNode[{}] executing {} request to: {}", nodeId, method, url);

        try {
            // 1. 构建完整的URL（带查询参数）
            String fullUrl = buildUrl(inputs, context);

            // 2. 构建请求头
            Headers requestHeaders = buildHeaders(inputs, context);

            // 3. 构建请求体
            RequestBody requestBody = buildRequestBody(inputs, context);

            // 4. 创建并执行请求
            Request request = new Request.Builder()
                    .url(fullUrl)
                    .headers(requestHeaders)
                    .method(method, requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                // 5. 处理响应
                return processResponse(response, context);
            }

        } catch (IOException e) {
            log.error("HttpNode[{}] request failed: {}", nodeId, e.getMessage(), e);
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private String buildUrl(Map<String, Object> inputs, FlowContext context) {
        String fullUrl = url;

        // 替换URL中的变量占位符 {varName}}
        fullUrl = replaceVariables(fullUrl, inputs, context);

        // 添加查询参数
        if (!queryParams.isEmpty()) {
            StringBuilder sb = new StringBuilder(fullUrl);
            boolean first = !fullUrl.contains("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                String value = replaceVariables(entry.getValue(), inputs, context);
                if (first) {
                    sb.append("?");
                    first = false;
                } else {
                    sb.append("&");
                }
                sb.append(entry.getKey()).append("=").append(value);
            }
            fullUrl = sb.toString();
        }

        return fullUrl;
    }

    private Headers buildHeaders(Map<String, Object> inputs, FlowContext context) {
        Headers.Builder builder = new Headers.Builder();

        // 默认Content-Type
        if (!headers.containsKey("Content-Type") && bodyTemplate != null) {
            builder.add("Content-Type", "application/json");
        }

        // 添加配置的header
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String value = replaceVariables(entry.getValue(), inputs, context);
            builder.add(entry.getKey(), value);
        }

        return builder.build();
    }

    private RequestBody buildRequestBody(Map<String, Object> inputs, FlowContext context) {
        if (bodyTemplate == null || method.equals("GET") || method.equals("DELETE")) {
            return null;
        }

        String bodyContent = replaceVariables(bodyTemplate, inputs, context);

        // 尝试解析为JSON
        try {
            // 如果是JSON字符串，直接返回
            if (bodyContent.trim().startsWith("{") || bodyContent.trim().startsWith("[")) {
                return RequestBody.create(bodyContent, JSON_MEDIA_TYPE);
            }
            // 否则作为普通表单处理
            return RequestBody.create(bodyContent, MediaType.get("text/plain; charset=utf-8"));
        } catch (Exception e) {
            return RequestBody.create(bodyContent, MediaType.get("text/plain; charset=utf-8"));
        }
    }

    private String replaceVariables(String template, Map<String, Object> inputs, FlowContext context) {
        if (template == null) {
            return null;
        }


        String result = template;

        // 替换 {varName}} 格式的变量
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(template);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = resolveVariable(varName, inputs, context);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private Object resolveVariable(String varName, Map<String, Object> inputs, FlowContext context) {
        // 尝试从inputs获取
        if (inputs.containsKey(varName)) {
            return inputs.get(varName);
        }

        // 尝试从context获取
        Object ctxVal = context.getVariable(varName);
        if (ctxVal != null) {
            return ctxVal;
        }

        // 尝试嵌套路径 (e.g., "response.data")
        if (varName.contains(".")) {
            String[] parts = varName.split("\\.", 2);
            Object val = inputs.containsKey(parts[0]) ? inputs.get(parts[0]) : context.getVariable(parts[0]);
            if (val instanceof Map) {
                return ((Map<?, ?>) val).get(parts[1]);
            }
        }

        return null;
    }

    private Object processResponse(Response response, FlowContext context) throws IOException {
        int statusCode = response.code();
        String responseBody = response.body() != null ? response.body().string() : "";

        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", statusCode);
        result.put("success", statusCode >= 200 && statusCode < 300);
        result.put("body", responseBody);

        // 解析响应头
        Map<String, String> respHeaders = new HashMap<>();
        Headers headers = response.headers();
        for (String name : headers.names()) {
            respHeaders.put(name, headers.get(name));
        }
        result.put("headers", respHeaders);

        // 尝试解析JSON响应体
        if (responseBody != null && !responseBody.isEmpty()) {
            try {
                Object jsonData = JsonUtil.toPlainValue(JsonUtil.parse(responseBody));
                result.put("data", jsonData);
            } catch (Exception e) {
                // 非JSON响应，保持原body字符串
                log.debug("HttpNode[{}] response is not JSON, keeping as string", nodeId);
            }
        }

        // 应用响应映射
        if (!responseMapping.isEmpty()) {
            Map<String, Object> mappedResult = new HashMap<>();
            for (Map.Entry<String, String> entry : responseMapping.entrySet()) {
                String path = entry.getKey();
                String varName = entry.getValue();
                Object value = extractByPath(result, path);
                mappedResult.put(varName, value);
            }
            result.put("_mapped", mappedResult);
        }

        // 检查HTTP错误状态
        if (statusCode >= 400) {
            log.warn("HttpNode[{}] received error status: {} - {}", nodeId, statusCode, responseBody);
        }

        return result;
    }

    private Object extractByPath(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
            return data;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    // ==================== 配置方法 ====================

    public HttpNode withHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public HttpNode withHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public HttpNode withQueryParam(String name, String value) {
        this.queryParams.put(name, value);
        return this;
    }

    public HttpNode withQueryParams(Map<String, String> queryParams) {
        this.queryParams.putAll(queryParams);
        return this;
    }

    public HttpNode withBodyTemplate(String template) {
        this.bodyTemplate = template;
        return this;
    }

    public HttpNode withResponseMapping(String jsonPath, String variableName) {
        this.responseMapping.put(jsonPath, variableName);
        return this;
    }

    public HttpNode withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        initDefaults(); // 重新初始化client
        return this;
    }

    public HttpNode withFollowRedirects(boolean follow) {
        this.followRedirects = follow;
        initDefaults();
        return this;
    }

    public HttpNode withHttpClient(OkHttpClient client) {
        this.httpClient = client;
        return this;
    }

    public HttpNode withConfig(String key, Object value) {
        this.config.put(key, value);
        return this;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> fullConfig = new HashMap<>(this.config);
        fullConfig.put("url", url);
        fullConfig.put("method", method);
        fullConfig.put("headers", new HashMap<>(headers));
        fullConfig.put("queryParams", new LinkedHashMap<>(queryParams));
        fullConfig.put("bodyTemplate", bodyTemplate);
        fullConfig.put("responseMapping", new HashMap<>(responseMapping));
        fullConfig.put("timeoutMs", timeoutMs);
        fullConfig.put("followRedirects", followRedirects);
        return fullConfig;
    }

    @Override
    public boolean validate() {
        if (url == null || url.trim().isEmpty()) {
            log.error("HttpNode[{}] URL cannot be empty", nodeId);
            return false;
        }
        if (!Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS").contains(method)) {
            log.error("HttpNode[{}] invalid HTTP method: {}", nodeId, method);
            return false;
        }
        return true;
    }

    // Getters
    public String getNodeId() {
        return nodeId;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    public Map<String, String> getQueryParams() {
        return new LinkedHashMap<>(queryParams);
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public Map<String, String> getResponseMapping() {
        return new HashMap<>(responseMapping);
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}