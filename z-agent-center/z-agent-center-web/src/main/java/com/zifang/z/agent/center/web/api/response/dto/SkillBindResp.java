package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Skill 绑定响应 DTO.
 */
public class SkillBindResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private int created;
    private String instanceCode;
    private List<String> skillCodes;
    private String bindingType;

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
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

    public String getBindingType() {
        return bindingType;
    }

    public void setBindingType(String bindingType) {
        this.bindingType = bindingType;
    }
}
