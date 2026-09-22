package com.zifang.z.agent.core.runner;

import com.zifang.z.agent.api.dto.AgentDto;
import com.zifang.z.agent.api.dto.RunRequest;
import com.zifang.z.agent.api.dto.RunResponse;
import com.zifang.z.agent.api.dto.StreamChunk;
import com.zifang.z.agent.api.dto.TokenUsageDto;
import com.zifang.z.agent.api.exception.AgentException;
import com.zifang.z.agent.core.registry.AgentRegistry;
import com.zifang.z.agent.kernel.agent.Agent;
import com.zifang.z.agent.kernel.agent.AgentRequest;
import com.zifang.z.agent.kernel.agent.AgentResponse;
import com.zifang.z.agent.kernel.message.Msg;
import com.zifang.z.agent.kernel.types.MessageRole;
import com.zifang.z.agent.kernel.types.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 调用门面. 负责:
 *
 * <ul>
 *   <li>RunRequest → AgentRequest 转换</li>
 *   <li>调 kernel.agent.Agent.run / streamRun</li>
 *   <li>AgentResponse → RunResponse / StreamChunk 转换</li>
 *   <li>中间件拦截 / 事件分发(未来扩展)</li>
 * </ul>
 */
public class AgentRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentRunner.class);

    private final AgentRegistry registry;

    public AgentRunner(AgentRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("registry required");
        this.registry = registry;
    }

    /**
     * 同步运行.
     */
    public RunResponse run(RunRequest req) {
        Agent agent = registry.get(req.getAgentName());
        AgentRequest agentReq = toAgentRequest(req);
        long startMs = System.currentTimeMillis();
        AgentResponse resp;
        try {
            resp = agent.run(agentReq);
        } catch (RuntimeException e) {
            throw AgentException.runFailed(req.getAgentName(), e);
        }
        log.info("AgentRunner.run[{}]: finished reason={}, usage={}",
                req.getAgentName(), resp.getFinishedReason(), resp.getUsage());
        return toRunResponse(req.getAgentName(), resp, System.currentTimeMillis() - startMs);
    }

    /**
     * 流式运行, 每个 step / done 回调.
     */
    public void stream(RunRequest req,
                       java.util.function.Consumer<StreamChunk> onChunk,
                       java.util.function.Consumer<Throwable> onError) {
        Agent agent = registry.get(req.getAgentName());
        AgentRequest agentReq = toAgentRequest(req);

        agent.streamRun(agentReq,
                step -> onChunk.accept(StreamChunk.step(req.getAgentName(), toStepDto(step))),
                resp -> {
                    onChunk.accept(StreamChunk.output(req.getAgentName(),
                            resp.getOutput() == null ? "" : resp.getOutput().getContent()));
                    onChunk.accept(StreamChunk.usage(req.getAgentName(),
                            toUsageDto(resp.getUsage())));
                    onChunk.accept(StreamChunk.done(req.getAgentName()));
                },
                err -> {
                    if (onError != null) onError.accept(err);
                });
    }

    /**
     * 列出注册的所有 agent.
     */
    public List<AgentDto> listAgents() {
        return registry.list();
    }

    public AgentDto describeAgent(String name) {
        return registry.describe(name);
    }

    private AgentRequest toAgentRequest(RunRequest req) {
        Msg input = req.getInput() == null ? null : Msg.user(req.getInput());
        List<Msg> history = toMsgList(req.getHistory());
        // toolNames 在 ReActAgent 内部通过 config 处理
        Map<String, Object> cfg = new HashMap<String, Object>();
        if (req.getConfig() != null) cfg.putAll(req.getConfig());
        if (req.getToolNames() != null && !req.getToolNames().isEmpty()) {
            cfg.put("toolNames", new ArrayList<String>(req.getToolNames()));
        }
        return new AgentRequest(input, history, null, null, cfg);
    }

    private List<Msg> toMsgList(List<Map<String, Object>> history) {
        if (history == null) return Collections.emptyList();
        List<Msg> out = new ArrayList<Msg>();
        for (Map<String, Object> h : history) {
            Object roleObj = h.get("role");
            String content = h.get("content") == null ? "" : h.get("content").toString();
            MessageRole role;
            if (roleObj == null) {
                role = MessageRole.USER;
            } else {
                String roleStr = roleObj.toString().toLowerCase();
                if ("assistant".equals(roleStr)) role = MessageRole.ASSISTANT;
                else if ("system".equals(roleStr)) role = MessageRole.SYSTEM;
                else if ("tool".equals(roleStr)) role = MessageRole.TOOL;
                else role = MessageRole.USER;
            }
            out.add(new Msg(role, content));
        }
        return out;
    }

    private RunResponse toRunResponse(String agentName, AgentResponse resp, long durationMs) {
        String outputText = resp.getOutput() == null ? "" : resp.getOutput().getContent();
        List<Map<String, Object>> stepsDto = new ArrayList<Map<String, Object>>();
        for (AgentResponse.Step s : resp.getSteps()) {
            Map<String, Object> sd = new HashMap<String, Object>();
            sd.put("index", s.getIndex());
            sd.put("kind", s.getKind());
            sd.put("durationMs", s.getDurationMs());
            sd.put("thought", s.getThought() == null ? null : s.getThought().getContent());
            sd.put("action", s.getAction() == null ? null : s.getAction().getContent());
            sd.put("observation", s.getObservation() == null ? null : s.getObservation().getContent());
            stepsDto.add(sd);
        }
        Map<String, Object> meta = new HashMap<String, Object>();
        if (resp.getMetadata() != null) meta.putAll(resp.getMetadata());
        meta.put("totalDurationMs", durationMs);
        String errorMessage = resp.getError() == null ? null : resp.getError().getMessage();
        return new RunResponse(agentName, outputText, stepsDto,
                toUsageDto(resp.getUsage()),
                resp.getFinishedReason(), errorMessage, meta);
    }

    private TokenUsageDto toUsageDto(TokenUsage u) {
        if (u == null) return TokenUsageDto.empty();
        return new TokenUsageDto(u.getPromptTokens(), u.getCompletionTokens(), u.getTotalTokens());
    }

    private Map<String, Object> toStepDto(AgentResponse.Step s) {
        Map<String, Object> sd = new HashMap<String, Object>();
        sd.put("index", s.getIndex());
        sd.put("kind", s.getKind());
        sd.put("thought", s.getThought() == null ? null : s.getThought().getContent());
        sd.put("action", s.getAction() == null ? null : s.getAction().getContent());
        sd.put("observation", s.getObservation() == null ? null : s.getObservation().getContent());
        return sd;
    }
}