package com.zifang.z.agent.engine.agent.plan;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.engine.agent.llm.model.ChatCompletionsRequest;
import com.zifang.z.agent.engine.agent.llm.model.ChatMessage;
import com.zifang.z.agent.engine.agent.llm.provider.LlmClient;
import com.zifang.z.agent.engine.agent.plan.model.Plan;
import com.zifang.z.agent.engine.agent.plan.model.PlanStep;
import com.zifang.z.agent.engine.agent.skill.Skill;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 基于 LLM 的规划器。
 * 向 LLM 发送系统提示 + 用户目标 + skills 列表，
 * 要求 LLM 返回 JSON 格式的 Plan，然后解析为 Plan 对象。
 */
public class LlmPlanner implements Planner {

    private static final String PLANNER_PROMPT_TEMPLATE =
            "你是一个任务规划专家。用户将提出一个复杂目标，你需要将其分解为多个步骤，" +
                    "每个步骤对应一个可执行的 skill。\n" +
                    "可用的 skills：\n%s\n" +
                    "你的任务：根据用户目标，输出一份 JSON 格式的执行计划。\n" +
                    "格式要求：\n" +
                    "{\"steps\": [\n" +
                    "  {\"goal\": \"本步目标描述\", \"skillName\": \"skill名称\", \"params\": {\"参数名\": \"参数值\"}},\n" +
                    "  ...\n" +
                    "]}\n" +
                    "重要：只输出 JSON，不要输出任何其他内容。\n" +
                    "用户目标：%s%s";
    private final LlmClient llmClient;
    private final String providerCode;
    private final String model;

    public LlmPlanner(LlmClient llmClient, String providerCode, String model) {
        this.llmClient = llmClient;
        this.providerCode = providerCode;
        this.model = model;
    }

    @Override
    public Plan decompose(String goal, List<Skill> availableSkills, String memoryContext) {
        String skillsDesc = buildSkillsDesc(availableSkills);
        String contextPart = (memoryContext != null && !memoryContext.isEmpty())
                ? "\n相关上下文：\n" + memoryContext
                : "";
        String prompt = String.format(PLANNER_PROMPT_TEMPLATE, skillsDesc, goal, contextPart);

        List<ChatMessage> messages = Collections.singletonList(ChatMessage.user(prompt));
        ChatCompletionsRequest request = ChatCompletionsRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.3)
                .maxTokens(2048)
                .build();

        StringBuilder jsonResponse = new StringBuilder();
        try {
            llmClient.streamChatFlux(providerCode, request)
                    .doOnNext(delta -> {
                        if (delta.getContent() != null) {
                            jsonResponse.append(delta.getContent());
                        }
                    })
                    .blockLast();
        } catch (Exception e) {
            System.err.println("LlmPlanner LLM 调用失败: " + e.getMessage());
            return createFallbackPlan(goal);
        }

        return parseJsonPlan(jsonResponse.toString(), goal);
    }

    /**
     * 解析 LLM 返回的 JSON plan。
     * 处理 markdown 代码块包裹等干扰。
     */
    private Plan parseJsonPlan(String json, String originalGoal) {
        Plan plan = new Plan(originalGoal);
        try {
            // 提取 JSON 对象（去掉 markdown 代码块等干扰）
            json = json.trim();
            if (json.startsWith("```")) {
                int firstBrace = json.indexOf('{');
                int lastBrace = json.lastIndexOf('}');
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    json = json.substring(firstBrace, lastBrace + 1);
                }
            }
            // 再次检查去除可能的 markdown 标记
            json = json.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "").trim();

            // 注意：必须过 toPlainValue —— parseToMap 是浅的，嵌套节点仍是 JsonObject/JsonArray，
            // 直接 get("steps") 再强转 List<Map<String,Object>> 会 ClassCastException
            Map<String, Object> root = toPlainMap(json);
            List<Map<String, Object>> stepsConf = (List<Map<String, Object>>) root.get("steps");
            if (stepsConf == null || stepsConf.isEmpty()) {
                return createFallbackPlan(originalGoal);
            }
            for (int i = 0; i < stepsConf.size(); i++) {
                Map<String, Object> s = stepsConf.get(i);
                PlanStep step = PlanStep.builder()
                        .order(i)
                        .goal(String.valueOf(s.get("goal")))
                        .skillName(String.valueOf(s.get("skillName")))
                        .params(s.get("params") != null ? (Map<String, Object>) s.get("params") : Collections.emptyMap())
                        .build();
                plan.addStep(step);
            }
        } catch (Exception e) {
            System.err.println("LlmPlanner JSON 解析失败: " + e.getMessage() + ", raw: " + json);
            return createFallbackPlan(originalGoal);
        }
        return plan;
    }

    /**
     * 创建降级计划：当 LLM 调用失败或解析失败时返回单步计划。
     */
    private Plan createFallbackPlan(String goal) {
        Plan plan = new Plan(goal);
        plan.addStep(PlanStep.builder()
                .order(0)
                .goal(goal)
                .skillName("unknown")
                .params(Collections.emptyMap())
                .build());
        return plan;
    }

    /**
     * 构建 skills 描述字符串，用于 prompt。
     */
    private String buildSkillsDesc(List<Skill> skills) {
        StringBuilder sb = new StringBuilder();
        if (skills == null || skills.isEmpty()) {
            sb.append("(无可用 skill)");
        } else {
            for (Skill s : skills) {
                sb.append("- ").append(s.getName()).append(": ")
                        .append(s.getDescription() != null ? s.getDescription() : "")
                        .append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public void markStepCompleted(Plan plan, int stepIndex, String result) {
        if (plan != null && plan.getSteps() != null && stepIndex >= 0 && stepIndex < plan.getSteps().size()) {
            PlanStep step = plan.getSteps().get(stepIndex);
            step.setCompleted(true);
            step.setResult(result);
        }
    }

    @Override
    public boolean needsPlanning(String goal) {
        // 启发式：目标长度 > 30 字符 且 不以"查询"、"告诉我"、"what is" 开头
        if (goal == null || goal.length() <= 30) {
            return false;
        }
        String lower = goal.toLowerCase();
        if (lower.startsWith("查询") || lower.startsWith("告诉我") || lower.startsWith("what is")
                || lower.startsWith("find ") || lower.startsWith("get ") || lower.startsWith("show ")
                || lower.startsWith("帮我查") || lower.startsWith("请问")) {
            return false;
        }
        return true;
    }

    /**
     * JSON 字符串 → 深度摊平的 {@code Map<String,Object>}。
     * <p>
     * 必须过 {@code JsonUtil.toPlainValue}：{@code parseToMap} 只摊平第一层，
     * 嵌套节点仍是 {@code JsonObject}/{@code JsonArray}，{@code get("steps")} 拿到后
     * 强转 {@code List<Map<String,Object>>} 会 ClassCastException。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toPlainMap(String json) {
        return (Map<String, Object>) JsonUtil.toPlainValue(JsonUtil.parse(json));
    }
}