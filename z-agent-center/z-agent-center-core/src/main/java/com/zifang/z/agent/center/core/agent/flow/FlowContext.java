package com.zifang.z.agent.center.core.agent.flow;

import java.io.Serializable;
import java.util.*;

/**
 * 流程上下文 - 在节点间传递状态和数据
 */
public class FlowContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 根流程ID
     */
    private String rootFlowId;

    /**
     * 当前节点ID
     */
    private String currentNodeId;

    /**
     * 上一个节点ID
     */
    private String previousNodeId;

    /**
     * 全局变量
     */
    private Map<String, Object> variables = new HashMap<>();

    /**
     * 消息历史
     */
    private List<ContextMessage> messages = new ArrayList<>();

    /**
     * 节点输出缓存
     */
    private Map<String, Object> nodeOutputs = new HashMap<>();

    /**
     * 流程元数据
     */
    private Map<String, String> metadata = new HashMap<>();

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 开始时间
     */
    private long startTime;

    public FlowContext() {
        this.startTime = System.currentTimeMillis();
    }

    public FlowContext(String userId, String userName) {
        this();
        this.userId = userId;
        this.userName = userName;
    }

    // ==================== 变量操作 ====================

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public Object getVariable(String key, Object defaultValue) {
        return variables.getOrDefault(key, defaultValue);
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    // ==================== 消息操作 ====================

    public void addMessage(String role, String content) {
        messages.add(new ContextMessage(role, content, System.currentTimeMillis()));
    }

    public void addMessage(String role, String content, String nodeId) {
        messages.add(new ContextMessage(role, content, System.currentTimeMillis(), nodeId));
    }

    public List<ContextMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public List<ContextMessage> getMessagesSince(String nodeId) {
        List<ContextMessage> result = new ArrayList<>();
        boolean found = false;
        for (ContextMessage msg : messages) {
            if (msg.getNodeId() != null && msg.getNodeId().equals(nodeId)) {
                found = true;
            }
            if (found) {
                result.add(msg);
            }
        }
        return result;
    }

    // ==================== 节点输出 ====================

    public void setNodeOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }

    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }

    public Map<String, Object> getNodeOutputs() {
        return Collections.unmodifiableMap(nodeOutputs);
    }

    // ==================== 上下文传递 ====================

    /**
     * 创建子上下文（用于子流程/子Agent）
     */
    public FlowContext createSubContext(String subFlowId, String nodeId) {
        FlowContext sub = new FlowContext(this.userId, this.userName);
        sub.rootFlowId = this.rootFlowId != null ? this.rootFlowId : subFlowId;
        sub.variables = new HashMap<>(this.variables); // 继承变量
        sub.messages = new ArrayList<>(this.messages); // 继承消息历史
        sub.currentNodeId = nodeId;
        return sub;
    }

    /**
     * 合并子上下文结果
     */
    public void mergeSubContext(FlowContext sub) {
        if (sub == null) {
            return;
        }
        this.variables.putAll(sub.variables);
        this.messages.addAll(sub.messages);
        this.nodeOutputs.putAll(sub.nodeOutputs);
    }

    // ==================== Getters/Setters ====================

    public String getRootFlowId() {
        return rootFlowId;
    }

    public void setRootFlowId(String rootFlowId) {
        this.rootFlowId = rootFlowId;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public String getPreviousNodeId() {
        return previousNodeId;
    }

    public void setPreviousNodeId(String previousNodeId) {
        this.previousNodeId = previousNodeId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getElapsedMs() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 消息记录
     */
    public static class ContextMessage implements Serializable {
        private static final long serialVersionUID = 1L;

        private String role;      // system/user/assistant/tool
        private String content;
        private long timestamp;
        private String nodeId;    // 产生这条消息的节点

        public ContextMessage() {
        }

        public ContextMessage(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public ContextMessage(String role, String content, long timestamp, String nodeId) {
            this(role, content, timestamp);
            this.nodeId = nodeId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
    }
}