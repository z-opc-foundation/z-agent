package com.zifang.z.agent.center.core.agent.trigger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * FEATURE013 T3.1: Agent 定时触发实体
 */
@BizModel(name = "FEATURE013 T3.1: Agent 定时触发实体", module = "z-agent", description = "FEATURE013 T3.1: Agent 定时触发实体")
@TableName("z_agent_trigger")
public class AgentTrigger {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "triggerCode", type = BizField.FieldType.STRING)
    private String triggerCode;
    @BizField(name = "triggerName", type = BizField.FieldType.STRING)
    private String triggerName;
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "instanceCode", type = BizField.FieldType.STRING)
    private String instanceCode;
    @BizField(name = "cron", type = BizField.FieldType.STRING)
    private String cron;
    @BizField(name = "message", type = BizField.FieldType.STRING)
    private String message;
    @BizField(name = "status", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "nextFireTime", type = BizField.FieldType.DATETIME)
    private LocalDateTime nextFireTime;
    @BizField(name = "lastFireTime", type = BizField.FieldType.DATETIME)
    private LocalDateTime lastFireTime;
    @BizField(name = "lastFireStatus", type = BizField.FieldType.STRING)
    private String lastFireStatus;
    @BizField(name = "lastFireMessage", type = BizField.FieldType.STRING)
    private String lastFireMessage;
    @BizField(name = "fireCount", type = BizField.FieldType.INTEGER)
    private Integer fireCount;
    @BizField(name = "ownerId", type = BizField.FieldType.STRING)
    private String ownerId;
    @BizField(name = "ownerName", type = BizField.FieldType.STRING)
    private String ownerName;
    @BizField(name = "description", type = BizField.FieldType.STRING)
    private String description;
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

    public String getTriggerCode() {
        return triggerCode;
    }

    public void setTriggerCode(String triggerCode) {
        this.triggerCode = triggerCode;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
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

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(LocalDateTime nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    public LocalDateTime getLastFireTime() {
        return lastFireTime;
    }

    public void setLastFireTime(LocalDateTime lastFireTime) {
        this.lastFireTime = lastFireTime;
    }

    public String getLastFireStatus() {
        return lastFireStatus;
    }

    public void setLastFireStatus(String lastFireStatus) {
        this.lastFireStatus = lastFireStatus;
    }

    public String getLastFireMessage() {
        return lastFireMessage;
    }

    public void setLastFireMessage(String lastFireMessage) {
        this.lastFireMessage = lastFireMessage;
    }

    public Integer getFireCount() {
        return fireCount;
    }

    public void setFireCount(Integer fireCount) {
        this.fireCount = fireCount;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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



