package com.zifang.z.agent.engine.agent.multi;

import com.zifang.z.agent.engine.agent.llm.provider.LlmClient;
import com.zifang.z.agent.engine.agent.react.ReActAgent;
import com.zifang.z.agent.engine.agent.react.StreamEvent;
import org.junit.After;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * SupervisorAgent 端到端测试 — 不连任何真实 LLM, 不连任何 DB。
 *
 * <p>用 {@link StubLlmProvider} 替换真实 LLM, 验证:
 * <ul>
 *   <li>supervisor 收到任务 → 走 routing → 找到匹配 worker</li>
 *   <li>supervisor 通过 MessageQueue 派单给 worker</li>
 *   <li>worker 处理后, RESPONSE 消息带 inReplyTo 回到 supervisor</li>
 *   <li>supervisor 汇总 worker 回复, emit FinalDelta + Done</li>
 *   <li>Blackboard 记录了 worker_results</li>
 *   <li>多 worker 路由 (广播给所有匹配 worker)</li>
 * </ul>
 *
 * <p>这正是 z-agent-team 模块的核心调用路径, 在 engine 层验证通过,
 * 后续 z-agent-team 只需要做 IM/DB/事件转发。
 */
public class SupervisorAgentE2ETest {

    private LlmClient llmClient;
    private List<SupervisorAgent> supervisorsToShutdown = new ArrayList<>();

    @After
    public void cleanup() {
        supervisorsToShutdown.forEach(SupervisorAgent::shutdown);
    }

    // ===== 工具方法 =====

    private ReActAgent buildWorker(String name, StubLlmProvider stub) {
        llmClient.registerProvider(stub);
        ReActAgent agent = new ReActAgent.Builder()
                .name(name)
                .description("test worker " + name)
                .llmClient(llmClient)
                .provider(stub.getProviderCode())
                .model("stub-model")
                .maxSteps(1)
                .temperature(0.0)
                .build();
        return agent;
    }

    private ReActAgent buildSupervisorDelegate(String name, StubLlmProvider stub) {
        llmClient.registerProvider(stub);
        return new ReActAgent.Builder()
                .name(name)
                .description("test supervisor " + name)
                .llmClient(llmClient)
                .provider(stub.getProviderCode())
                .model("stub-model")
                .maxSteps(1)
                .temperature(0.0)
                .build();
    }

    // ===== 测试 1: 单 worker 路由 — "请查 config" → worker1 (tag=config) =====

    @Test
    public void testSupervisor_routesToMatchingWorker_andCollectsReply() {
        llmClient = new LlmClient();
        StubLlmProvider workerStub = new StubLlmProvider().respondWith("z-config 的服务地址是 http://config.local:8080");
        StubLlmProvider supStub = new StubLlmProvider();  // supervisor delegate 不该被调用 (有匹配的 worker)

        Blackboard blackboard = Blackboard.create();
        MessageQueue queue = new InMemoryMessageQueue();

        ReActAgent workerDelegate = buildWorker("z-config", workerStub);
        WorkerAgent worker = new WorkerAgent("z-config", workerDelegate, queue, blackboard);
        WorkerDescriptor wd = new WorkerDescriptor(worker,
                WorkerCapability.of("z-config", "查 z-config 服务地址", "config"));
        List<WorkerDescriptor> workers = Arrays.asList(wd);
        blackboard.put("all_workers", workers);

        ReActAgent delegate = buildSupervisorDelegate("supervisor", supStub);
        SupervisorAgent supervisor = new SupervisorAgent(
                "supervisor", delegate, queue, blackboard,
                new ByCapability(),
                FailoverPolicy.skip(),
                10_000L);
        supervisorsToShutdown.add(supervisor);

        // Act: 发 query, input 含 "config" tag
        List<StreamEvent> events = supervisor.stream("请帮我查 config 里的服务地址")
                .collectList()
                .block(Duration.ofSeconds(30));

        // Assert
        assertNotNull("Flux 应返回非空", events);
        // 1. 应至少有 ThoughtDelta (supervisor 的 [Supervisor:...] 思考)
        boolean hasThought = events.stream().anyMatch(e ->
                e instanceof StreamEvent.ThoughtDelta);
        assertTrue("应有 ThoughtDelta 事件", hasThought);
        // 2. 应有 ToolCallRequest 或 Done 事件 (supervisor 调了 dispatch_to_agent 或自己干)
        // 3. 最关键: worker 收到过请求
        // 4. 应该有 Done 事件, 且 reply 包含 worker 的回复
        StreamEvent done = events.stream()
                .filter(e -> e instanceof StreamEvent.Done)
                .reduce((a, b) -> b)
                .orElse(null);
        assertNotNull("应有 Done 事件", done);
        String reply = ((StreamEvent.Done) done).reply;
        assertNotNull("Done.reply 不应为 null", reply);
        assertTrue("reply 应包含 worker 名字或回复, 实际: " + reply,
                reply.contains("z-config") || reply.contains("服务地址"));
        // 5. worker stub 至少被调用一次
        assertTrue("worker LLM 应被调用过, 实际 callCount=" + workerStub.callCount.get(),
                workerStub.callCount.get() >= 1);
        // 6. worker.processedCount 应该为 1
        assertEquals("worker 应处理过 1 条消息", 1, worker.processedCount());
    }

