package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * 模板 Fork 响应 DTO.
 */
public class TemplateForkResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long templateId;
    private String templateCode;
    private String newAppCode;
    private String newAppName;
    private String requiredPlugins;
    private String requiredSkills;
    private String requiredKnowledge;
    private Object appDto;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getNewAppCode() {
        return newAppCode;
    }

    public void setNewAppCode(String newAppCode) {
        this.newAppCode = newAppCode;
    }

    public String getNewAppName() {
        return newAppName;
    }

    public void setNewAppName(String newAppName) {
        this.newAppName = newAppName;
    }

    public String getRequiredPlugins() {
        return requiredPlugins;
    }

    public void setRequiredPlugins(String requiredPlugins) {
        this.requiredPlugins = requiredPlugins;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getRequiredKnowledge() {
        return requiredKnowledge;
    }

    public void setRequiredKnowledge(String requiredKnowledge) {
        this.requiredKnowledge = requiredKnowledge;
    }

    public Object getAppDto() {
        return appDto;
    }

    public void setAppDto(Object appDto) {
        this.appDto = appDto;
    }
}
