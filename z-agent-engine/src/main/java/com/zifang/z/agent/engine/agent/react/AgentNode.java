package com.zifang.z.agent.engine.agent.react;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AgentNode — SubAgent 的具体实现
 * <p>
 * 实现任务树节点的完整生命周期管理。
 * 内部运行自己的 ReAct 循环，通过 parent 共享 Memory。
 */
public class AgentNode implements SubAgent {

    private final String name;
    private final String task;
    private final AgentContext context;
    private final ReActAgent reActAgent;
    private final AtomicInteger state = new AtomicInteger(State.CREATED.ordinal());
    private final List<SubAgent> children = new ArrayList<>();
    private final Object childrenLock = new Object();
    // 结果完成通知
    private final CompletableFuture<String> resultFuture = new CompletableFuture<>();
    private volatile String result;
    private volatile String error;
    private volatile Thread runThread;

    public AgentNode(String name, String task, AgentContext context, ReActAgent reActAgent) {
        this.name = name;
        this.task = task;
        this.context = context;
        this.reActAgent = reActAgent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public State getState() {
        return State.values()[state.get()];
    }

    @Override
    public String getTask() {
        return task;
    }

    @Override
    public SubAgent getParent() {
        return context.getParent();
    }

    @Override
    public String getResult() {
        return result;
    }

    @Override
    public String getError() {
        return error;
    }

    public CompletableFuture<String> getResultFuture() {
        return resultFuture;
    }

    @Override
    public void start() {
        if (!state.compareAndSet(State.CREATED.ordinal(), State.RUNNING.ordinal())) {
            return; // 已经启动或完成
        }

        runThread = new Thread(() -> {
            try {
                // 在子上下文中执行（memory 父子共享）
                AgentContext childContext = context.fork(this);

                // 添加子任务开始记录到 memory
                context.addToMemory("system", "[SubAgent '" + name + "' started] " + task);

                // 运行 ReAct 循环
                String outcome = reActAgent.run(task, childContext);

                this.result = outcome;
                state.set(State.COMPLETED.ordinal());
                resultFuture.complete(outcome);

                context.addToMemory("system", "[SubAgent '" + name + "' completed] result=" + truncate(outcome, 200));

            } catch (Exception e) {
                this.error = e.getMessage();
                state.set(State.FAILED.ordinal());
                resultFuture.completeExceptionally(e);

                context.addToMemory("system", "[SubAgent '" + name + "' failed] " + e.getMessage());
            }
        }, "SubAgent-" + name);

        runThread.start();
    }

    @Override
    public void await() throws InterruptedException {
        Thread t = this.runThread;
        if (t != null) {
            t.join();
        }
    }

    @Override
    public void kill() {
        state.set(State.CANCELLED.ordinal());

        Thread t = this.runThread;
        if (t != null && t.isAlive()) {
            t.interrupt();
        }

        // 同时 kill 所有子节点
        synchronized (childrenLock) {
            for (SubAgent child : children) {
                child.kill();
            }
        }

        context.addToMemory("system", "[SubAgent '" + name + "' killed]");
    }

    @Override
    public List<SubAgent> getChildren() {
        synchronized (childrenLock) {
            return new ArrayList<>(children);
        }
    }

    /**
     * 注册子代理
     */
    public void addChild(SubAgent child) {
        synchronized (childrenLock) {
            children.add(child);
        }
    }

    // ========== 工具方法 ==========
    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "null";
        }

        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    @Override
    public String toString() {
        return "AgentNode[" + name + "|" + getState() + "]";
    }
}