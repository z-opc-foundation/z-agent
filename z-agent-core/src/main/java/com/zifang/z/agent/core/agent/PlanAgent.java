package com.zifang.z.agent.core.agent;

import com.zifang.z.agent.api.dto.AgentDto;
import com.zifang.z.agent.api.exception.AgentException;
import com.zifang.z.agent.core.properties.AgentProperties;
import com.zifang.z.agent.kernel.agent.Agent;
import com.zifang.z.agent.kernel.agent.AgentRequest;
import com.zifang.z.agent.kernel.agent.AgentResponse;
import com.zifang.z.agent.kernel.llm.ChatCompletionsRequest;
import com.zifang.z.agent.kernel.llm.ChatCompletionsResponse;
import com.zifang.z.agent.kernel.message.Msg;
import com.zifang.z.agent.kernel.tool.Tool;
import com.zifang.z.agent.kernel.types.MessageRole;
import com.zifang.z.agent.kernel.types.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plan agent 默认实现. 推理分两阶段:
 *
 * <pre>
 *   phase 1 (planning):
 *     plan = llm.chat(plan_prompt) — 返回有序步骤列表 (1. xxx 2. yyy 3. zzz)
 *   phase 2 (execution):
 *     for each step in plan:
 *       result = llm.chat(execute_step_prompt(step))
 *       step_results.append(result)
 *   final synthesis:
 *     final = llm.chat(synthesize_prompt(step_results))
 * </pre>
 *
 * <p>适合多步骤、需要中间规划的复杂任务(如 "写一份市场调研报告").
 * <p>不适合需要工具调用的任务(那种走 ReActAgent).
 */
