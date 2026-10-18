package com.zifang.z.agent.engine.agent.plan.model;

import java.io.Serializable;
import java.util.Map;

/**
 * 单个规划步骤。
 * 对应一个可执行的 skill，包含目标描述、skill 名称和参数。
 */
public class PlanStep implements Serializable {
    private static final long serialVersionUID = 1L;

    private int order;
    private String goal;          // 本步目标描述
    private String skillName;     // 对应 skill 名称
    private Map<String, Object> params; // skill 参数
    private boolean completed;
    private String result;        // 执行结果（事后填充）

    public PlanStep() {
    }

    public PlanStep(int order, String goal, String skillName, Map<String, Object> params) {
        this.order = order;
        this.goal = goal;
        this.skillName = skillName;
        this.params = params;
    }

    // builder
    public static Builder builder() {
        return new Builder();
    }

    // getter / setter
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public static class Builder {
        private int order;
        private String goal;
        private String skillName;
        private Map<String, Object> params;

        public Builder order(int v) {
            this.order = v;
            return this;
        }

        public Builder goal(String v) {
            this.goal = v;
            return this;
        }

        public Builder skillName(String v) {
            this.skillName = v;
            return this;
        }

        public Builder params(Map<String, Object> v) {
            this.params = v;
            return this;
        }

        public PlanStep build() {
            return new PlanStep(order, goal, skillName, params);
        }
    }
}