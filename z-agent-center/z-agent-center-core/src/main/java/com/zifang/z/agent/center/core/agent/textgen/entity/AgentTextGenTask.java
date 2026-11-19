package com.zifang.z.agent.center.core.agent.textgen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * FEATURE013 T3.4: Agent 文本生成器 任务实体
 */
@BizModel(name = "FEATURE013 T3.4: Agent 文本生成器 任务实体", module = "z-agent", description = "FEATURE013 T3.4: Agent 文本生成器 任务实体")
@TableName("z_agent_text_gen_task")
public class AgentTextGenTask {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "taskCode", type = BizField.FieldType.STRING)
    private String taskCode;
    @BizField(name = "taskName", type = BizField.FieldType.STRING)
    private String taskName;
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "instanceCode", type = BizField.FieldType.STRING)
    private String instanceCode;
    @BizField(name = "promptTemplate", type = BizField.FieldType.STRING)
    private String promptTemplate;
    @BizField(name = "inputData", type = BizField.FieldType.STRING)
    private String inputData;
    @BizField(name = "variables", type = BizField.FieldType.STRING)
    private String variables;
    @BizField(name = "outputFormat", type = BizField.FieldType.STRING)
    private String outputFormat;
    @BizField(name = "status", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "totalCount", type = BizField.FieldType.INTEGER)
    private Integer totalCount;
    @BizField(name = "successCount", type = BizField.FieldType.INTEGER)
    private Integer successCount;
    @BizField(name = "failedCount", type = BizField.FieldType.INTEGER)
    private Integer failedCount;
    @BizField(name = "startedAt", type = BizField.FieldType.DATETIME)
    private LocalDateTime startedAt;
    @BizField(name = "finishedAt", type = BizField.FieldType.DATETIME)
    private LocalDateTime finishedAt;
    @BizField(name = "elapsedMs", type = BizField.FieldType.INTEGER)
    private Long elapsedMs;
    @BizField(name = "outputCsv", type = BizField.FieldType.STRING)
    private String outputCsv;
    @BizField(name = "errorMessage", type = BizField.FieldType.STRING)
    private String errorMessage;
    @BizField(name = "ownerId", type = BizField.FieldType.STRING)
    private String ownerId;
    @BizField(name = "ownerName", type = BizField.FieldType.STRING)
    private String ownerName;
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "gmtModified", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;
    @BizField(name = "isDeleted", type = BizField.FieldType.INTEGER)
    private Integer isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getOutputCsv() {
        return outputCsv;
    }

    public void setOutputCsv(String outputCsv) {
        this.outputCsv = outputCsv;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}



