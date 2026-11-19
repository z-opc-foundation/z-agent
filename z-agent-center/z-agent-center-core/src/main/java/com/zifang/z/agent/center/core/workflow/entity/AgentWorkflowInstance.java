package com.zifang.z.agent.center.core.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

@BizModel(name = "z_agent_workflow_instance", module = "z-agent", description = "z_agent_workflow_instance")
@TableName("z_agent_workflow_instance")
public class AgentWorkflowInstance {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "流程实例ID", type = BizField.FieldType.STRING)
    private String processId;
    @BizField(name = "所属应用编码", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "状态: RUNNING/COMPLETED/FAILED/PAUSED", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "工作流配置JSON", type = BizField.FieldType.STRING)
    private String workflowConfig;
    @BizField(name = "执行快照JSON", type = BizField.FieldType.STRING)
    private String snapshotData;
    @BizField(name = "当前执行节点", type = BizField.FieldType.STRING)
    private String currentNodeId;
    @BizField(name = "错误信息", type = BizField.FieldType.STRING)
    private String errorMessage;
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdTime;
    @BizField(name = "更新时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedTime;
    @BizField(name = "逻辑删除", type = BizField.FieldType.INTEGER)
    private Integer isDeleted = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWorkflowConfig() {
        return workflowConfig;
    }

    public void setWorkflowConfig(String workflowConfig) {
        this.workflowConfig = workflowConfig;
    }

    public String getSnapshotData() {
        return snapshotData;
    }

    public void setSnapshotData(String snapshotData) {
        this.snapshotData = snapshotData;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}



