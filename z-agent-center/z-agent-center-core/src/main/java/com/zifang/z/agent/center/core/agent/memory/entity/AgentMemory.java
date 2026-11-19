package com.zifang.z.agent.center.core.agent.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * FEATURE013 T2.3: Agent 长期记忆实体.
 */
@BizModel(name = "FEATURE013 T2.3: Agent 长期记忆实体", module = "z-agent", description = "FEATURE013 T2.3: Agent 长期记忆实体.")
@TableName("z_agent_memory")
public class AgentMemory {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    /**
     * 归属用户 ID
     */
    @BizField(name = "归属用户 ID", type = BizField.FieldType.STRING)
    private String userId;

    /**
     * 关联 Agent 应用 (NULL=全局用户记忆)
     */
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;

    /**
     * 记忆类型: FACT/PREFERENCE/EVENT
     */
    @BizField(name = "memoryType", type = BizField.FieldType.STRING)
    private String memoryType;

    /**
     * 记忆内容
     */
    @BizField(name = "记忆内容", type = BizField.FieldType.STRING)
    private String content;

    /**
     * 向量表示 (JSON array, 后续接 Embedding)
     */
    @BizField(name = "向量表示 (JSON array, 后续接 Embedding)", type = BizField.FieldType.STRING)
    private String embedding;

    /**
     * 空格分隔关键词 (用于 BM25 简版检索)
     */
    @BizField(name = "空格分隔关键词 (用于 BM25 简版检索)", type = BizField.FieldType.STRING)
    private String keywords;

    /**
     * 重要性 0-1
     */
    @BizField(name = "重要性 0-1", type = BizField.FieldType.DECIMAL)
    private Float importance;

    /**
     * 活跃度 0-1
     */
    @BizField(name = "活跃度 0-1", type = BizField.FieldType.DECIMAL)
    private Float decayScore;

    /**
     * 命中次数
     */
    @BizField(name = "命中次数", type = BizField.FieldType.INTEGER)
    private Integer accessCount;

    /**
     * 来源: AUTO/AUTO_DISTILL/MANUAL/IMPORT
     */
    @BizField(name = "source", type = BizField.FieldType.STRING)
    private String source;

    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;

    @BizField(name = "lastAccessedAt", type = BizField.FieldType.DATETIME)
    private LocalDateTime lastAccessedAt;

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getMemoryType() {
        return memoryType;
    }

    public void setMemoryType(String memoryType) {
        this.memoryType = memoryType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Float getImportance() {
        return importance;
    }

    public void setImportance(Float importance) {
        this.importance = importance;
    }

    public Float getDecayScore() {
        return decayScore;
    }

    public void setDecayScore(Float decayScore) {
        this.decayScore = decayScore;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
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



