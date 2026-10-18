package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.center.core.agent.llm.KernelOllamaModel;
import com.zifang.z.agent.engine.agent.AgentEngine;
import com.zifang.z.agent.engine.agent.llm.model.define.Model;
import com.zifang.z.agent.engine.agent.skill.Skill;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存中的 Agent 工厂 - 直接在 JVM 中创建 AgentEngine
 * 不依赖外部服务，不走 HTTP
 * <p>
 * LLM 调用走 kernel 的 OllamaProvider（见 {@link KernelOllamaModel}），
 * 不再走 engine 自带的 OllamaModel。
 */
@Component
public class InMemoryAgentFactory {

    private static final Logger log = LogManager.getLogger(InMemoryAgentFactory.class);

    private static final String DEFAULT_MODEL = "qwen:7b";

    /** kernel 的 OpenAIProvider 把 baseUrl 当 API 前缀，Ollama 侧必须带 /v1 后缀。 */
    private static final String OLLAMA_BASE_URL = "http://localhost:11434/v1";

    // AgentEngine 实例缓存 (appCode -> AgentEngine)
    private final Map<String, AgentEngine> engineCache = new ConcurrentHashMap<String, AgentEngine>();

    // 默认 Skills (可配置)
    private List<Skill> defaultSkills;

    @PostConstruct
    public void init() {
        this.defaultSkills = buildDefaultSkills();
        log.info("InMemoryAgentFactory initialized with {} default skills", defaultSkills.size());
    }

    public synchronized AgentEngine getEngine(String appCode) {
        if (!engineCache.containsKey(appCode)) {
            AgentEngine engine = createEngine(appCode, defaultSkills);
            engineCache.put(appCode, engine);
            log.info("Created new AgentEngine for appCode: {}", appCode);
        }
        return engineCache.get(appCode);
    }

    public synchronized AgentEngine getEngine(String appCode, List<Skill> skills) {
        if (!engineCache.containsKey(appCode)) {
            AgentEngine engine = createEngine(appCode, skills);
            engineCache.put(appCode, engine);
            log.info("Created new AgentEngine for appCode: {} with {} skills", appCode, skills.size());
        }
        return engineCache.get(appCode);
    }

    private AgentEngine createEngine(String appCode, List<Skill> skills) {
        Model model = new KernelOllamaModel(OLLAMA_BASE_URL, DEFAULT_MODEL);
        return new AgentEngine(skills, model);
    }

    private List<Skill> buildDefaultSkills() {
        List<Skill> skills = new ArrayList<Skill>();
        Skill writeFile = new Skill();
        writeFile.setName("write_file");
        writeFile.setDescription("写入内容到指定文件路径");
        writeFile.setComposite(false);
        skills.add(writeFile);
        Skill runCommand = new Skill();
        runCommand.setName("run_command");
        runCommand.setDescription("执行系统命令");
        runCommand.setComposite(false);
        skills.add(runCommand);
        Skill search = new Skill();
        search.setName("search");
        search.setDescription("搜索信息并返回结果");
        search.setComposite(true);
        skills.add(search);
        return skills;
    }

    public void removeEngine(String appCode) {
        engineCache.remove(appCode);
        log.info("Removed AgentEngine for appCode: {}", appCode);
    }

    public void clearAll() {
        engineCache.clear();
        log.info("Cleared all AgentEngine instances");
    }

    public int getEngineCount() {
        return engineCache.size();
    }
}
