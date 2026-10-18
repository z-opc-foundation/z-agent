package com.zifang.z.agent.center.core.agent.textgen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * FEATURE013 T3.4: Agent 文本生成器 每行结果实体
 */
@TableName("z_agent_text_gen_result")
public class AgentTextGenResult {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer rowIndex;
    private String inputJson;
    private String outputText;
    private String status;
    private String errorMessage;
    private Integer tokenCount;
    private Long elapsedMs;
    private java.time.LocalDateTime gmtCreate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public String getInputJson() {
        return inputJson;
    }

    public void setInputJson(String inputJson) {
        this.inputJson = inputJson;
    }

    public String getOutputText() {
        return outputText;
    }

    public void setOutputText(String outputText) {
        this.outputText = outputText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public java.time.LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(java.time.LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
}
