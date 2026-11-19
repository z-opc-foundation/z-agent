package com.zifang.z.agent.admin.service;

import com.zifang.z.agent.core.agent.LlmAdapter;
import com.zifang.z.agent.core.registry.AgentRegistry;
import com.zifang.z.meta.api.annotation.BizMethod;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * z-agent-admin 控制面只读查询服务.
 */
@Service
public class AdminQueryService {

    private final AgentRegistry registry;
    private final LlmAdapter llm;

    public AdminQueryService(AgentRegistry registry, LlmAdapter llm) {
        this.registry = registry;
        this.llm = llm;
    }

    @BizMethod(name = "overview", module = "z-agent", description = "", operation = BizMethod.Operation.ACTION)
    public Map<String, Object> overview() {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("agentCount", registry.size());
        out.put("agents", registry.list());
        out.put("providerCount", llm.providerCount());
        out.put("models", llm.listModels());
        out.put("status", "ok");
        return out;
    }

    @BizMethod(name = "agentNames", module = "z-agent", description = "", operation = BizMethod.Operation.ACTION)
    public List<String> agentNames() {
        return registry.names();
    }

    @BizMethod(name = "supportedModels", module = "z-agent", description = "", operation = BizMethod.Operation.ACTION)
    public List<String> supportedModels() {
        return llm.listModels();
    }
}