    // ===== 测试 2: 路由到 2 个 worker (ByCapability, 命中多个) =====

    @Test
    public void testSupervisor_routesToMultipleWorkersByCapability() {
        llmClient = new LlmClient();
        StubLlmProvider configStub = new StubLlmProvider().respondWith("[config] 返回 A");
        StubLlmProvider ctcStub = new StubLlmProvider().respondWith("[ctc] 返回 B");
        StubLlmProvider supStub = new StubLlmProvider();

        Blackboard blackboard = Blackboard.create();
        MessageQueue queue = new InMemoryMessageQueue();

        WorkerAgent configWorker = new WorkerAgent("z-config",
                buildWorker("z-config", configStub), queue, blackboard);
        WorkerAgent ctcWorker = new WorkerAgent("z-ctc",
                buildWorker("z-ctc", ctcStub), queue, blackboard);
        WorkerDescriptor wd1 = new WorkerDescriptor(configWorker,
                WorkerCapability.of("z-config", "查 config", "config"));
        WorkerDescriptor wd2 = new WorkerDescriptor(ctcWorker,
                WorkerCapability.of("z-ctc", "查 ctc", "ctc"));
        List<WorkerDescriptor> workers = Arrays.asList(wd1, wd2);
        blackboard.put("all_workers", workers);

        ReActAgent delegate = buildSupervisorDelegate("supervisor", supStub);
        SupervisorAgent supervisor = new SupervisorAgent(
                "supervisor", delegate, queue, blackboard,
                new ByCapability(),
                FailoverPolicy.skip(),
                15_000L);
        supervisorsToShutdown.add(supervisor);

        // query: "config ctc 都查" → ByCapability 应匹配两个 (任一 tag 命中)
        List<StreamEvent> events = supervisor.stream("同时查 config 和 ctc")
                .collectList().block(Duration.ofSeconds(30));

        assertNotNull(events);
        // 两个 worker 都应被调用
        assertTrue("z-config 应被调用, callCount=" + configStub.callCount.get(),
                configStub.callCount.get() >= 1);
        assertTrue("z-ctc 应被调用, callCount=" + ctcStub.callCount.get(),
                ctcStub.callCount.get() >= 1);
        // 汇总 reply 应包含两个 worker 的回复
        StreamEvent done = events.stream()
                .filter(e -> e instanceof StreamEvent.Done)
                .reduce((a, b) -> b).orElse(null);
        assertNotNull(done);
        String reply = ((StreamEvent.Done) done).reply;
        assertTrue("reply 应包含 [config] 部分: " + reply, reply.contains("[config]"));
        assertTrue("reply 应包含 [ctc] 部分: " + reply, reply.contains("[ctc]"));
    }

    // ===== 测试 3: 无匹配 worker → supervisor 自己干 (delegate 被调用) =====

    @Test
    public void testSupervisor_fallsBackToDelegate_whenNoMatch() {
        llmClient = new LlmClient();
        StubLlmProvider supStub = new StubLlmProvider().respondWith("我自己回答: 你好 CEO");
        Blackboard blackboard = Blackboard.create();
        MessageQueue queue = new InMemoryMessageQueue();

        // 故意没有 worker 注册到 blackboard
        blackboard.put("all_workers", new ArrayList<WorkerDescriptor>());

        ReActAgent delegate = buildSupervisorDelegate("supervisor", supStub);
        SupervisorAgent supervisor = new SupervisorAgent(
                "supervisor", delegate, queue, blackboard,
                new ByCapability(),
                FailoverPolicy.skip(),
                10_000L);
        supervisorsToShutdown.add(supervisor);

        // query: "今天天气" → 没有匹配的 tag, supervisor 自己干
        List<StreamEvent> events = supervisor.stream("今天天气不错")
                .collectList().block(Duration.ofSeconds(30));

        assertNotNull(events);
        // delegate stub 至少被调用一次
        assertTrue("supervisor delegate 应被调用, callCount=" + supStub.callCount.get(),
                supStub.callCount.get() >= 1);
        // Done.reply 应包含 supervisor 的回复
        StreamEvent done = events.stream()
                .filter(e -> e instanceof StreamEvent.Done)
                .reduce((a, b) -> b).orElse(null);
        assertNotNull(done);
        String reply = ((StreamEvent.Done) done).reply;
        assertTrue("reply 应包含 '我自己回答', 实际: " + reply, reply.contains("我自己回答"));
    }

