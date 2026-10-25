package com.zifang.z.agent.llm.agent.runtime;

import com.zifang.z.agent.llm.agent.observability.TurnResult;
import com.zifang.z.agent.kernel.tool.BaseTool;
import com.zifang.z.agent.llm.agent.turn.TurnRun;
import com.zifang.z.agent.llm.builtins.extensions.session.InMemorySessionStateStore;
import com.zifang.z.agent.llm.builtins.extensions.session.SessionStateStore;
import com.zifang.z.agent.llm.infrastructure.events.AgentMessage;
import com.zifang.z.agent.llm.infrastructure.events.Message;
import com.zifang.z.agent.llm.infrastructure.llm.context.LlmStreamRequest;
import com.zifang.z.agent.llm.infrastructure.llm.engine.LlmReactEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * ReAct (Reason + Act) Agent runtime.
 * Supports tool registration, context builders, turn lifecycle, and both sync and streaming chat.
 */
public class ReactAgent {

    private final String name;
    private final String id;
    private final String sessionId;
    private final String llmCode;
    private final LlmStreamRequest.StreamOptions defaultStreamOptions;
    private final LlmReactEngine llmEngine;
    private final List<BaseTool> tools;
    private final List<Consumer<TurnContext>> turnEndListeners;
    private final List<java.util.function.Function<TurnContext, List<AgentMessage>>> contextBuilders;
    private final java.util.List<Consumer<TurnContext>> turnStartListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private String systemPrompt;
    private SessionStateStore sessionStateStore;
    private ActiveRun currentActiveRun;
    private volatile boolean cancelled = false;
    private volatile TurnResult lastTurnResult;

    public ReactAgent(String name, String systemPrompt, String id, String sessionId,
                      String llmCode, LlmStreamRequest.StreamOptions defaultStreamOptions,
                      LlmReactEngine llmEngine, List<BaseTool> tools) {
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.id = id;
        this.sessionId = sessionId;
        this.llmCode = llmCode;
        this.defaultStreamOptions = defaultStreamOptions;
        this.llmEngine = llmEngine;
        this.tools = new CopyOnWriteArrayList<>(tools != null ? tools : Collections.<BaseTool>emptyList());
        this.sessionStateStore = new InMemorySessionStateStore();
        this.turnEndListeners = new CopyOnWriteArrayList<>();
        this.contextBuilders = new CopyOnWriteArrayList<>();
    }

    private static Message toMessage(AgentMessage am) {
        return new Message(am.getRole(), am.getContent());
    }

    public void registerTool(BaseTool tool) {
        if (tool != null) {
            tools.add(tool);
        }
    }

    public void bindSessionStateStore(SessionStateStore store) {
        if (store != null) {
            this.sessionStateStore = store;
        }
    }

    // ---- Phase 7 extensions: onTurnStart / onAgentEvent / lastTurnResult ----

    public void addContextBuilder(java.util.function.Function<TurnContext, List<AgentMessage>> builder) {
        if (builder != null) {
            contextBuilders.add(builder);
        }
    }

    public void onTurnEnd(Consumer<TurnContext> listener) {
        if (listener != null) {
            turnEndListeners.add(listener);
        }
    }

    public void onTurnStart(Consumer<TurnContext> listener) {
        if (listener != null) {
            turnStartListeners.add(listener);
        }
    }

    public void onAgentEvent(java.util.function.BiConsumer<AgentEvent, AgentEventContext> listener) {
        // Phase 7 stub: agent event listeners are registered but not yet dispatched
        // in the base ReactAgent run-loop (future enhancement).
    }

    public TurnResult lastTurnResult() {
        return lastTurnResult;
    }

    /**
     * @internal called by TurnRun after turn completes
     */
    public void setLastTurnResult(TurnResult result) {
        this.lastTurnResult = result;
    }

