package com.zifang.z.agent.center.core.agent.executor;

import com.zifang.z.agent.engine.agent.llm.provider.LlmCallerConfig;
import com.zifang.z.agent.engine.agent.llm.provider.OllamaModel;
import com.zifang.z.agent.engine.agent.skill.Skill;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReAct 模式的 Agent 工厂
 * <p>
 * ReAct = Reason + Act
 * 循环: Thought → Action → Observation → Thought → ... → Response
 * <p>
 * 不依赖外部服务，在内存中完成整个 ReAct 执行流程
 */
@Component
public class ReActAgentFactory {

    private static final Logger log = LogManager.getLogger(ReActAgentFactory.class);

    private static final String DEFAULT_MODEL = "qwen:7b";
    private static final int MAX_ITERATIONS = 10;  // 防止无限循环

    // Agent 实例缓存 (instanceCode -> ReActAgent)
    private final Map<String, ReActAgent> agentCache = new ConcurrentHashMap<String, ReActAgent>();

    // 默认 Skills
    private final List<Skill> defaultSkills;

    public ReActAgentFactory() {
        this.defaultSkills = buildDefaultSkills();
    }

    public ReActAgentFactory(List<Skill> skills) {
        this.defaultSkills = skills;
    }

    /**
     * 获取或创建 ReAct Agent
     */
    public synchronized ReActAgent getAgent(String instanceCode) {
        if (!agentCache.containsKey(instanceCode)) {
            ReActAgent agent = new ReActAgent(instanceCode, defaultSkills);
            agentCache.put(instanceCode, agent);
            log.info("Created new ReActAgent for instance: {}", instanceCode);
        }
        return agentCache.get(instanceCode);
    }

    /**
     * 获取或创建 ReAct Agent (使用指定的 skills)
     */
    public synchronized ReActAgent getAgent(String instanceCode, List<Skill> skills) {
        if (!agentCache.containsKey(instanceCode)) {
            ReActAgent agent = new ReActAgent(instanceCode, skills);
            agentCache.put(instanceCode, agent);
            log.info("Created new ReActAgent for instance: {} with {} skills", instanceCode, skills.size());
        }
        return agentCache.get(instanceCode);
    }

    /**
     * 移除 Agent
     */
    public void removeAgent(String instanceCode) {
        agentCache.remove(instanceCode);
        log.info("Removed ReActAgent for instance: {}", instanceCode);
    }

    /**
     * 清理所有 Agent
     */
    public void clearAll() {
        agentCache.clear();
        log.info("Cleared all ReActAgent instances");
    }

    /**
     * 构建默认 Skills
     */
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

        Skill readFile = new Skill();
        readFile.setName("read_file");
        readFile.setDescription("读取指定文件路径的内容");
        readFile.setComposite(false);
        skills.add(readFile);

        Skill search = new Skill();
        search.setName("search");
        search.setDescription("搜索信息并返回结果");
        search.setComposite(true);
        skills.add(search);

