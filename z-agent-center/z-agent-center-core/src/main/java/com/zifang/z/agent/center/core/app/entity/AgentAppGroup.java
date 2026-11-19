package com.zifang.z.agent.center.core.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

@BizModel(name = "z_agent_app_group", module = "z-agent", description = "z_agent_app_group")
@TableName("z_agent_app_group")
public class AgentAppGroup {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "分组编码（唯一标识）", type = BizField.FieldType.STRING)
    private String groupCode;
    @BizField(name = "父分组编码，空字符串表示顶级", type = BizField.FieldType.STRING)
    private String parentCode = "";
    @BizField(name = "分组名称", type = BizField.FieldType.STRING)
    private String groupName;
    @BizField(name = "排序号", type = BizField.FieldType.INTEGER)
    private Integer sortOrder = 0;
    @BizField(name = "description", type = BizField.FieldType.STRING)
    private String description = "";
    @BizField(name = "extConfig", type = BizField.FieldType.STRING)
    private String extConfig = "";
    @BizField(name = "createdTime", type = BizField.FieldType.DATETIME)
    private LocalDateTime createdTime;
    @BizField(name = "updatedTime", type = BizField.FieldType.DATETIME)
    private LocalDateTime updatedTime;
    @BizField(name = "domainCode", type = BizField.FieldType.STRING)
    private String domainCode = "";
    @BizField(name = "orgCode", type = BizField.FieldType.STRING)
    private String orgCode = "";
    @BizField(name = "deptCode", type = BizField.FieldType.STRING)
    private String deptCode = "";
    @BizField(name = "租户编码", type = BizField.FieldType.STRING)
    private String tenantCode = "default";
    @BizField(name = "status", type = BizField.FieldType.INTEGER)
    private Integer status = 1;
    @BizField(name = "isDeleted", type = BizField.FieldType.INTEGER)
    private Integer isDeleted = 0;
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "gmtModified", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtConfig() {
        return extConfig;
    }

    public void setExtConfig(String extConfig) {
        this.extConfig = extConfig;
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

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
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
}