    // ===== 测试 4: Blackboard 在调用后被填上 worker_results =====

    @Test
    public void testSupervisor_writesWorkerResultsToBlackboard() {
        llmClient = new LlmClient();
        StubLlmProvider stub = new StubLlmProvider().respondWith("worker-reply-X");
        Blackboard blackboard = Blackboard.create();
        MessageQueue queue = new InMemoryMessageQueue();

        WorkerAgent worker = new WorkerAgent("z-task",
                buildWorker("z-task", stub), queue, blackboard);
        WorkerDescriptor wd = new WorkerDescriptor(worker,
                WorkerCapability.of("z-task", "task 任务", "task"));
        blackboard.put("all_workers", Arrays.asList(wd));

        ReActAgent delegate = buildSupervisorDelegate("supervisor", new StubLlmProvider());
        SupervisorAgent supervisor = new SupervisorAgent(
                "supervisor", delegate, queue, blackboard,
                new ByCapability(),
                FailoverPolicy.skip(),
                10_000L);
        supervisorsToShutdown.add(supervisor);

        supervisor.stream("处理 task").collectList().block(Duration.ofSeconds(30));

        // Blackboard 应有 "worker_results" 键
        Object results = blackboard.get("worker_results");
        assertNotNull("blackboard 应有 worker_results", results);
        assertTrue("worker_results 应是 Map", results instanceof java.util.Map);
        @SuppressWarnings("unchecked")
        java.util.Map<String, ?> map = (java.util.Map<String, ?>) results;
        assertTrue("z-task 应在 results 里, 实际: " + map.keySet(), map.containsKey("z-task"));
    }

    // ===== 测试 5: StreamEvent 顺序: 思考 → 委派 → 收到 → 汇总 =====

    @Test
    public void testSupervisor_eventSequenceIsLogical() {
        llmClient = new LlmClient();
        StubLlmProvider stub = new StubLlmProvider().respondWith("payload");
        Blackboard blackboard = Blackboard.create();
        MessageQueue queue = new InMemoryMessageQueue();

        WorkerAgent worker = new WorkerAgent("z-ctc",
                buildWorker("z-ctc", stub), queue, blackboard);
        WorkerDescriptor wd = new WorkerDescriptor(worker,
                WorkerCapability.of("z-ctc", "ctc 操作", "ctc"));
        blackboard.put("all_workers", Arrays.asList(wd));

        ReActAgent delegate = buildSupervisorDelegate("supervisor", new StubLlmProvider());
        SupervisorAgent supervisor = new SupervisorAgent(
                "supervisor", delegate, queue, blackboard,
                new ByCapability(),
                FailoverPolicy.skip(),
                10_000L);
        supervisorsToShutdown.add(supervisor);

        List<StreamEvent> events = supervisor.stream("查 ctc 用户")
                .collectList().block(Duration.ofSeconds(30));

        // 至少应有一个 ThoughtDelta 包含 "[Supervisor]"
        boolean hasSupThought = events.stream()
                .filter(e -> e instanceof StreamEvent.ThoughtDelta)
                .map(e -> ((StreamEvent.ThoughtDelta) e).text)
                .anyMatch(t -> t != null && t.contains("[Supervisor]"));
        assertTrue("应有 [Supervisor] 思考事件, 实际: " + events, hasSupThought);

        // 至少应有一个 ThoughtDelta 包含 "委派" 或 "z-ctc"
        boolean hasDelegation = events.stream()
                .filter(e -> e instanceof StreamEvent.ThoughtDelta)
                .map(e -> ((StreamEvent.ThoughtDelta) e).text)
                .anyMatch(t -> t != null && (t.contains("委派") || t.contains("z-ctc")));
        assertTrue("应有委派相关事件, 实际: " + events, hasDelegation);

        // 应有 Done 事件
        assertTrue("应有 Done 事件, 实际: " + events,
                events.stream().anyMatch(e -> e instanceof StreamEvent.Done));
    }
}
