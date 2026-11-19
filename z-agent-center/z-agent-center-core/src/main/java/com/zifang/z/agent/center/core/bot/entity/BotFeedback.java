package com.zifang.z.agent.center.core.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * z-bot 用户反馈 — FEATURE014 Wishlist 4
 */
@BizModel(name = "z-bot 用户反馈 — FEATURE014 Wishlist 4", module = "z-agent", description = "z-bot 用户反馈 — FEATURE014 Wishlist 4")
@TableName("z_bot_feedback")
public class BotFeedback {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "所属 Agent 实例", type = BizField.FieldType.STRING)
    private String instanceCode;
    @BizField(name = "关联的对话 trace_id", type = BizField.FieldType.STRING)
    private String traceId;
    @BizField(name = "反馈用户 ID", type = BizField.FieldType.STRING)
    private String userId;
    /**
     * 1=点赞, -1=点踩, 0=中性
     */
    @BizField(name = "1=点赞, -1=点踩, 0=中性", type = BizField.FieldType.INTEGER)
    private Integer rating;
    @BizField(name = "反馈备注", type = BizField.FieldType.STRING)
    private String comment;
    @BizField(name = "触发反馈的应用", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "当时使用的 skill（逗号分隔）", type = BizField.FieldType.STRING)
    private String skillCodes;
    @BizField(name = "所属租户", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "isDeleted", type = BizField.FieldType.INTEGER)
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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getSkillCodes() {
        return skillCodes;
    }

    public void setSkillCodes(String skillCodes) {
        this.skillCodes = skillCodes;
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

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}



