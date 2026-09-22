package com.zifang.z.agent.admin.controller;

import com.zifang.z.agent.admin.service.AdminQueryService;
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
    public Map<String, Object> overview() {
        return service.overview();
    }

    @GetMapping("/agents")
    public List<String> listAgents() {
        return service.agentNames();
    }

    @GetMapping("/models")
    public List<String> listModels() {
        return service.supportedModels();
    }
}