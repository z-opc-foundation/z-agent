package com.zifang.z.agent.center.core.agent.flow;

import java.io.Serializable;
import java.util.*;

/**
 * 流程定义 - 描述一个有向无环图(DAG)
 */
public class FlowDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程ID
     */
    private String flowId;

    /**
     * 流程名称
     */
    private String name;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 节点定义 Map<nodeId, FlowNode>
     */
    private Map<String, FlowNode> nodes = new LinkedHashMap<>();

    /**
     * 有向边 Map<sourceNodeId, List<targetNodeId>>
     */
    private Map<String, List<String>> edges = new HashMap<>();

    /**
     * 超时时间(ms) 默认30分钟
     */
    private long timeoutMs = 30 * 60 * 1000;

    /**
     * 重试策略
     */
    private RetryPolicy retryPolicy;

    /**
     * 流程变量(静态配置)
     */
    private Map<String, Object> variables = new HashMap<>();

    public FlowDefinition() {
    }

    public FlowDefinition(String flowId, String name) {
        this.flowId = flowId;
        this.name = name;
    }

    // ==================== 节点操作 ====================

    public void addNode(String nodeId, FlowNode node) {
        nodes.put(nodeId, node);
    }

    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
        // 移除相关边
        edges.remove(nodeId);
        edges.values().forEach(list -> list.remove(nodeId));
    }

    public FlowNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Set<String> getNodeIds() {
        return nodes.keySet();
    }

    public Collection<FlowNode> getNodes() {
        return nodes.values();
    }

    public int getNodeCount() {
        return nodes.size();
    }

    // ==================== 边操作 ====================

    public void addEdge(String fromNodeId, String toNodeId) {
        edges.computeIfAbsent(fromNodeId, k -> new ArrayList<>()).add(toNodeId);
    }

    public void removeEdge(String fromNodeId, String toNodeId) {
        List<String> list = edges.get(fromNodeId);
        if (list != null) {
            list.remove(toNodeId);
        }
    }

    public List<String> getNextNodes(String nodeId) {
        return edges.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<String> getPrevNodes(String nodeId) {
        List<String> prev = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : edges.entrySet()) {
            if (entry.getValue().contains(nodeId)) {
                prev.add(entry.getKey());
            }
        }
        return prev;
    }

    public Map<String, List<String>> getEdges() {
        return edges;
    }

    // ==================== 验证 ====================

    public boolean isDAG() {
        if (nodes.isEmpty()) {
            return true;
        }

        Set<String> visited = new HashSet<>();
        Set<String> recursion = new HashSet<>();

        for (String nodeId : nodes.keySet()) {
            if (hasCycle(nodeId, visited, recursion)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCycle(String nodeId, Set<String> visited, Set<String> recursion) {
        if (recursion.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursion.add(nodeId);

        for (String next : getNextNodes(nodeId)) {
            if (hasCycle(next, visited, recursion)) {
                return true;
            }
        }

        recursion.remove(nodeId);
        return false;
    }

    // ==================== Getters/Setters ====================

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    /**
     * 重试策略
     */
    public static class RetryPolicy implements Serializable {
        private static final long serialVersionUID = 1L;

        private int maxAttempts = 3;
        private long initialDelayMs = 1000;
        private double backoffMultiplier = 2.0;
        private List<Class<? extends Exception>> retryableExceptions;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }

        public List<Class<? extends Exception>> getRetryableExceptions() {
            return retryableExceptions;
        }

        public void setRetryableExceptions(List<Class<? extends Exception>> retryableExceptions) {
            this.retryableExceptions = retryableExceptions;
        }
    }
}