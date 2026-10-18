package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Resource;

/**
 * 内存模式 Agent 执行器 (ReAct 循环)
 * <p>
 * IN_PROCESS 模式:
 * - ReAct 循环在内存中执行
 * - 不依赖外部服务
 * - 通过 Ollama LLM 进行 Thought/Action/Observation 循环
 */
public class InProcessAgentExecutor implements AgentExecutor {

    private static final Logger log = LogManager.getLogger(InProcessAgentExecutor.class);

    private final ReActAgentFactory reActAgentFactory;

    @Resource
    private AgentInstanceService agentInstanceService;

    public InProcessAgentExecutor() {
        this.reActAgentFactory = new ReActAgentFactory();
    }

    public InProcessAgentExecutor(ReActAgentFactory reActAgentFactory) {
        this.reActAgentFactory = reActAgentFactory;
    }

    @Override
    public String execute(String instanceCode, String userId, String userName, String message) {
        // 1. 获取实例信息
        AgentInstance instance = agentInstanceService.getByInstanceCode(instanceCode);
        if (instance == null) {
            throw new RuntimeException("Agent instance not found: " + instanceCode);
        }

        // 2. 检查运行模式
        if (!"IN_PROCESS".equals(instance.getRunMode())) {
            throw new RuntimeException("Instance is not in IN_PROCESS mode: " + instanceCode);
        }

        try {
            // 3. 获取 ReAct Agent
            ReActAgentFactory.ReActAgent agent = reActAgentFactory.getAgent(instanceCode);

            // 4. 执行 ReAct 循环
            String result = agent.execute(message);

            log.info("ReAct execution completed for instance: {}", instanceCode);
            return result;

        } catch (Exception e) {
            log.error("Failed to execute ReAct agent: {}", e.getMessage(), e);
            throw new RuntimeException("ReAct execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getType() {
        return "IN_PROCESS";
    }

    @Override
    public boolean isHealthy() {
        // 检查 Ollama 是否可用
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL("http://localhost:11434").openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            log.warn("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }
}