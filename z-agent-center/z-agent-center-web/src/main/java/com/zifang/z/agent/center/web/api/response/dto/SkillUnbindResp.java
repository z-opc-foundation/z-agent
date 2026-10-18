package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Skill 解绑响应 DTO.
 */
public class SkillUnbindResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean removed;
    private String instanceCode;
    private List<String> skillCodes;

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public List<String> getSkillCodes() {
        return skillCodes;
    }

    public void setSkillCodes(List<String> skillCodes) {
        this.skillCodes = skillCodes;
    }
}
