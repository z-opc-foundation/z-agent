package com.zifang.z.agent.center.core.agent.tool.lc;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.agent.tool.ToolRegistry;
import com.zifang.z.agent.engine.agent.llm.tool.core.NanoBotTool;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * z-lc 物化 + schema 管理 Agent 工具 (FEATURE006 T2).
 * <p>
 * 提供 6 个工具给 z-agent ToolRegistry:
 * <ul>
 *   <li>lc_list_entities       — 列出指定 app 的 entity</li>
 *   <li>lc_get_entity_fields   — 获取 entity 字段定义</li>
 *   <li>lc_create_entity       — 创建 entity (含字段)</li>
 *   <li>lc_update_entity       — 更新 entity</li>
 *   <li>lc_delete_entity       — 删除 entity</li>
 *   <li>lc_trigger_materialize — 触发代码物化 (异步)</li>
 * </ul>
 * <p>
 * 解耦设计: 工具通过 OkHttp 调用 z-lc 的 REST 端点, 不依赖 z-lc 内部类.
 */
@Component
public class LcMaterializationTool implements NanoBotTool {

    private static final Logger log = LogManager.getLogger(LcMaterializationTool.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    @Value("${z-lc.tool.base-url:http://localhost:8888}")
    private String baseUrl;

    @PostConstruct
    public void registerToToolRegistry() {
        ToolRegistry registry = ToolRegistry.getInstance();
        String[] names = {
                "lc_list_entities", "lc_get_entity_fields", "lc_create_entity",
                "lc_update_entity", "lc_delete_entity", "lc_trigger_materialize"};
        for (String name : names) {
            NanoBotTool proxy = new NanoBotTool() {
                @Override
                public String getToolName() {
                    return name;
                }

                @Override
                public String getToolDescription() {
                    return toolDescription(name);
                }

                @Override
                public String execute(Map<String, Object> params) {
                    return dispatch(name, params);
                }
            };
            registry.registerEngineTool(proxy);
        }
        log.info("LcMaterializationTool registered {} tools to ToolRegistry (baseUrl={})", names.length, baseUrl);
    }

    private String toolDescription(String name) {
        switch (name) {
            case "lc_list_entities":
                return "List all entities in a z-lc app. Params: appCode (string, required).";
            case "lc_get_entity_fields":
                return "Get entity field definitions via /schema. Params: appCode (string, required).";
            case "lc_create_entity":
                return "Create a new entity with field definitions. Params: appCode, entityCode, entityName, tableName, fields (array).";
            case "lc_update_entity":
                return "Update entity schema. Params: appCode, entityCode, fields.";
            case "lc_delete_entity":
                return "Delete entity. Params: appCode, entityCode.";
            case "lc_trigger_materialize":
                return "Trigger code materialization (async). Params: appCode, materializationPath, entityCodes (optional array), description.";
            default:
                return "Unknown tool";
        }
    }

    private String dispatch(String name, Map<String, Object> params) {
        try {
            switch (name) {
                case "lc_list_entities":
                    return doListEntities(params);
                case "lc_get_entity_fields":
                    return doGetEntityFields(params);
                case "lc_create_entity":
                    return doCreateEntity(params);
                case "lc_update_entity":
                    return doUpdateEntity(params);
                case "lc_delete_entity":
                    return doDeleteEntity(params);
                case "lc_trigger_materialize":
                    return doTriggerMaterialize(params);
                default:
                    return "{\"error\":\"unknown tool: " + name + "\"}";
            }
        } catch (Exception ex) {
            log.warn("Tool {} failed: {}", name, ex.getMessage());
            return "{\"error\":\"" + ex.getClass().getSimpleName() + ": " + ex.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String doListEntities(Map<String, Object> p) {
        return doGet(baseUrl + "/api/lc/app/" + require(p, "appCode") + "/schema");
    }

    private String doGetEntityFields(Map<String, Object> p) {
        return doGet(baseUrl + "/api/lc/app/" + require(p, "appCode") + "/schema");
    }

    private String doCreateEntity(Map<String, Object> p) {
        String appCode = require(p, "appCode");
        p.put("source", "AGENT");
        p.put("eventType", "CREATE");
        return doPost(baseUrl + "/api/lc/app/" + appCode + "/event", p);
    }

    private String doUpdateEntity(Map<String, Object> p) {
        String appCode = require(p, "appCode");
        p.put("source", "AGENT");
        p.put("eventType", "UPDATE");
        return doPost(baseUrl + "/api/lc/app/" + appCode + "/event", p);
    }

    private String doDeleteEntity(Map<String, Object> p) {
        String appCode = require(p, "appCode");
        p.put("source", "AGENT");
        p.put("eventType", "DELETE");
        return doPost(baseUrl + "/api/lc/app/" + appCode + "/event", p);
    }

    private String doTriggerMaterialize(Map<String, Object> p) {
        String appCode = require(p, "appCode");
        if (!p.containsKey("materializationPath")) {
            p.put("materializationPath", "agent-gen-" + System.currentTimeMillis());
        }
        if (!p.containsKey("triggerSource")) {
            p.put("triggerSource", "AGENT");
        }

        return doPost(baseUrl + "/api/lc/app/" + appCode + "/materialize", p);
    }

    // ── HTTP helpers (OkHttp) ─────────────────────────────────────────────

    private String doGet(String url) {
        try {
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = http.newCall(req).execute()) {
                ResponseBody body = resp.body();
                return body == null ? "{\"status\":" + resp.code() + "}" : body.string();
            }
        } catch (Exception ex) {
            return "{\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String doPost(String url, Map<String, Object> body) {
        try {
            String jsonStr = JsonUtil.toJsonExcludingNulls(body);
            Request req = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(JSON, jsonStr))
                    .build();
            try (Response resp = http.newCall(req).execute()) {
                ResponseBody respBody = resp.body();
                return respBody == null ? "{\"status\":" + resp.code() + "}" : respBody.string();
            }
        } catch (Exception ex) {
            return "{\"error\":\"" + ex.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String require(Map<String, Object> p, String key) {
        Object v = p == null ? null : p.get(key);
        if (v == null || v.toString().isEmpty()) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return v.toString();
    }

    // ── NanoBotTool: this class is also a meta-dispatcher ──────────────────

    @Override
    public String getToolName() {
        return "lc_dispatch";
    }

    @Override
    public String getToolDescription() {
        return "z-lc 物化 + schema 管理工具集. 通过 op 参数路由到子工具: "
                + "lc_list_entities, lc_get_entity_fields, lc_create_entity, lc_update_entity, "
                + "lc_delete_entity, lc_trigger_materialize";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        Object op = parameters == null ? null : parameters.get("op");
        if (op == null) return "{\"error\":\"missing op\"}";
        return dispatch(op.toString(), parameters);
    }
}
