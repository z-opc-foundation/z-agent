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
import com.zifang.z.agent.kernel.message.MessageType;
import com.zifang.z.agent.kernel.message.ToolCall;
import com.zifang.z.agent.kernel.tool.Tool;
import com.zifang.z.agent.kernel.tool.ToolResult;
import com.zifang.z.agent.kernel.types.MessageRole;
import com.zifang.z.agent.kernel.types.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ReAct agent 默认实现. 推理循环:
 *
 * <pre>
 *   for step in 0..maxSteps:
 *     response = llm.chat(messages + tools)
 *     if no tool_calls: return response.content
 *     for tool_call in response.tool_calls:
 *       result = execute(tool_call)
 *       messages += tool_result_msg(tool_call.id, result)
 * </pre>
 *
 * <p>支持 kernel.agent.Agent SPI: getName / run / streamRun / reset.
 * <p>支持 middleware 拦截 + event 分发(通过构造函数注入).
 */
public class ReActAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ReActAgent.class);

    private final AgentDto dto;
    private final AgentProperties props;
    private final LlmAdapter llm;
    private final List<Tool> tools;
    private final EventListener eventListener;

    public ReActAgent(AgentDto dto, AgentProperties props, LlmAdapter llm, List<Tool> tools) {
        this(dto, props, llm, tools, null);
    }

    public ReActAgent(AgentDto dto, AgentProperties props, LlmAdapter llm,
                      List<Tool> tools, EventListener eventListener) {
        if (dto == null) throw new IllegalArgumentException("dto required");
        if (props == null) throw new IllegalArgumentException("props required");
        if (llm == null) throw new IllegalArgumentException("llm required");
        this.dto = dto;
        this.props = props;
        this.llm = llm;
        this.tools = tools == null ? Collections.<Tool>emptyList() : Collections.unmodifiableList(new ArrayList<Tool>(tools));
        this.eventListener = eventListener;
    }

    @Override
    public String getName() {
        return dto.getName();
    }

    @Override
    public void reset() {
        // ReAct 无内部状态, reset = no-op
    }

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
        int maxSteps = resolveMaxSteps(request);
        List<Msg> messages = buildMessages(request);
        TokenUsage totalUsage = TokenUsage.empty();
        List<AgentResponse.Step> steps = new ArrayList<AgentResponse.Step>();
        String finishedReason = "unknown";

        for (int i = 0; i < maxSteps; i++) {
            ChatCompletionsRequest req = new ChatCompletionsRequest(
                    model, messages, tools.isEmpty() ? null : tools,
                    props.getDefaultTemperature(), props.getDefaultTopP(),
                    props.getDefaultMaxTokens(), false, null);

            ChatCompletionsResponse resp;
            try {
                resp = llm.chat(req);
            } catch (RuntimeException e) {
                finishedReason = "llm_error";
                log.warn("ReActAgent[{}] step {} LLM error: {}", dto.getName(), i, e.getMessage());
                Msg assistantMsg = Msg.assistant("[LLM error: " + e.getMessage() + "]");
                return new AgentResponse(assistantMsg, steps, totalUsage, finishedReason, e, null);
            }

            totalUsage = llm.addUsage(totalUsage, resp.getUsage());

            Msg assistantMsg = resp.toAssistantMsg();
            messages.add(assistantMsg);

            if (assistantMsg.getToolCalls().isEmpty()) {
                finishedReason = "completed";
                steps.add(new AgentResponse.Step(i, "thought", assistantMsg, null, null,
                        System.currentTimeMillis() - startMs));
                Map<String, Object> meta = new HashMap<String, Object>();
                meta.put("durationMs", System.currentTimeMillis() - startMs);
                return new AgentResponse(assistantMsg, steps, totalUsage, finishedReason, null, meta);
            }

            // 执行工具调用
            for (ToolCall tc : assistantMsg.getToolCalls()) {
                Tool tool = llm.findTool(tools, tc.getName());
                ToolResult result;
                if (tool == null) {
                    result = ToolResult.failure(tc.getId(), tc.getName(),
                            "tool not found: " + tc.getName());
                } else {
                    try {
                        Map<String, Object> args = llm.parseArguments(tc.getArgumentsJson());
                        result = tool.execute(args);
                    } catch (RuntimeException ex) {
                        result = ToolResult.failure(tc.getId(), tc.getName(), ex.getMessage());
                    } catch (Exception ex) {
                        result = ToolResult.failure(tc.getId(), tc.getName(), ex.getMessage());
                    }
                }
                messages.add(llm.toolResultMsg(tc.getId(), result));
                steps.add(new AgentResponse.Step(i, "action",
                        assistantMsg, Msg.assistant(tc.getArgumentsJson()),
                        Msg.user(result.getContent()),
                        System.currentTimeMillis() - startMs));
                if (eventListener != null) {
                    eventListener.onToolCall(dto.getName(), tc.getName(), result);
                }
            }
        }

        finishedReason = "max_steps";
        Msg finalMsg = messages.isEmpty()
                ? Msg.assistant("[ReAct: max steps reached without final output]")
                : messages.get(messages.size() - 1);
        Map<String, Object> meta = new HashMap<String, Object>();
        meta.put("durationMs", System.currentTimeMillis() - startMs);
        return new AgentResponse(finalMsg, steps, totalUsage, finishedReason, null, meta);
    }

    private String resolveModel(AgentRequest req) {
        if (req.getConfig() != null && req.getConfig().get("model") instanceof String) {
            return (String) req.getConfig().get("model");
        }
        if (dto.getModel() != null && !dto.getModel().isEmpty()) return dto.getModel();
        return props.getDefaultModel();
    }

    private int resolveMaxSteps(AgentRequest req) {
        if (req.getConfig() != null && req.getConfig().get("maxSteps") instanceof Integer) {
            return (Integer) req.getConfig().get("maxSteps");
        }
        return props.getDefaultMaxSteps();
    }

    private List<Msg> buildMessages(AgentRequest req) {
        List<Msg> out = new ArrayList<Msg>();
        if (dto.getSystemPrompt() != null && !dto.getSystemPrompt().isEmpty()) {
            out.add(Msg.system(dto.getSystemPrompt()));
        }
        if (req.getSystemPrompt() != null && !req.getSystemPrompt().isEmpty()) {
            out.add(Msg.system(req.getSystemPrompt()));
        }
        if (req.getHistory() != null) out.addAll(req.getHistory());
        if (req.getInput() != null) out.add(req.getInput());
        return out;
    }

    /**
     * 事件回调 SPI (供 AgentRunner / EventBus 桥接).
     */
    public interface EventListener {
        void onToolCall(String agentName, String toolName, ToolResult result);
    }
}