        return skills;
    }

    /**
     * ReAct Agent 核心类
     * 实现 ReAct 循环: Thought → Action → Observation → ... → Response
     */
    public static class ReActAgent {

        private final String instanceCode;
        private final List<Skill> skills;
        private final OllamaModel modelClient;
        private final List<String> executionHistory;  // 执行历史

        public ReActAgent(String instanceCode, List<Skill> skills) {
            this.instanceCode = instanceCode;
            this.skills = skills;
            this.modelClient = new OllamaModel();
            this.modelClient.init(LlmCallerConfig.builder().modelName(DEFAULT_MODEL).build());
            this.executionHistory = new ArrayList<String>();
        }

        /**
         * 执行 ReAct 循环
         *
         * @param userMessage 用户输入
         * @return 最终响应
         */
        public String execute(String userMessage) {
            executionHistory.clear();
            String currentInput = userMessage;
            String lastObservation = "";

            for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                log.info("【ReAct 第 {} 轮】", iteration + 1);

                // 1. Thought: 让 LLM 分析情况，决定下一步行动
                String thought = think(currentInput, lastObservation);

                // 2. 检查是否结束
                if (isFinished(thought)) {
                    String response = extractResponse(thought);
                    executionHistory.add("【最终响应】" + response);
                    return response;
                }

                // 3. Action: 解析并执行 Action
                String actionResult = act(thought);

                // 4. Observation: 获取执行结果
                lastObservation = "执行结果: " + actionResult;
                executionHistory.add("【Thought】" + thought);
                executionHistory.add("【Action执行】" + actionResult);
                executionHistory.add("【Observation】" + lastObservation);

                // 5. 更新输入 (让 LLM 看到历史)
                currentInput = buildIterationInput(userMessage);
            }

            return "任务执行达到最大迭代次数(" + MAX_ITERATIONS + ")，请检查输入是否合理";
        }

        /**
         * 获取执行历史 (用于调试/展示)
         */
        public List<String> getExecutionHistory() {
            return new ArrayList<String>(executionHistory);
        }

        /**
         * Step 1: Thought - 让 LLM 分析并决策
         */
        private String think(String currentInput, String lastObservation) {
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是 ReAct Agent，遵循以下循环:\n");
            prompt.append("Thought → Action → Observation → Thought → ... → Response\n\n");

            prompt.append("可用 Skills:\n");
            prompt.append(toJson(skills)).append("\n\n");

            prompt.append("执行规则:\n");
            prompt.append("1. 分析用户意图和当前状态\n");
            prompt.append("2. 选择最合适的 Skill 执行\n");
            prompt.append("3. 如果任务完成，输出: Response: [你的最终回答]\n");
            prompt.append("4. 如果需要继续执行，输出: Action: [skill_name] + 参数\n\n");

            if (!lastObservation.isEmpty()) {
                prompt.append("上次执行结果:\n").append(lastObservation).append("\n\n");
            }

            prompt.append("当前用户输入:\n").append(currentInput).append("\n\n");
            prompt.append("请分析并输出你的 Thought:\n");

            try {
                java.util.List<com.zifang.z.agent.engine.agent.llm.model.define.ModelMessage> msgs =
                        new java.util.ArrayList<com.zifang.z.agent.engine.agent.llm.model.define.ModelMessage>();
                msgs.add(new com.zifang.z.agent.engine.agent.llm.model.define.ModelMessage("user", prompt.toString()));
                return modelClient.generate(msgs);
            } catch (Exception e) {
                log.error("LLM 调用失败", e);
                return "Error: " + e.getMessage();
            }
        }

        /**
         * Step 2: Action - 解析并执行 Skill
         */
        private String act(String thought) {
            // 解析 Action
            String actionLine = extractAction(thought);
            if (actionLine == null) {
                return "无法解析 Action";
            }

            // 解析 skill 名称和参数
            Map<String, Object> params = parseAction(actionLine);
            String skillName = (String) params.get("skill");

            // 执行 Skill
            return executeSkill(skillName, params);
        }

        /**
         * 执行单个 Skill
         */
        private String executeSkill(String skillName, Map<String, Object> params) {
            try {
                switch (skillName) {
                    case "write_file":
                        return executeWriteFile(params);
                    case "run_command":
                        return executeRunCommand(params);
                    case "read_file":
                        return executeReadFile(params);
                    default:
                        return "不支持的 Skill: " + skillName;
                }
            } catch (Exception e) {
                log.error("Skill 执行失败: {}", skillName, e);
                return "执行失败: " + e.getMessage();
            }
        }

        private String executeWriteFile(Map<String, Object> params) {
            String path = (String) params.get("path");
            String content = (String) params.get("content");
            try {
                java.nio.file.Files.write(
                        java.nio.file.Paths.get(path),
                        content.getBytes("UTF-8")
                );
                return "文件写入成功: " + path;
            } catch (Exception e) {
                return "文件写入失败: " + e.getMessage();
            }
        }

        private String executeRunCommand(Map<String, Object> params) {
            String command = (String) params.get("command");
            try {
                ProcessBuilder pb = new ProcessBuilder(command.split(" "));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                process.waitFor();
                return output.toString();
            } catch (Exception e) {
                return "命令执行失败: " + e.getMessage();
            }
        }

        private String executeReadFile(Map<String, Object> params) {
            String path = (String) params.get("path");
            try {
                return new String(
                        java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)),
                        "UTF-8"
                );
            } catch (Exception e) {
                return "文件读取失败: " + e.getMessage();
            }
        }

        /**
         * 检查是否结束
         */
        private boolean isFinished(String thought) {
            return thought.contains("Response:");
        }

        /**
         * 提取 Response
         */
        private String extractResponse(String thought) {
            int idx = thought.indexOf("Response:");
            if (idx >= 0) {
                return thought.substring(idx + 9).trim();
            }
            return thought;
        }

        /**
         * 提取 Action 行
         */
        private String extractAction(String thought) {
            int idx = thought.indexOf("Action:");
            if (idx >= 0) {
                int end = thought.indexOf("\n", idx);
                if (end < 0) {
                    end = thought.length();
                }

                return thought.substring(idx + 7, end).trim();
            }
            return null;
        }

        /**
         * 解析 Action 参数
         */
        @SuppressWarnings("unchecked")
        private Map<String, Object> parseAction(String actionLine) {
            Map<String, Object> result = new ConcurrentHashMap<String, Object>();

            // 简单解析: skill_name param1=value1 param2=value2
            String[] parts = actionLine.split(" ");
            if (parts.length > 0) {
                result.put("skill", parts[0]);
            }

            for (int i = 1; i < parts.length; i++) {
                String[] kv = parts[i].split("=", 2);
                if (kv.length == 2) {
                    result.put(kv[0], kv[1]);
                }
            }

            return result;
        }

        /**
         * 构建迭代输入
         */
        private String buildIterationInput(String userMessage) {
            StringBuilder input = new StringBuilder();
            input.append("原始用户输入:\n").append(userMessage).append("\n\n");
            input.append("执行历史:\n");
            for (String h : executionHistory) {
                input.append(h).append("\n");
            }
            input.append("\n请基于以上历史，继续执行或完成任务:");
            return input.toString();
        }

        /**
         * 工具方法: 对象转 JSON
         */
        private String toJson(Object obj) {
            try {
                return com.zifang.util.json.JsonUtil.toJson(obj);
            } catch (Exception e) {
                return obj.toString();
            }
        }
    }
}