package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.engine.agent.AgentEngine;
import com.zifang.z.agent.engine.agent.llm.model.define.*;
import com.zifang.z.agent.engine.agent.llm.provider.LlmCallerConfig;
import com.zifang.z.agent.engine.agent.llm.provider.OllamaModel;
import com.zifang.z.agent.engine.agent.llm.tool.core.ToolSchema;
import com.zifang.z.agent.engine.agent.skill.Skill;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存中的 Agent 工厂 - 直接在 JVM 中创建 AgentEngine
 * 不依赖外部服务，不走 HTTP
 * <p>
 * 历史: 之前用 LocalModelClient (demo 包, 已移到 test), 现改用引擎正式 OllamaModel
 */
@Component
public class InMemoryAgentFactory {

    private static final Logger log = LogManager.getLogger(InMemoryAgentFactory.class);

    private static final String DEFAULT_MODEL = "qwen:7b";

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
        // 引擎正式 OllamaModel (替代之前的 demo LocalModelClient)
        OllamaModel ollama = new OllamaModel();
        ollama.init(LlmCallerConfig.builder().modelName(DEFAULT_MODEL).build());

        // Model 装饰器
        Model model = new OllamaModelAdapter(ollama);

        return new AgentEngine(skills, ollama, model);
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

    /**
     * OllamaModel 适配器 (将 OllamaModel 适配为 Model 接口给 AgentEngine 用)
     */
    private static class OllamaModelAdapter implements Model {

        private final OllamaModel ollama;

        public OllamaModelAdapter(OllamaModel ollama) {
            this.ollama = ollama;
        }

        @Override
        public String chat(List<ModelMessage> modelMessages) {
            return ollama.generate(modelMessages);
        }

        @Override
        public String generate(List<ModelMessage> modelMessages) {
            return ollama.generate(modelMessages);
        }

        @Override
        public String identity() {
            return "OllamaModelAdapter(" + ollama.identity() + ")";
        }

        @Override
        public Flux<ModelResponse> stream(ModelRequest modelRequest) {
            return Flux.empty();
        }

        @Override
        public Flux<ModelResponse> stream(List<ModelMessage> modelMessages, List<ToolSchema> tools, ModelOptions options) {
            return Flux.empty();
        }

        @Override
        public List<ModelMessage> adapter() {
            return new ArrayList<ModelMessage>();
        }
    }
}
