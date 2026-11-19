package com.zifang.z.agent.center.core.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * z-bot 长期记忆条目 — FEATURE014 Wishlist 3
 */
@BizModel(name = "z-bot 长期记忆条目 — FEATURE014 Wishlist 3", module = "z-agent", description = "z-bot 长期记忆条目 — FEATURE014 Wishlist 3")
@TableName("z_bot_memory")
public class BotMemoryEntry {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "所属 Agent 实例", type = BizField.FieldType.STRING)
    private String instanceCode;
    @BizField(name = "用户 ID（多用户隔离）", type = BizField.FieldType.STRING)
    private String userId;
    /**
     * user / assistant / system / summary
     */
    @BizField(name = "user / assistant / system / summary", type = BizField.FieldType.STRING)
    private String role;
    @BizField(name = "记忆内容（短文本 / JSON 摘要）", type = BizField.FieldType.STRING)
    private String content;
    /**
     * 重要度 0-1（summary 一般 1.0）
     */
    @BizField(name = "重要度 0-1（summary 一般 1.0）", type = BizField.FieldType.DECIMAL)
    private BigDecimal importance;
    /**
     * chat / feedback / consolidation / manual
     */
    @BizField(name = "chat / feedback / consolidation / manual", type = BizField.FieldType.STRING)
    private String source;
    @BizField(name = "逗号分隔标签（便于检索）", type = BizField.FieldType.STRING)
    private String tags;
    @BizField(name = "所属租户", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "创建时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "过期时间（NULL = 永不过期）", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtExpire;
    @BizField(name = "逻辑删除", type = BizField.FieldType.INTEGER)
    private Integer isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public BigDecimal getImportance() {
        return importance;
    }

    public void setImportance(BigDecimal importance) {
        this.importance = importance;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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

    public LocalDateTime getGmtExpire() {
        return gmtExpire;
    }

    public void setGmtExpire(LocalDateTime gmtExpire) {
        this.gmtExpire = gmtExpire;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}



