package com.zifang.z.agent.engine.agent.observe;

import com.zifang.z.agent.engine.agent.observe.model.AgentSpan;
import com.zifang.z.agent.engine.agent.observe.model.AgentTrace;
import com.zifang.z.agent.engine.agent.react.Agent;
import com.zifang.z.agent.engine.agent.react.ReActAgent;
import com.zifang.z.agent.engine.agent.react.StreamEvent;
import reactor.core.publisher.Flux;

/**
 * 为 ReActAgent 装饰一层可观测性。
 * 在关键节点调用 AgentTracer 和 AgentMetrics。
 */
public class ObservabilityReActAgent implements Agent {
    private final ReActAgent delegate;
    private final AgentTracer tracer;
    private final AgentMetrics metrics;
    private final String sessionId;

    public ObservabilityReActAgent(ReActAgent delegate, AgentTracer tracer, AgentMetrics metrics, String sessionId) {
        this.delegate = delegate;
        this.tracer = tracer;
        this.metrics = metrics;
        this.sessionId = sessionId;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public Flux<StreamEvent> stream(String input) {
        AgentTrace trace = tracer.beginTrace(sessionId, delegate.name(), input);
        metrics.incAgentExecutions(delegate.name(), "STARTED");

        return delegate.stream(input)
                .doOnNext(e -> {
                    if (e instanceof StreamEvent.StepStart) {
                        AgentSpan span = tracer.beginSpan(null, "step_" + ((StreamEvent.StepStart) e).step);
                        span.setAttribute("step", ((StreamEvent.StepStart) e).step);
                    }
                    if (e instanceof StreamEvent.ToolCallRequest) {
                        StreamEvent.ToolCallRequest tc = (StreamEvent.ToolCallRequest) e;
                        AgentSpan span = tracer.beginSpan(null, "tool_call");
                        span.setAttribute("tool_name", tc.name);
                        span.setAttribute("arguments", tc.argumentsJson);
                    }
                    if (e instanceof StreamEvent.ToolResult) {
                        StreamEvent.ToolResult tr = (StreamEvent.ToolResult) e;
                        metrics.incToolCalls(tr.name, tr.success ? "SUCCESS" : "FAILURE");
                        tracer.endSpan(null, tr.success ? "SUCCESS" : "FAILURE", tr.error);
                    }
                    if (e instanceof StreamEvent.Done) {
                        tracer.endTrace(trace, "SUCCESS");
                        metrics.incAgentExecutions(delegate.name(), "SUCCESS");
                    }
                    if (e instanceof StreamEvent.ErrorEvent) {
                        tracer.endTrace(trace, "FAILURE");
                        metrics.incAgentExecutions(delegate.name(), "FAILURE");
                    }
                })
                .doOnError(err -> {
                    tracer.endTrace(trace, "ERROR");
                    metrics.incAgentExecutions(delegate.name(), "ERROR");
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