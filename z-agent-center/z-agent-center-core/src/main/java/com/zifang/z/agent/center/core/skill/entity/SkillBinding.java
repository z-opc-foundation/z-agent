package com.zifang.z.agent.center.core.skill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * Skill 绑定关系实体 — FEATURE014 §5.1
 *
 * <p>对应表 {@code z_agent_skill_binding}，由 {@code _sql/z-agent-multi-instance.sql} 创建。</p>
 */
@BizModel(name = "Skill 绑定关系实体 — FEATURE014 §5.1", module = "z-agent", description = "Skill 绑定关系实体 — FEATURE014 §5.1 对应表 z_agent_skill_binding，由 _sql/z-agent-multi-instance.sql 创建。")
@TableName("z_agent_skill_binding")
public class SkillBinding {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    /**
     * Agent 实例编码
     */
    @BizField(name = "Agent 实例编码", type = BizField.FieldType.STRING)
    private String instanceCode;
    /**
     * 技能编码
     */
    @BizField(name = "技能编码", type = BizField.FieldType.STRING)
    private String skillCode;
    /**
     * 绑定类型: PUSH / PULL
     */
    @BizField(name = "绑定类型: PUSH 推送 / PULL 拉取", type = BizField.FieldType.STRING)
    private String bindingType;
    /**
     * 状态: BOUND / UNBOUND / SYNCING / ERROR
     */
    @BizField(name = "状态: BOUND / UNBOUND / SYNCING / ERROR", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "绑定时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtBind;
    @BizField(name = "最后同步时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtSync;
    @TableField("sync_error")
    @BizField(name = "errorMsg", type = BizField.FieldType.STRING)
    private String errorMsg;
    @BizField(name = "所属租户", type = BizField.FieldType.STRING)
    private String tenantCode;

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

    public String getBindingType() {
        return bindingType;
    }

    public void setBindingType(String bindingType) {
        this.bindingType = bindingType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getGmtBind() {
        return gmtBind;
    }

    public void setGmtBind(LocalDateTime gmtBind) {
        this.gmtBind = gmtBind;
    }

    public LocalDateTime getGmtSync() {
        return gmtSync;
    }

    public void setGmtSync(LocalDateTime gmtSync) {
        this.gmtSync = gmtSync;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}



