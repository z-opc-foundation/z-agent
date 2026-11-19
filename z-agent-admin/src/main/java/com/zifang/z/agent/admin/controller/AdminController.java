package com.zifang.z.agent.admin.controller;

import com.zifang.z.agent.admin.service.AdminQueryService;
import com.zifang.z.meta.api.annotation.BizApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * z-agent-admin 控制面 REST.
 */
@RestController
@RequestMapping("/z-agent/admin")
public class AdminController {

    private final AdminQueryService service;

    public AdminController(AdminQueryService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @BizApi(summary = "overview", description = "", tags = { "z-agent" })
    public Map<String, Object> overview() {
        return service.overview();
    }

    @GetMapping("/agents")
    @BizApi(summary = "listAgents", description = "", tags = { "z-agent" })
    public List<String> listAgents() {
        return service.agentNames();
    }

    @GetMapping("/models")
    @BizApi(summary = "listModels", description = "", tags = { "z-agent" })
    public List<String> listModels() {
        return service.supportedModels();
    }
}

