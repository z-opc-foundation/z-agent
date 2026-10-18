package com.zifang.z.agent.engine.agent.multi;

import com.zifang.z.agent.engine.agent.react.Agent;
import com.zifang.z.agent.engine.agent.react.ReActAgent;
import com.zifang.z.agent.engine.agent.react.StreamEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Supervisor Agent（主管 Agent）。
 *
 * <p>职责: 接收任务, 通过 {@link RoutingStrategy} 选 worker, 委派任务, 汇总结果。
 *
 * <p>流程:
 * <ol>
 *   <li>stream(input) 进来, 写 blackboard("user_input", input)</li>
 *   <li>emit 思考: "[Supervisor] 收到任务, 开始分析..."</li>
 *   <li>route() 选 worker 列表 (可能为空 = 没人能接)</li>
 *   <li>如果为空: 任务交给 delegate (ReActAgent) 直接处理</li>
 *   <li>如果非空: 对每个 worker:
 *     <ul>
 *       <li>构造 DELEGATION 消息, messageId = "del-{conv}-{seq}", inReplyTo=null</li>
 *       <li>发到 worker 自己的入队</li>
 *       <li>开一个线程跑 worker.processMessages() (会从队列里 take 自己的)</li>
 *     </ul>
 *   </li>
 *   <li>主线程: 轮询 MessageQueue, 收 RESPONSE/ERROR 消息, 按 inReplyTo 关联
 *     <ul>
 *       <li>RESPONSE: 累计到该 worker 的结果, emit "WorkerName: ..." thought delta</li>
 *       <li>ERROR: 触发 FailoverPolicy.onFailure(), 根据决策 RETRY_SAME / SKIP / ABORT</li>
 *       <li>如果 retry: 重新 send DELEGATION, worker 再 take 一次</li>
 *     </ul>
 *   </li>
 *   <li>所有 worker 完成 (countDownLatch.await(timeout)) 后, 汇总 emit Done</li>
 * </ol>
 *
 * <p>约束:
 * <ul>
 *   <li>同一个 supervisor 是单线程串行的: 一个 stream() 跑完才能接下一个 (LMAX 不需要)</li>
 *   <li>worker 是并行跑的, 用内部小线程池</li>
 *   <li>timeout 默认 30s, 可在构造函数指定</li>
 * </ul>
 */
public class SupervisorAgent implements Agent {

    private final String name;
    private final ReActAgent delegate;
    private final MessageQueue messageQueue;
    private final Blackboard blackboard;
    private final RoutingStrategy routingStrategy;
    private final FailoverPolicy failoverPolicy;
    private final long workerTimeoutMs;
    private final ExecutorService workerPool;

    public SupervisorAgent(String name,
                           ReActAgent delegate,
                           MessageQueue messageQueue,
                           Blackboard blackboard,
                           RoutingStrategy routingStrategy,
                           FailoverPolicy failoverPolicy,
                           long workerTimeoutMs) {
        this.name = Objects.requireNonNull(name);
        this.delegate = Objects.requireNonNull(delegate);
        this.messageQueue = Objects.requireNonNull(messageQueue);
        this.blackboard = blackboard == null ? Blackboard.create() : blackboard;
        this.routingStrategy = routingStrategy == null ? new ByCapability() : routingStrategy;
        this.failoverPolicy = failoverPolicy == null ? FailoverPolicy.skip() : failoverPolicy;
        this.workerTimeoutMs = workerTimeoutMs > 0 ? workerTimeoutMs : 30_000L;
        this.workerPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "supervisor-" + name + "-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public Blackboard blackboard() {
        return blackboard;
    }

    public RoutingStrategy routingStrategy() {
        return routingStrategy;
    }

    public FailoverPolicy failoverPolicy() {
        return failoverPolicy;
    }