    public ActiveRun currentActiveRun() {
        return currentActiveRun;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public void applySystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String chat(Message userMessage) {
        List<Message> messages = buildMessages(userMessage);
        LlmStreamRequest request = new LlmStreamRequest(llmCode, defaultStreamOptions);
        return llmEngine.chat(messages, request);
    }

    public String chat() {
        List<Message> messages = new ArrayList<>();
        TurnState state = sessionStateStore.loadTurnState();
        List<AgentMessage> stored = state.messages();
        for (AgentMessage am : stored) {
            messages.add(toMessage(am));
        }
        LlmStreamRequest request = new LlmStreamRequest(llmCode, defaultStreamOptions);
        String result = llmEngine.chat(messages, request);
        sessionStateStore.appendMessage(AgentMessage.assistant(result));
        return result;
    }

    /**
     * Start a stream with fluent callback registration.
     */
    public AgentStream stream(Message userMessage) {
        return new AgentStream(stream -> {
            List<Message> messages = buildMessages(userMessage);
            LlmStreamRequest request = new LlmStreamRequest(llmCode, defaultStreamOptions);
            // 关键修复：把 LLM token / completion / error 真实转发到 AgentStream 注册的回调。
            // 之前是空实现，导致 SSE 232 字符被 LlmReactEngine 内部 acc 累加但从来不到业务层。
            // Run-loop 用 try/catch 包裹，provider 抛错时通过 stream.fireError 走 onError 回调。
            final StringBuilder acc = new StringBuilder();
            try {
                llmEngine.stream(messages, request, new LlmReactEngine.StreamHandler() {
                    @Override
                    public void onText(String text) {
                        if (text != null && !text.isEmpty()) {
                            acc.append(text);
                            stream.fireText(text);
                        }
                    }

                    @Override
                    public void onThought(String thought) {
                        // 思考走独立通道（2026-10-25）：不再混进正文（老协议下思考以 "[REASONING:]"
                        // 混在正文里 ⇒ 群聊派单的 JSON 被思考碎片切开 ⇒ 解析必失败）。
                        // 这里把思考喂给 AgentStream 的 thought 回调（此前该回调无人触发，是条死路）。
                        if (thought != null && !thought.isEmpty()) {
                            stream.fireThought(thought);
                        }
                    }

                    @Override
                    public void onComplete() {
                        TurnResult tr = new TurnResult(acc.toString(), null);
                        stream.fireTurnFinish(acc.toString(), tr);
                    }

                    @Override
                    public void onError(Throwable t) {
                        stream.fireError(t);
                    }
                });
            } catch (Throwable t) {
                stream.fireError(t);
            }
        });
    }

    /**
     * Phase 7 扩展：双参 stream(Message, Consumer<AgentStream>) 重载，
     * 兼容 0815 业务 ft 调用模式（lambda 形式配置回调）。
     *
     * <p>实现策略：构造完 AgentStream 后立即把回调装载好，再通过 starter 触发 start，
     * 与单参 stream 等价。</p>
     */
    public AgentStream stream(Message userMessage, Consumer<AgentStream> configurer) {
        AgentStream stream = stream(userMessage);
        if (configurer != null) {
            configurer.accept(stream);
        }
        // 如果 configurer 注册了 onTurnFinish 或 onError（这些会自动 fire start），stream 自动启动；
        // 否则需要外部显式 start。0815 业务都注册了 onToken/onError，因此本处不强制启动。
        return stream;
    }

    /**
     * Phase 7 扩展：单参 stream(Consumer<AgentStream>) 重载，
     * 兼容 0815 业务 ft 调用模式（不带 Message，仅设置回调）。
     * <p>会用空 Message 作为 userMessage，效果上等价于 stream(new UserMessage(""))。</p>
     */
    public AgentStream stream(Consumer<AgentStream> configurer) {
        return stream(Message.user(""), configurer);
    }

    /**
     * Phase 7 扩展：三参 stream(Message, Object, Consumer<AgentStream>) 重载，
     * 兼容 0815 EphemeralLlmRunner 三参调用模式（null 第二个参数为 context span 透传）。
     */
    public AgentStream stream(Message userMessage, Object contextSpan, Consumer<AgentStream> configurer) {
        if (contextSpan != null) {
            // Phase 7 存根：contextSpan 暂时不处理，0815 业务传 null 即可。
        }
        return stream(userMessage, configurer);
    }

    public String getName() {
        return name;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getLlmCode() {
        return llmCode;
    }

    public LlmReactEngine getLlmEngine() {
        return llmEngine;
    }

    public List<BaseTool> getTools() {
        return Collections.unmodifiableList(tools);
    }

    public SessionStateStore sessionStateStore() {
        return sessionStateStore;
    }

    private List<Message> buildMessages(Message userMessage) {
        List<Message> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Message.system(systemPrompt));
        }

        TurnState state = sessionStateStore.loadTurnState();
        List<AgentMessage> stored = state.messages();
        for (AgentMessage am : stored) {
            messages.add(toMessage(am));
        }

        if (userMessage != null) {
            messages.add(userMessage);
        }

        return messages;
    }

    // ---- Inner classes ----

    public static class ActiveRun {
        private final TurnRun turnRun;
        private final TurnResult turnResult;

        public ActiveRun(TurnRun turnRun, TurnResult turnResult) {
            this.turnRun = turnRun;
            this.turnResult = turnResult;
        }

        public TurnRun turnRun() {
            return turnRun;
        }

        public TurnResult turnResult() {
            return turnResult;
        }
    }
}
