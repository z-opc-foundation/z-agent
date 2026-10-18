package com.zifang.z.agent.llm.agent.runtime;

import com.zifang.z.agent.llm.agent.observability.TurnResult;
import com.zifang.z.agent.llm.infrastructure.events.ToolCallData;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent stream — fluent builder for registering stream event callbacks.
 * <p>
 * 实现要点：
 * - 6 个 callback 现在真实存储（之前是 stub return this）
 * - starter 字段类型改为 Consumer<AgentStream>，让起动函数能拿到 AgentStream 引用并 fire 回调
 * - 自动 start()：当第一次注册 onTurnFinish / onError 时自动调 starter.run()（按需）
 * - start() 也可被外部显式调用
 * - 防止重复 start（一次只能 start 一次）
 */
public class AgentStream {

    private final StreamStarter starter;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private Callback<String> textHandler;
    private Callback<String> thoughtHandler;
    private Callback<List<ToolCallData>> toolCallsHandler;
    private Callback2<String, TurnResult> turnFinishHandler;
    private Callback4<String, String, String, Boolean> toolResultHandler;
    private Callback<Throwable> errorHandler;

    public AgentStream(StreamStarter starter) {
        this.starter = starter;
    }

    /**
     * 兼容旧 Runnable 构造器（starter 直接跑，无 agent stream 引用）
     */
    public AgentStream(Runnable starter) {
        this.starter = stream -> starter.run();
    }

    public AgentStream onText(Callback<String> handler) {
        this.textHandler = handler;
        return this;
    }

    public AgentStream onThought(Callback<String> handler) {
        this.thoughtHandler = handler;
        return this;
    }

    public AgentStream onToolCalls(Callback<List<ToolCallData>> handler) {
        this.toolCallsHandler = handler;
        return this;
    }

    public AgentStream onTurnFinish(Callback2<String, TurnResult> handler) {
        this.turnFinishHandler = handler;
        // 注册了 onTurnFinish 视作用户完成事件订阅，自动启动流。
        autoStart();
        return this;
    }

    public AgentStream onToolResult(Callback4<String, String, String, Boolean> handler) {
        this.toolResultHandler = handler;
        return this;
    }

    public AgentStream onError(Callback<Throwable> handler) {
        this.errorHandler = handler;
        // 注册 onError 同样视作用户准备就绪，自动启动流（防止仅注册 onError 的代码忘了 start）。
        autoStart();
        return this;
    }

    private void autoStart() {
        if (started.compareAndSet(false, true) && starter != null) {
            // 异步启动，避免在 setter 链里抛 start 内的异常打断链式调用。
            try {
                starter.run(this);
            } catch (Throwable t) {
                fireError(t);
            }
        }
    }

    /**
     * 显式启动（同步）。如果用户没有注册任何 callback，也可以无 starter 跑。
     */
    public void start() {
        if (started.compareAndSet(false, true) && starter != null) {
            starter.run(this);
        }
    }

    public void fireText(String text) {
        if (textHandler != null) {
            try {
                textHandler.accept(text);
            } catch (Throwable t) {
                fireError(t);
            }
        }
    }

    // ─── starter 内部触发回调的入口 ────────────────────────────────────

    public void fireThought(String text) {
        if (thoughtHandler != null) {
            try {
                thoughtHandler.accept(text);
            } catch (Throwable t) {
                fireError(t);
            }
        }
    }

    public void fireToolCalls(List<ToolCallData> calls) {
        if (toolCallsHandler != null) {
            try {
                toolCallsHandler.accept(calls);
            } catch (Throwable t) {
                fireError(t);
            }
        }
    }

    public void fireTurnFinish(String content, TurnResult turnResult) {
        if (turnFinishHandler != null) {
            try {
                turnFinishHandler.accept(content, turnResult);
            } catch (Throwable t) {
                fireError(t);
            }
        }
    }

    public void fireToolResult(String callId, String name, String result, boolean isError) {
        if (toolResultHandler != null) {
            try {
                toolResultHandler.accept(callId, name, result, isError);
            } catch (Throwable t) {
                fireError(t);
            }
        }
    }

    public void fireError(Throwable t) {
        if (errorHandler != null) {
            try {
                errorHandler.accept(t);
            } catch (Throwable ignored) {
                // 错误回调里自身的异常不能再触发，避免无限递归
            }
        }
    }

    /**
     * Dispatch a runtime event (used by CompactionExtension).
     * Currently a no-op sink for extension-generated events.
     */
    public void dispatch(AgentEvent event, AgentEventContext context) {
        // no-op: extension events are logged by the extension itself
    }

    /**
     * 启动函数：拿到 AgentStream 引用后调 onText/onTurnFinish/onError 等
     */
    public interface StreamStarter {
        void run(AgentStream stream);
    }

    /**
     * Generic callback interface.
     */
    public interface Callback<T> {
        void accept(T value);
    }

    /**
     * Bi-argument callback.
     */
    public interface Callback2<A, B> {
        void accept(A a, B b);
    }

    /**
     * Four-argument callback.
     */
    public interface Callback4<A, B, C, D> {
        void accept(A a, B b, C c, D d);
    }
}
