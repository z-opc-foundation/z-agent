package com.zifang.z.agent.center.core.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.time.LocalDateTime;

/**
 * Agent 应用草稿实体（平台层专用，z-agent-common 不需要这个）
 */
@BizModel(name = "Agent 应用草稿实体（平台层专用，z-agent-common 不需要这个）", module = "z-agent", description = "Agent 应用草稿实体（平台层专用，z-agent-common 不需要这个）")
@TableName("z_agent_app_draft")
public class AgentAppDraft {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;
    @BizField(name = "draftData", type = BizField.FieldType.STRING)
    private String draftData;
    @BizField(name = "version", type = BizField.FieldType.INTEGER)
    private Integer version;
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

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getDraftData() {
        return draftData;
    }

    public void setDraftData(String draftData) {
        this.draftData = draftData;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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

