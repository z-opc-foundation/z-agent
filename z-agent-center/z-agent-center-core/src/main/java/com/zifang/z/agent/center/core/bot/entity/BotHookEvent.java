package com.zifang.z.agent.center.core.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * Hook 事件审计 — FEATURE014 Wishlist 5
 */
@BizModel(name = "Hook 事件审计 — FEATURE014 Wishlist 5", module = "z-agent", description = "Hook 事件审计 — FEATURE014 Wishlist 5")
@TableName("z_bot_hook_event")
public class BotHookEvent {

    @TableId(type = IdType.AUTO)
    @BizField(name = "主键", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "所属 Agent 实例", type = BizField.FieldType.STRING)
    private String instanceCode;
    @BizField(name = "Hook 实现类名（短名）", type = BizField.FieldType.STRING)
    private String hookName;
    /**
     * preChat / postChat / preTool / postTool / onError
     */
    @BizField(name = "preChat / postChat / preTool / postTool / onError", type = BizField.FieldType.STRING)
    private String eventType;
    @BizField(name = "事件 payload（JSON）", type = BizField.FieldType.STRING)
    private String payload;
    /**
     * SUCCESS / FAIL / SKIPPED
     */
    @BizField(name = "SUCCESS / FAIL / SKIPPED", type = BizField.FieldType.STRING)
    private String result;
    @BizField(name = "errorMsg", type = BizField.FieldType.STRING)
    private String errorMsg;
    @BizField(name = "durationMs", type = BizField.FieldType.INTEGER)
    private Long durationMs;
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

    public String getHookName() {
        return hookName;
    }

    public void setHookName(String hookName) {
        this.hookName = hookName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
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

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
}



