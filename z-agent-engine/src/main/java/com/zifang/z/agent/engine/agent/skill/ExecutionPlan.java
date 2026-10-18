package com.zifang.z.agent.engine.agent.skill;

import java.util.List;
import java.util.Map;

/**
 * Execution plan for skill execution.
 */
public class ExecutionPlan {

    private String skill;
    private Map<String, Object> params;
    private List<SelectedSkill> decompose;

    public ExecutionPlan() {
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

    public List<SelectedSkill> getDecompose() {
        return decompose;
    }

    public void setDecompose(List<SelectedSkill> decompose) {
        this.decompose = decompose;
    }
}