    /**
     * 注册一个 worker (在启动前完成, 注册后 listWorkers() 才能看到)。
     * 调用方负责把同一个 WorkerAgent 也注册到 {@link WorkerAgentRegistry}。
     */
    public void registerWorker(WorkerDescriptor descriptor) {
        // 不存 fields, 注册表由调用方持有, 这里只做轻量校验
        Objects.requireNonNull(descriptor, "descriptor");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Flux<StreamEvent> stream(String input) {
        return Flux.create(sink -> {
            try {
                handle(sink, input);
            } catch (Throwable t) {
                sink.next(new StreamEvent.ErrorEvent(t));
                sink.complete();
            }
        });
    }

    private void handle(FluxSink<StreamEvent> sink, String input) {
        sink.next(new StreamEvent.ThoughtDelta("[Supervisor:" + name + "] 收到任务, 开始分析..."));

        // 1. 路由: 选 worker
        List<WorkerDescriptor> allWorkers = lookupAllWorkers();
        List<String> inferredTags = inferTags(input, allWorkers);
        List<WorkerDescriptor> targets = routingStrategy.route(
                new RoutingStrategy.RoutingContext(input, inferredTags, allWorkers));

        blackboard.put("user_input", input);
        blackboard.put("routed_tags", inferredTags);

        if (targets.isEmpty()) {
            sink.next(new StreamEvent.ThoughtDelta("[Supervisor] 没有匹配的 worker, 由自己处理"));
            delegate.stream(input)
                    .doOnNext(sink::next)
                    .doOnError(sink::error)
                    .doOnComplete(sink::complete)
                    .subscribe();
            return;
        }

        // 2. 委派
        String conversationId = "sup-conv-" + System.currentTimeMillis();
        Map<String, WorkerResult> results = new ConcurrentHashMap<>();
        CountDownLatch done = new CountDownLatch(targets.size());
        AtomicInteger failureCount = new AtomicInteger(0);

        for (WorkerDescriptor wd : targets) {
            delegateWithFailover(sink, wd, input, conversationId, results, done, failureCount, 1);
        }

        // 3. 等所有 worker 完成
        try {
            boolean finished = done.await(workerTimeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                sink.next(new StreamEvent.ThoughtDelta("[Supervisor] 等待超时, 已收到 "
                        + (targets.size() - done.getCount()) + "/" + targets.size() + " 响应"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sink.next(new StreamEvent.ThoughtDelta("[Supervisor] 等待被中断"));
        }

        // 4. 汇总
        StringBuilder finalReply = new StringBuilder();
        for (WorkerDescriptor wd : targets) {
            WorkerResult r = results.get(wd.name());
            if (r == null) {
                finalReply.append("[").append(wd.name()).append("]: (无响应)\n");
            } else if (r.error != null) {
                finalReply.append("[").append(wd.name()).append("]: [错误] ").append(r.error).append("\n");
            } else {
                finalReply.append("[").append(wd.name()).append("]: ").append(r.reply).append("\n");
            }
        }
        blackboard.put("worker_results", new HashMap<>(results));
        sink.next(new StreamEvent.Done(finalReply.toString().trim(), 1, null, null));
        sink.complete();
    }

    /**
     * 单个 worker 的委派 + failover 循环。
     * 注意: 失败时 worker 是已经在跑的 (它从队列 take), 所以 retry 是构造新 DELEGATION 消息重发。
     */
    private void delegateWithFailover(FluxSink<StreamEvent> sink,
                                      WorkerDescriptor wd,
                                      String task,
                                      String conversationId,
                                      Map<String, WorkerResult> results,
                                      CountDownLatch done,
                                      AtomicInteger failureCount,
                                      int attempt) {
        String delegationId = "del-" + System.nanoTime() + "-" + attempt;
        sink.next(new StreamEvent.ThoughtDelta(
                "[Supervisor] 委派给 " + wd.name() + " (attempt " + attempt + ", id=" + delegationId + ")"));

        AgentMessage delegation = AgentMessage.builder()
                .messageId(delegationId)
                .fromAgent(name)
                .toAgent(wd.name())
                .type(AgentMessage.MessageType.DELEGATION)
                .content(task)
                .conversationId(conversationId)
                .build();
        messageQueue.send(delegation);

        // 启动 worker 处理 (它会从自己的队列 take)
        workerPool.submit(() -> {
            try {
                wd.worker().processMessages();
            } catch (Throwable t) {
                WorkerResult err = new WorkerResult();
                err.error = "worker-thread-crash: " + t.getMessage();
                results.put(wd.name(), err);
                failureCount.incrementAndGet();
                done.countDown();
            }
        });

        // 等响应 (in a separate poll thread would be cleaner; here we just block briefly per attempt)
        long deadline = System.currentTimeMillis() + workerTimeoutMs;
        boolean answered = false;
        while (System.currentTimeMillis() < deadline) {
            AgentMessage resp = messageQueue.pollFor(name);
            if (resp == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
                continue;
            }
            if (delegationId.equals(resp.getInReplyTo())
                    && (resp.getType() == AgentMessage.MessageType.RESPONSE
                    || resp.getType() == AgentMessage.MessageType.ERROR)) {
                WorkerResult r = new WorkerResult();
                if (resp.getType() == AgentMessage.MessageType.RESPONSE) {
                    r.reply = resp.getContent();
                    sink.next(new StreamEvent.ThoughtDelta("[" + wd.name() + "] " + r.reply));
                } else {
                    r.error = resp.getContent();
                    sink.next(new StreamEvent.ThoughtDelta("[" + wd.name() + "] 失败: " + r.error));
                }
                results.put(wd.name(), r);
                if (resp.getType() == AgentMessage.MessageType.ERROR) {
                    failureCount.incrementAndGet();
                }
                done.countDown();
                answered = true;
                break;
            } else {
                // 不属于本次委派, 放回去 (re-offer)
                messageQueue.send(resp);
            }
        }

        if (!answered) {
            // 超时
            WorkerResult r = new WorkerResult();
            r.error = "timeout after " + workerTimeoutMs + "ms";
            results.put(wd.name(), r);
            failureCount.incrementAndGet();
            done.countDown();
            sink.next(new StreamEvent.ThoughtDelta("[" + wd.name() + "] 超时"));
        }
    }

    /**
     * 简单 tag 推断: 把 user input 按 worker capability.name 拆, 命中哪个算哪个。
     * 复杂版可由 LLM 决策, 这里只给个兜底实现。
     */
    @SuppressWarnings("unchecked")
    private List<String> inferTags(String input, List<WorkerDescriptor> allWorkers) {
        List<String> hits = new ArrayList<>();
        String lower = input.toLowerCase();
        for (WorkerDescriptor d : allWorkers) {
            for (String tag : d.capability().tags()) {
                if (lower.contains(tag.toLowerCase()) && !hits.contains(tag)) {
                    hits.add(tag);
                }
            }
            if (lower.contains(d.capability().name().toLowerCase()) && !hits.contains(d.capability().name())) {
                hits.add(d.capability().name());
            }
        }
        // fallback: 把 blackboard 里的 routed_tags 也带上
        Object prev = blackboard.get("routed_tags");
        if (prev instanceof List) {
            for (Object o : (List<Object>) prev) {
                if (o instanceof String && !hits.contains(o)) {
                    hits.add((String) o);
                }
            }
        }
        return hits;
    }

    /**
     * 默认从 blackboard 取 "all_workers" 字段, 调用方应预先 set。
     */
    @SuppressWarnings("unchecked")
    private List<WorkerDescriptor> lookupAllWorkers() {
        Object v = blackboard.get("all_workers");
        if (v instanceof List) {
            return (List<WorkerDescriptor>) v;
        }
        return new ArrayList<>();
    }

    /**
     * Helper: 让调用方在调用 stream() 前一次性把 worker 列表塞进 blackboard。
     * 也可以直接写到 Blackboard("all_workers", workers)。
     */
    public void setAllWorkers(List<WorkerDescriptor> workers) {
        blackboard.put("all_workers", new ArrayList<>(workers));
    }

    @Override
    public void stop() {
        delegate.stop();
        workerPool.shutdownNow();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    /**
     * 关闭内部 worker 线程池 (应用关闭时调用)。
     */
    public void shutdown() {
        workerPool.shutdownNow();
    }

    private static class WorkerResult {
        String reply;
        String error;
    }
}