public class PlanAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(PlanAgent.class);

    private static final Pattern PLAN_LINE = Pattern.compile("^\\s*(\\d+)[.\\)、]\\s*(.+)$");

    private final AgentDto dto;
    private final AgentProperties props;
    private final LlmAdapter llm;

    public PlanAgent(AgentDto dto, AgentProperties props, LlmAdapter llm) {
        if (dto == null) throw new IllegalArgumentException("dto required");
        if (props == null) throw new IllegalArgumentException("props required");
        if (llm == null) throw new IllegalArgumentException("llm required");
        this.dto = dto;
        this.props = props;
        this.llm = llm;
    }

    @Override
    public String getName() { return dto.getName(); }

    @Override
    public void reset() { /* no internal state */ }

    @Override
    public void streamRun(AgentRequest request,
                          java.util.function.Consumer<AgentResponse.Step> onStep,
                          java.util.function.Consumer<AgentResponse> onComplete,
                          java.util.function.Consumer<Throwable> onError) {
        try {
            AgentResponse resp = run(request);
            if (onComplete != null) onComplete.accept(resp);
        } catch (RuntimeException e) {
            if (onError != null) onError.accept(e);
            else throw e;
        }
    }

    @Override
    public AgentResponse run(AgentRequest request) {
        long startMs = System.currentTimeMillis();
        String model = resolveModel(request);
        List<Msg> history = request.getHistory() == null ? Collections.<Msg>emptyList() : request.getHistory();
        Msg input = request.getInput();

        // Phase 1: planning
        List<Msg> planPrompt = new ArrayList<Msg>();
        if (dto.getSystemPrompt() != null) planPrompt.add(Msg.system(dto.getSystemPrompt()));
        if (request.getSystemPrompt() != null) planPrompt.add(Msg.system(request.getSystemPrompt()));
        planPrompt.add(Msg.system("你是一个任务规划助手, 给定用户目标, 先拆解成有序步骤列表, 每行一个步骤, 格式: 1. xxx 2. yyy ..."));
        planPrompt.addAll(history);
        if (input != null) planPrompt.add(input);

        ChatCompletionsResponse planResp;
        try {
            planResp = llm.chat(new ChatCompletionsRequest(model, planPrompt, null,
                    props.getDefaultTemperature(), props.getDefaultTopP(), props.getDefaultMaxTokens(),
                    false, null));
        } catch (RuntimeException e) {
            return errorResponse("plan_failed", e, startMs);
        }
        TokenUsage totalUsage = planResp.getUsage();
        String planText = planResp.toAssistantMsg().getContent();
        List<String> steps = parsePlan(planText);
        List<AgentResponse.Step> trace = new ArrayList<AgentResponse.Step>();
        trace.add(new AgentResponse.Step(0, "plan",
                planResp.toAssistantMsg(), null, null,
                System.currentTimeMillis() - startMs));

        // Phase 2: execution
        List<Msg> stepResults = new ArrayList<Msg>();
        for (int i = 0; i < steps.size(); i++) {
            String step = steps.get(i);
            List<Msg> execPrompt = new ArrayList<Msg>();
            execPrompt.add(Msg.system("你正在执行步骤 " + (i + 1) + "/" + steps.size() + ": " + step));
            execPrompt.add(Msg.user("执行这个步骤并给出结果."));
            ChatCompletionsResponse execResp;
            try {
                execResp = llm.chat(new ChatCompletionsRequest(model, execPrompt, null,
                        props.getDefaultTemperature(), props.getDefaultTopP(), props.getDefaultMaxTokens(),
                        false, null));
            } catch (RuntimeException e) {
                return errorResponse("step_failed", e, startMs);
            }
            totalUsage = llm.addUsage(totalUsage, execResp.getUsage());
            String result = execResp.toAssistantMsg().getContent();
            stepResults.add(Msg.assistant("[Step " + (i + 1) + "] " + step + "\n" + result));
            trace.add(new AgentResponse.Step(i + 1, "step",
                    Msg.assistant(step), null,
                    Msg.assistant(result), System.currentTimeMillis() - startMs));
        }

        // Final synthesis
        List<Msg> synthPrompt = new ArrayList<Msg>();
        synthPrompt.add(Msg.system("基于以下步骤结果, 给用户最终答复:"));
        synthPrompt.addAll(stepResults);
        ChatCompletionsResponse synthResp;
        try {
            synthResp = llm.chat(new ChatCompletionsRequest(model, synthPrompt, null,
                    props.getDefaultTemperature(), props.getDefaultTopP(), props.getDefaultMaxTokens(),
                    false, null));
        } catch (RuntimeException e) {
            return errorResponse("synthesis_failed", e, startMs);
        }
        totalUsage = llm.addUsage(totalUsage, synthResp.getUsage());

        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("durationMs", System.currentTimeMillis() - startMs);
        meta.put("planSteps", steps.size());
        return new AgentResponse(synthResp.toAssistantMsg(), trace, totalUsage, "completed", null, meta);
    }

    private AgentResponse errorResponse(String reason, Throwable e, long startMs) {
        log.warn("PlanAgent[{}] {}: {}", dto.getName(), reason, e.getMessage());
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("durationMs", System.currentTimeMillis() - startMs);
        return new AgentResponse(
                Msg.assistant("[Plan failed: " + reason + "] " + e.getMessage()),
                Collections.<AgentResponse.Step>emptyList(),
                TokenUsage.empty(), reason, e, meta);
    }

    private String resolveModel(AgentRequest req) {
        if (req.getConfig() != null && req.getConfig().get("model") instanceof String) {
            return (String) req.getConfig().get("model");
        }
        if (dto.getModel() != null && !dto.getModel().isEmpty()) return dto.getModel();
        return props.getDefaultModel();
    }

    static List<String> parsePlan(String planText) {
        List<String> out = new ArrayList<String>();
        if (planText == null || planText.trim().isEmpty()) return out;
        for (String line : planText.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            Matcher m = PLAN_LINE.matcher(trimmed);
            if (m.find()) {
                out.add(m.group(2).trim());
            }
        }
        if (out.isEmpty()) {
            // 没识别到 numbered 列表, 把整个文本当一步
            String whole = planText.trim();
            if (!whole.isEmpty()) out.add(whole);
        }
        return out;
    }
}