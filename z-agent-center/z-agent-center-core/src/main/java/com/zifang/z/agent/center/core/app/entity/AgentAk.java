package com.zifang.z.agent.center.core.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

@BizModel(name = "z_agent_ak", module = "z-agent", description = "z_agent_ak")
@TableName("z_agent_ak")
public class AgentAk {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "akName", type = BizField.FieldType.STRING)
    private String akName;
    @BizField(name = "accessKey", type = BizField.FieldType.STRING)
    private String accessKey;
    @BizField(name = "secretKey", type = BizField.FieldType.STRING)
    private String secretKey;
    @BizField(name = "status", type = BizField.FieldType.INTEGER)
    private Integer status = 1;
    @BizField(name = "totalTokens", type = BizField.FieldType.INTEGER)
    private Long totalTokens = 0L;
    @BizField(name = "lastUsedTime", type = BizField.FieldType.DATETIME)
    private LocalDateTime lastUsedTime;
    @BizField(name = "description", type = BizField.FieldType.STRING)
    private String description = "";
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode = "default";
    @BizField(name = "akType", type = BizField.FieldType.STRING)
    private String akType = "PERSONAL";
    @BizField(name = "ownerUserId", type = BizField.FieldType.STRING)
    private String ownerUserId;
    @BizField(name = "ownerUserName", type = BizField.FieldType.STRING)
    private String ownerUserName;
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "appName", type = BizField.FieldType.STRING)
    private String appName;
    @BizField(name = "createdTime", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdTime;
    @BizField(name = "updatedTime", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedTime;
    @BizField(name = "isDeleted", type = BizField.FieldType.INTEGER)
    private Integer isDeleted = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAkName() {
        return akName;
    }

    public void setAkName(String akName) {
        this.akName = akName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public LocalDateTime getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(LocalDateTime lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
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

    public String getAkType() {
        return akType;
    }

    public void setAkType(String akType) {
        this.akType = akType;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerUserName() {
        return ownerUserName;
    }

    public void setOwnerUserName(String ownerUserName) {
        this.ownerUserName = ownerUserName;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
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



