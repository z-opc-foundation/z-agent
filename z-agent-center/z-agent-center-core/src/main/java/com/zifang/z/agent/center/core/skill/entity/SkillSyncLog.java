package com.zifang.z.agent.center.core.skill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Skill 同步日志实体 — FEATURE014 §5.2
 */
@TableName("z_agent_skill_sync_log")
public class SkillSyncLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String instanceCode;
    private String skillCode;
    private String agentType;
    /**
     * INSTALL / UPGRADE / REMOVE
     */
    private String action;
    /**
     * SUCCESS / FAIL
     */
    private String result;
    private String errorMsg;
    private Long durationMs;
    private String tenantCode;
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
