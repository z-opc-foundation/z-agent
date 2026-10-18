package com.zifang.z.agent.engine.agent.multi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Worker 集合的能力注册中心。
 *
 * <p>Supervisor 启动时从 registry 拉所有 worker, 然后交给 {@link RoutingStrategy} 选。
 * 增删 worker 是线程安全的, 注册时可附带 {@link WorkerCapability}。
 *
 * <p>典型用法:
 * <pre>{@code
 *   WorkerAgentRegistry registry = new WorkerAgentRegistry();
 *   registry.register(myWorker, WorkerCapability.of("z-config", "config queries", "config"));
 *   List<WorkerDescriptor> all = registry.listAll();
 * }</pre>
 */
public class WorkerAgentRegistry {

    private final ConcurrentMap<String, WorkerDescriptor> workers = new ConcurrentHashMap<>();

    public void register(WorkerAgent worker, WorkerCapability capability) {
        workers.put(worker.name(), new WorkerDescriptor(worker, capability));
    }

    public void register(WorkerDescriptor descriptor) {
        workers.put(descriptor.name(), descriptor);
    }

    public boolean unregister(String workerName) {
        return workers.remove(workerName) != null;
    }

    public WorkerDescriptor get(String workerName) {
        return workers.get(workerName);
    }

    public List<WorkerDescriptor> listAll() {
        return new ArrayList<>(workers.values());
    }

    public int size() {
        return workers.size();
    }

    public void clear() {
        workers.clear();
    }

    /**
     * 返回所有 worker 的 capability 渲染文本, 用于 supervisor 的 system prompt。
     */
    public String renderCapabilityList() {
        if (workers.isEmpty()) {
            return "(no workers registered)";
        }
        return workers.values().stream()
                .map(d -> d.capability().render())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 按 tag 查找 worker (精确匹配 tag 集合中的任一)。
     */
    public List<WorkerDescriptor> findByTag(String tag) {
        return workers.values().stream()
                .filter(d -> d.capability().tags().contains(tag))
                .collect(Collectors.toList());
    }

    public boolean contains(String workerName) {
        return workers.containsKey(workerName);
    }

    public Collection<WorkerDescriptor> descriptors() {
        return workers.values();
    }

    public Map<String, WorkerDescriptor> snapshot() {
        return new ConcurrentHashMap<>(workers);
    }
}
