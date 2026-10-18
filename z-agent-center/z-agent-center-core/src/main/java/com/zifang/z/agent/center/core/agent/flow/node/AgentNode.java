package com.zifang.z.agent.center.core.agent.flow.node;

import com.zifang.z.agent.center.core.agent.executor.AgentExecutor;
import com.zifang.z.agent.center.core.agent.flow.FlowContext;
import com.zifang.z.agent.center.core.agent.flow.FlowNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent节点 - 在流程中调用其他Agent
 * 支持上下文传递和嵌套执行
 */
public class AgentNode implements FlowNode {

    private static final Logger log = LogManager.getLogger(AgentNode.class);

    private final String nodeId;
    private final String agentInstanceCode;
    private final String runMode;  // IN_PROCESS / CONTAINER
    private final AgentExecutor agentExecutor;
    private final Map<String, Object> config = new HashMap<>();

    /**
     * 输入映射: flowVariable -> agentInput
     */
    private Map<String, String> inputMapping = new HashMap<>();

    /**
     * 输出映射: agentOutput -> flowVariable
     */
    private Map<String, String> outputMapping = new HashMap<>();

    /**
     * 超时时间(ms)
     */
    private long timeoutMs = 5 * 60 * 1000;

    /**
     * 是否继承父上下文消息
     */
    private boolean inheritMessages = true;

    public AgentNode(String nodeId, String agentInstanceCode, String runMode, AgentExecutor agentExecutor) {
        this.nodeId = nodeId;
        this.agentInstanceCode = agentInstanceCode;
        this.runMode = runMode;
        this.agentExecutor = agentExecutor;
    }

    @Override
    public String getType() {
        return TYPE_AGENT;
    }

