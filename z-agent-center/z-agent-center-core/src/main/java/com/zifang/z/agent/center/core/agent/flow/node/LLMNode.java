package com.zifang.z.agent.center.core.agent.flow.node;

import com.zifang.z.agent.center.core.agent.flow.FlowContext;
import com.zifang.z.agent.center.core.agent.flow.FlowNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * LLM节点 - 直接调用LLM进行文本生成
 * 支持配置系统提示、用户模板、生成参数
 */
public class LLMNode implements FlowNode {

    private static final Logger log = LogManager.getLogger(LLMNode.class);

    private final String nodeId;
    private final String modelName;
    private final Map<String, Object> config = new HashMap<>();

    /**
     * 系统提示
     */
    private String systemPrompt;

    /**
     * 用户消息模板
     */
    private String userPromptTemplate;

    /**
     * 生成参数
     */
    private int maxTokens = 2000;
    private double temperature = 0.7;
    private double topP = 0.9;
    private String[] stopSequences;

    /**
     * LLM调用器（外部注入）
     */
    private LLMCaller llmCaller;

    /**
     * 输入映射
     */
    private Map<String, String> inputMapping = new HashMap<>();

    /**
     * 输出变量名
     */
    private String outputVarName = "_llm_output";

    /**
     * 是否流式输出
     */
    private boolean stream = false;

    public LLMNode(String nodeId, String modelName) {
        this.nodeId = nodeId;
        this.modelName = modelName;
    }

    @Override
    public String getType() {
        return TYPE_LLM;
    }

