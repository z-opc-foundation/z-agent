package com.zifang.z.agent.center.core.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * Skill 自动晋升记录 — FEATURE014 Wishlist 6
 */
@BizModel(name = "Skill 自动晋升记录 — FEATURE014 Wishlist 6", module = "z-agent", description = "Skill 自动晋升记录 — FEATURE014 Wishlist 6")
@TableName("z_bot_skill_promotion")
public class BotSkillPromotion {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "所属 Agent 实例", type = BizField.FieldType.STRING)
    private String instanceCode;
    @BizField(name = "被晋升的 skill", type = BizField.FieldType.STRING)
    private String skillCode;
    @BizField(name = "被写入的应用", type = BizField.FieldType.STRING)
    private String appCode;
    /**
     * add / remove
     */
    @BizField(name = "add / remove", type = BizField.FieldType.STRING)
    private String promotionKind;
    @BizField(name = "晋升理由（高赞数 / 反馈触发）", type = BizField.FieldType.STRING)
    private String reason;
    @BizField(name = "触发晋升时的累计反馈分数", type = BizField.FieldType.INTEGER)
    private Integer feedbackScore;
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;

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

    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getPromotionKind() {
        return promotionKind;
    }

    public void setPromotionKind(String promotionKind) {
        this.promotionKind = promotionKind;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getFeedbackScore() {
        return feedbackScore;
    }

    public void setFeedbackScore(Integer feedbackScore) {
        this.feedbackScore = feedbackScore;
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
}