    @Override
    public Object execute(Map<String, Object> inputs, FlowContext context) {
        long startTime = System.currentTimeMillis();
        log.info("AgentNode[{}] executing agent: {} (mode: {})", nodeId, agentInstanceCode, runMode);

        try {
            // 1. 构建传递给Agent的消息
            String userMessage = buildInputMessage(inputs, context);

            // 2. 创建子上下文（隔离子Agent的变量和消息）
            FlowContext subContext = context.createSubContext(context.getRootFlowId(), nodeId);
            subContext.setVariable("_node_input", inputs);

            // 3. 继承父上下文消息（如果配置了）
            if (inheritMessages && !context.getMessages().isEmpty()) {
                for (FlowContext.ContextMessage msg : context.getMessages()) {
                    subContext.addMessage(msg.getRole(), msg.getContent());
                }
            }

            // 4. 添加系统提示（节点配置）
            String systemPrompt = buildSystemPrompt();
            if (systemPrompt != null) {
                subContext.addMessage("system", systemPrompt);
            }

            // 5. 执行Agent
            Object agentResult;
            if ("IN_PROCESS".equals(runMode)) {
                agentResult = executeInProcess(userMessage, subContext);
            } else if ("CONTAINER".equals(runMode)) {
                agentResult = executeViaContainer(userMessage, subContext);
            } else {
                throw new IllegalArgumentException("Unsupported run mode: " + runMode);
            }

            // 6. 处理输出映射
            Object output = processOutput(agentResult, context);

            // 7. 合并子上下文（保留关键变量和消息）
            context.mergeSubContext(subContext);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("AgentNode[{}] completed in {}ms", nodeId, elapsed);

            // 8. 缓存输出供后续节点使用
            context.setNodeOutput(nodeId, output);

            return output;

        } catch (Exception e) {
            log.error("AgentNode[{}] execution failed", nodeId, e);
            throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * IN_PROCESS模式: 调用ReAct Agent
     */
    private Object executeInProcess(String userMessage, FlowContext subContext) {
        // 通过AgentExecutor执行
        if (agentExecutor instanceof com.zifang.z.agent.center.core.agent.executor.InProcessAgentExecutor) {
            com.zifang.z.agent.center.core.agent.executor.InProcessAgentExecutor executor =
                    (com.zifang.z.agent.center.core.agent.executor.InProcessAgentExecutor) agentExecutor;
            return executor.executeWithContext(agentInstanceCode, userMessage, subContext);
        }

        // 降级为直接调用
        return agentExecutor.execute(agentInstanceCode, userMessage, subContext.getUserId(), subContext.getUserName());
    }

    /**
     * CONTAINER模式: 调用远程Hermes容器
     */
    private Object executeViaContainer(String userMessage, FlowContext subContext) {
        if (agentExecutor instanceof com.zifang.z.agent.center.core.agent.executor.HermesProxyExecutor) {
            com.zifang.z.agent.center.core.agent.executor.HermesProxyExecutor executor =
                    (com.zifang.z.agent.center.core.agent.executor.HermesProxyExecutor) agentExecutor;
            return executor.executeWithContext(agentInstanceCode, userMessage, subContext);
        }

        return agentExecutor.execute(agentInstanceCode, userMessage, subContext.getUserId(), subContext.getUserName());
    }

    /**
     * 构建输入消息
     */
    private String buildInputMessage(Map<String, Object> inputs, FlowContext context) {
        if (inputs == null || inputs.isEmpty()) {
            Object userMsg = context.getVariable("_user_message");
            return userMsg != null ? userMsg.toString() : "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            sb.append(key).append(": ").append(value).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt() {
        String prompt = (String) config.get("systemPrompt");
        if (prompt != null) {
            return prompt;
        }


        String template = (String) config.get("systemPromptTemplate");
        if (template != null) {
            // 简单模板替换
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                template = template.replace("{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
            return template;
        }

        return null;
    }

    /**
     * 处理输出映射
     */
    @SuppressWarnings("unchecked")
    private Object processOutput(Object agentResult, FlowContext context) {
        if (outputMapping.isEmpty()) {
            return agentResult;
        }

        Map<String, Object> result = new HashMap<>();

        if (agentResult instanceof Map) {
            Map<String, Object> resultMap = (Map<String, Object>) agentResult;
            for (Map.Entry<String, String> entry : outputMapping.entrySet()) {
                String agentKey = entry.getKey();
                String flowVar = entry.getValue();
                result.put(flowVar, resultMap.get(agentKey));
            }
        } else {
            // 非Map类型，直接返回
            result.put("_raw_output", agentResult);
        }

        return result;
    }

    // ==================== 配置方法 ====================

    public AgentNode withInputMapping(String flowVar, String agentParam) {
        this.inputMapping.put(flowVar, agentParam);
        return this;
    }

    public AgentNode withOutputMapping(String agentOutput, String flowVar) {
        this.outputMapping.put(agentOutput, flowVar);
        return this;
    }

    public AgentNode withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public AgentNode withInheritMessages(boolean inherit) {
        this.inheritMessages = inherit;
        return this;
    }

    public AgentNode withConfig(String key, Object value) {
        this.config.put(key, value);
        return this;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> fullConfig = new HashMap<>(this.config);
        fullConfig.put("agentInstanceCode", agentInstanceCode);
        fullConfig.put("runMode", runMode);
        fullConfig.put("timeoutMs", timeoutMs);
        fullConfig.put("inheritMessages", inheritMessages);
        fullConfig.put("inputMapping", inputMapping);
        fullConfig.put("outputMapping", outputMapping);
        return fullConfig;
    }

    // Getters
    public String getNodeId() {
        return nodeId;
    }

    public String getAgentInstanceCode() {
        return agentInstanceCode;
    }

    public String getRunMode() {
        return runMode;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isInheritMessages() {
        return inheritMessages;
    }

    public Map<String, String> getInputMapping() {
        return inputMapping;
    }

    public Map<String, String> getOutputMapping() {
        return outputMapping;
    }
}