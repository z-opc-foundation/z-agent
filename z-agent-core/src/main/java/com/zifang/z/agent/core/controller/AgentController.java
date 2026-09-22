package com.zifang.z.agent.core.controller;

import com.zifang.z.agent.api.dto.AgentDto;
import com.zifang.z.agent.api.dto.RunRequest;
import com.zifang.z.agent.api.dto.RunResponse;
import com.zifang.z.agent.api.exception.AgentException;
import com.zifang.z.agent.core.runner.AgentRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z-agent 平台 REST 网关 — 主入口.
 *
 * <ul>
 *   <li>GET  /agent/list — 列出注册 agent 元信息</li>
 *   <li>GET  /agent/{name} — 取单个 agent 元信息</li>
 *   <li>POST /agent/{name}/run — 同步运行</li>
 *   <li>POST /agent/{name}/stream — SSE 流式</li>
 * </ul>
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentRunner runner;

    public AgentController(AgentRunner runner) {
        this.runner = runner;
    }

    @GetMapping("/list")
    public List<AgentDto> list() {
        return runner.listAgents();
    }

    @GetMapping("/{name}")
    public AgentDto describe(@PathVariable("name") String name) {
        return runner.describeAgent(name);
    }

    @PostMapping("/{name}/run")
    public RunResponse run(@PathVariable("name") String name,
                           @RequestBody(required = false) Map<String, Object> body) {
        RunRequest req = parseRequest(name, body);
        return runner.run(req);
    }

    @PostMapping(value = "/{name}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> stream(@PathVariable("name") String name,
                                    @RequestBody(required = false) Map<String, Object> body) {
        RunRequest req = parseRequest(name, body);
        return ResponseEntity.ok().body(new java.util.HashMap<String, Object>() {{
            put("status", "sse-ready");
            put("agent", name);
            put("note", "SSE streaming endpoint placeholder; production wires runner.stream() to SseEmitter");
        }});
    }

    private RunRequest parseRequest(String name, Map<String, Object> body) {
        if (body == null) body = Collections.emptyMap();
        String input = body.get("input") == null ? "" : body.get("input").toString();
        Object histObj = body.get("history");
        List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();
        if (histObj instanceof List) {
            for (Object h : (List<?>) histObj) {
                if (h instanceof Map) history.add((Map<String, Object>) h);
            }
        }
        Object toolsObj = body.get("toolNames");
        List<String> toolNames = new ArrayList<String>();
        if (toolsObj instanceof List) {
            for (Object t : (List<?>) toolsObj) {
                if (t != null) toolNames.add(t.toString());
            }
        }
        Object cfgObj = body.get("config");
        Map<String, Object> cfg = new HashMap<String, Object>();
        if (cfgObj instanceof Map) cfg.putAll((Map<String, Object>) cfgObj);
        return RunRequest.builder()
                .agentName(name)
                .input(input)
                .history(history)
                .toolNames(toolNames)
                .config(cfg)
                .build();
    }
}