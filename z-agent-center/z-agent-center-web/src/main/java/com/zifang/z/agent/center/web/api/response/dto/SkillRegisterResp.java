package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Skill 注册响应 DTO.
 */
public class SkillRegisterResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String instanceCode;
    private String authToken;
    private String skillRootPath;
    private List<String> assignedSkills;

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getSkillRootPath() {
        return skillRootPath;
    }

    public void setSkillRootPath(String skillRootPath) {
        this.skillRootPath = skillRootPath;
    }

    public List<String> getAssignedSkills() {
        return assignedSkills;
    }

    public void setAssignedSkills(List<String> assignedSkills) {
        this.assignedSkills = assignedSkills;
    }
}
