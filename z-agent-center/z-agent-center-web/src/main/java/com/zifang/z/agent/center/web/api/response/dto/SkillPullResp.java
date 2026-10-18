package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Skill 拉取响应 DTO.
 */
public class SkillPullResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String instanceCode;
    private String agentType;
    private List<String> skillCodes;
    private long timestamp;

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public List<String> getSkillCodes() {
        return skillCodes;
    }

    public void setSkillCodes(List<String> skillCodes) {
        this.skillCodes = skillCodes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
