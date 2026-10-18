package com.zifang.z.agent.center.core.agent.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 工作流定义实体 (FEATURE013 A2)
 * 对应表: z_agent_flow_definition
 */
@TableName("z_agent_flow_definition")
public class FlowDefinitionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流程ID (业务唯一标识)
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
     * DAG 节点数组 JSON (LogicFlow 画布的 nodes 数组)
     */
    private String nodes;

    /**
     * DAG 有向边 JSON (LogicFlow 画布的 edges 数组)
     */
    private String edges;

    /**
     * 流程静态变量 JSON
     */
    private String variables;

    /**
     * 流程总超时 (毫秒, 默认30分钟)
     */
    private Long timeoutMs = 1800000L;

    /**
     * 重试策略 JSON
     */
    private String retryPolicy;

    /**
     * 关联 Agent App (NULL 表示独立流程)
     */
    private String appCode;

    /**
     * AGENT / WF / STANDALONE
     */
    private String flowType = "AGENT";

    /**
     * 版本号 (FEATURE013 A9)
     */
    private Integer version = 1;

    /**
     * 上一版本 flow_definition.id (FEATURE013 A9)
     */
    private Long parentVersionId;

    /**
     * 本版本变更说明 (FEATURE013 A9)
     */
    private String changelog;

    /**
     * DRAFT / PUBLISHED / ARCHIVED
     */
    private String status = "DRAFT";

    /**
     * 创建人
     */
    private String creator;

    /**
     * 租户编码
     */
    @TableField("tenant_code")
    private String tenantCode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer isDeleted = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getEdges() {
        return edges;
    }

    public void setEdges(String edges) {
        this.edges = edges;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(String retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getFlowType() {
        return flowType;
    }

    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getParentVersionId() {
        return parentVersionId;
    }

    public void setParentVersionId(Long parentVersionId) {
        this.parentVersionId = parentVersionId;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}