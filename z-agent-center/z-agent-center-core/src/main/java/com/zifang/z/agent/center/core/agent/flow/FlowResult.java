package com.zifang.z.agent.center.core.agent.flow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程执行结果
 */
public class FlowResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String executionId;
    private boolean success;
    private Map<String, Object> outputs = new HashMap<>();
    private FlowContext context;
    private int nodeCount;
    private int completedCount;
    private List<ErrorInfo> errors = new ArrayList<>();
    private long startTime;
    private long endTime;
    private long elapsedMs;

    public FlowResult() {
        this.startTime = System.currentTimeMillis();
    }

    public static FlowResult timeout(String executionId) {
        FlowResult result = new FlowResult();
        result.executionId = executionId;
        result.success = false;
        result.errors.add(new ErrorInfo("SYSTEM", "Flow execution timeout"));
        result.endTime = System.currentTimeMillis();
        result.elapsedMs = result.endTime - result.startTime;
        return result;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, Object> getOutputs() {
        return outputs;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
    }

    public FlowContext getContext() {
        return context;
    }

    public void setContext(FlowContext context) {
        this.context = context;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public List<ErrorInfo> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorInfo> errors) {
        this.errors = errors;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    /**
     * 错误信息
     */
    public static class ErrorInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String nodeId;
        private String message;
        private long timestamp;

        public ErrorInfo() {
        }

        public ErrorInfo(String nodeId, String message) {
            this.nodeId = nodeId;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}