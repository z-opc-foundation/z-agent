package com.zifang.z.agent.engine.agent.plugin;

import com.zifang.z.agent.engine.agent.react.Agent;
import com.zifang.z.agent.engine.agent.react.ReActAgent;
import com.zifang.z.agent.engine.agent.react.StreamEvent;
import reactor.core.publisher.Flux;

/**
 * 为 ReActAgent 装饰一层生命周期钩子分发。
 * 在关键节点调用 LifecycleHookDispatcher。
 */
public class HookedReActAgent implements Agent {
    private final ReActAgent delegate;
    private final LifecycleHookDispatcher dispatcher;
    private final String sessionId;

    public HookedReActAgent(ReActAgent delegate, LifecycleHookDispatcher dispatcher, String sessionId) {
        this.delegate = delegate;
        this.dispatcher = dispatcher;
        this.sessionId = sessionId;
        dispatcher.dispatchCreated(delegate);
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public Flux<StreamEvent> stream(String input) {
        dispatcher.dispatchStarted(sessionId, input);
        return delegate.stream(input)
                .filter(e -> {
                    // 在这里分发各阶段事件
                    if (e instanceof StreamEvent.StepStart) {
                        dispatcher.dispatchStepStart(sessionId, ((StreamEvent.StepStart) e).step);
                    }
                    if (e instanceof StreamEvent.ToolCallRequest) {
                        StreamEvent.ToolCallRequest tc = (StreamEvent.ToolCallRequest) e;
                        dispatcher.dispatchToolCall(sessionId, tc.name, tc.arguments);
                    }
                    if (e instanceof StreamEvent.ToolResult) {
                        StreamEvent.ToolResult tr = (StreamEvent.ToolResult) e;
                        dispatcher.dispatchToolResult(sessionId, tr.name,
                                tr.result != null ? tr.result.toString() : null, tr.success);
                    }
                    if (e instanceof StreamEvent.Done) {
                        StreamEvent.Done d = (StreamEvent.Done) e;
                        dispatcher.dispatchFinished(sessionId, "SUCCESS", d.reply);
                    }
                    if (e instanceof StreamEvent.ErrorEvent) {
                        dispatcher.dispatchError(sessionId, ((StreamEvent.ErrorEvent) e).cause);
                    }
                    return true;
                });
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }
}