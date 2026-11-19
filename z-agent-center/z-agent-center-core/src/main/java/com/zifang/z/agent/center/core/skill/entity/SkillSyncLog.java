package com.zifang.z.agent.center.core.skill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * Skill 同步日志实体 — FEATURE014 §5.2
 */
@BizModel(name = "Skill 同步日志实体 — FEATURE014 §5.2", module = "z-agent", description = "Skill 同步日志实体 — FEATURE014 §5.2")
@TableName("z_agent_skill_sync_log")
public class SkillSyncLog {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "Agent 实例编码", type = BizField.FieldType.STRING)
    private String instanceCode;
    @BizField(name = "技能编码", type = BizField.FieldType.STRING)
    private String skillCode;
    @BizField(name = "Agent 类型（冗余便于查询）", type = BizField.FieldType.STRING)
    private String agentType;
    /**
     * INSTALL / UPGRADE / REMOVE
     */
    @BizField(name = "INSTALL / UPGRADE / REMOVE", type = BizField.FieldType.STRING)
    private String action;
    /**
     * SUCCESS / FAIL
     */
    @BizField(name = "SUCCESS / FAIL", type = BizField.FieldType.STRING)
    private String result;
    @BizField(name = "失败时的错误信息", type = BizField.FieldType.STRING)
    private String errorMsg;
    @BizField(name = "同步耗时（毫秒）", type = BizField.FieldType.INTEGER)
    private Long durationMs;
    @BizField(name = "所属租户", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "同步时间", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtSync;

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

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public LocalDateTime getGmtSync() {
        return gmtSync;
    }

    public void setGmtSync(LocalDateTime gmtSync) {
        this.gmtSync = gmtSync;
    }
}



