package com.zifang.z.agent.engine.agent.skill;

import java.util.Map;

/**
 * Selected skill with parameters for execution.
 */
public class SelectedSkill {

    private String skill;
    private Map<String, Object> params;

    public SelectedSkill() {
    }

    public SelectedSkill(String skill, Map<String, Object> params) {
        this.skill = skill;
        this.params = params;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