    @Override
    public Object execute(Map<String, Object> inputs, FlowContext context) {
        log.info("LLMNode[{}] invoking LLM model: {}", nodeId, modelName);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 构建消息
            List<Message> messages = buildMessages(inputs, context);

            // 2. 构建生成参数
            GenerationConfig genConfig = buildGenerationConfig();

            // 3. 调用LLM
            String response;
            if (stream) {
                response = invokeStreaming(messages, genConfig, context);
            } else {
                response = invokeSync(messages, genConfig);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("LLMNode[{}] completed in {}ms, response length: {}", nodeId, elapsed, response != null ? response.length() : 0);

            // 4. 处理输出
            Object output = processOutput(response, context);

            return output;

        } catch (Exception e) {
            log.error("LLMNode[{}] LLM call failed: {}", nodeId, e.getMessage(), e);
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    private List<Message> buildMessages(Map<String, Object> inputs, FlowContext context) {
        List<Message> messages = new ArrayList<>();

        // 添加系统消息
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            String resolvedPrompt = resolveTemplate(systemPrompt, inputs, context);
            messages.add(new Message("system", resolvedPrompt));
        }

        // 添加历史消息（继承父上下文）
        for (FlowContext.ContextMessage msg : context.getMessages()) {
            if (msg.getNodeId() == null || !msg.getNodeId().equals(nodeId)) {
                messages.add(new Message(msg.getRole(), msg.getContent()));
            }
        }

        // 添加当前用户消息
        if (userPromptTemplate != null && !userPromptTemplate.isEmpty()) {
            String userMessage = resolveTemplate(userPromptTemplate, inputs, context);
            messages.add(new Message("user", userMessage));
        } else {
            // 从inputs构建默认用户消息
            String userMessage = buildInputMessage(inputs, context);
            if (!userMessage.isEmpty()) {
                messages.add(new Message("user", userMessage));
            }
        }

        return messages;
    }

    private String resolveTemplate(String template, Map<String, Object> inputs, FlowContext context) {
        if (template == null) {
            return "";
        }


        String result = template;

        // 替换 {varName}} 格式的变量
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(template);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = resolveVariable(varName, inputs, context);
            String replacement = value != null ? escapeForReplacement(String.valueOf(value)) : "";
            matcher.appendReplacement(sb, replacement);
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

        // 尝试嵌套路径 (e.g., "user.name")
        if (varName.contains(".")) {
            String[] parts = varName.split("\\.", 2);
            Object val = inputs.containsKey(parts[0]) ? inputs.get(parts[0]) : context.getVariable(parts[0]);
            if (val instanceof Map) {
                return ((Map<?, ?>) val).get(parts[1]);
            }
        }

        return null;
    }

    private String escapeForReplacement(String value) {
        return value.replace("\\", "\\\\")
                .replace("$", "\\$")
                .replace("{", "\\{")
                .replace("}", "\\}");
    }

    private String buildInputMessage(Map<String, Object> inputs, FlowContext context) {
        if (inputs == null || inputs.isEmpty()) {
            Object userMsg = context.getVariable("_user_message");
            return userMsg != null ? userMsg.toString() : "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private GenerationConfig buildGenerationConfig() {
        GenerationConfig config = new GenerationConfig();
        config.setModelName(modelName);
        config.setMaxTokens(maxTokens);
        config.setTemperature(temperature);
        config.setTopP(topP);
        config.setStopSequences(stopSequences);
        return config;
    }

    private String invokeSync(List<Message> messages, GenerationConfig genConfig) {
        if (llmCaller != null) {
            return llmCaller.call(messages, genConfig);
        }

        // 默认实现 - 可以通过配置或扩展点注入真实的LLM调用
        log.warn("LLMNode[{}] no LLM caller configured, returning mock response", nodeId);
        return mockLLMResponse(messages);
    }

    private String invokeStreaming(List<Message> messages, GenerationConfig genConfig, FlowContext context) {
        if (llmCaller != null && llmCaller instanceof StreamingLLMCaller) {
            StringBuilder fullResponse = new StringBuilder();
            ((StreamingLLMCaller) llmCaller).callStream(messages, genConfig, token -> {
                fullResponse.append(token);
                // 可以在这里实时输出到context或其他地方
            });
            return fullResponse.toString();
        }

        // 非流式fallback
        return invokeSync(messages, genConfig);
    }

    private String mockLLMResponse(List<Message> messages) {
        // 简单的模拟响应，用于测试
        StringBuilder sb = new StringBuilder("Mock LLM response to: ");
        for (Message msg : messages) {
            if ("user".equals(msg.getRole())) {
                String content = msg.getContent();
                sb.append(content.length() > 50 ? content.substring(0, 50) + "..." : content);
                break;
            }
        }
        return sb.toString();
    }

    private Object processOutput(String response, FlowContext context) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", response);
        result.put("model", modelName);

        // 存储到context变量
        if (outputVarName != null) {
            context.setVariable(outputVarName, response);
        }

        // 添加到消息历史
        context.addMessage("assistant", response, nodeId);

        // 缓存节点输出
        context.setNodeOutput(nodeId, result);

        return result;
    }

    // ==================== 配置方法 ====================

    /**
     * 设置系统提示
     */
    public LLMNode withSystemPrompt(String prompt) {
        this.systemPrompt = prompt;
        return this;
    }

    /**
     * 设置用户消息模板
     */
    public LLMNode withUserPromptTemplate(String template) {
        this.userPromptTemplate = template;
        return this;
    }

    /**
     * 设置生成参数
     */
    public LLMNode withMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public LLMNode withTemperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    public LLMNode withTopP(double topP) {
        this.topP = topP;
        return this;
    }

    public LLMNode withStopSequences(String... stopSequences) {
        this.stopSequences = stopSequences;
        return this;
    }

    /**
     * 设置LLM调用器
     */
    public LLMNode withLLMCaller(LLMCaller caller) {
        this.llmCaller = caller;
        return this;
    }

    /**
     * 设置输入映射
     */
    public LLMNode withInputMapping(String flowVar, String promptVar) {
        this.inputMapping.put(flowVar, promptVar);
        return this;
    }

    /**
     * 设置输出变量名
     */
    public LLMNode withOutputVarName(String varName) {
        this.outputVarName = varName;
        return this;
    }

    /**
     * 启用流式输出
     */
    public LLMNode withStream(boolean stream) {
        this.stream = stream;
        return this;
    }

    public LLMNode withConfig(String key, Object value) {
        this.config.put(key, value);
        return this;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> fullConfig = new HashMap<>(this.config);
        fullConfig.put("modelName", modelName);
        fullConfig.put("systemPrompt", systemPrompt);
        fullConfig.put("userPromptTemplate", userPromptTemplate);
        fullConfig.put("maxTokens", maxTokens);
        fullConfig.put("temperature", temperature);
        fullConfig.put("topP", topP);
        fullConfig.put("stopSequences", stopSequences != null ? Arrays.asList(stopSequences) : Collections.emptyList());
        fullConfig.put("inputMapping", new HashMap<>(inputMapping));
        fullConfig.put("outputVarName", outputVarName);
        fullConfig.put("stream", stream);
        return fullConfig;
    }

    @Override
    public boolean validate() {
        if (modelName == null || modelName.trim().isEmpty()) {
            log.error("LLMNode[{}] model name cannot be empty", nodeId);
            return false;
        }
        if (temperature < 0 || temperature > 2) {
            log.error("LLMNode[{}] temperature must be between 0 and 2", nodeId);
            return false;
        }
        if (maxTokens <= 0) {
            log.error("LLMNode[{}] maxTokens must be positive", nodeId);
            return false;
        }
        return true;
    }

    // Getters
    public String getNodeId() {
        return nodeId;
    }

    public String getModelName() {
        return modelName;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPromptTemplate() {
        return userPromptTemplate;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getTopP() {
        return topP;
    }

    public String[] getStopSequences() {
        return stopSequences;
    }

    public String getOutputVarName() {
        return outputVarName;
    }

    public boolean isStream() {
        return stream;
    }

    // ==================== 内部类/接口 ====================

    /**
     * LLM调用器接口
     */
    public interface LLMCaller {
        String call(List<Message> messages, GenerationConfig config);
    }

    /**
     * 流式LLM调用器接口
     */
    public interface StreamingLLMCaller extends LLMCaller {
        void callStream(List<Message> messages, GenerationConfig config, StreamCallback callback);
    }

    /**
     * 流式回调
     */
    public interface StreamCallback {
        void onToken(String token);
    }

    /**
     * 消息
     */
    public static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }

    /**
     * 生成配置
     */
    public static class GenerationConfig {
        private String modelName;
        private int maxTokens = 2000;
        private double temperature = 0.7;
        private double topP = 0.9;
        private String[] stopSequences;

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public double getTopP() {
            return topP;
        }

        public void setTopP(double topP) {
            this.topP = topP;
        }

        public String[] getStopSequences() {
            return stopSequences;
        }

        public void setStopSequences(String[] stopSequences) {
            this.stopSequences = stopSequences;
        }
    }
}