package com.zifang.z.agent.engine.agent.plan.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 复杂目标的执行计划。
 * 包含多个 PlanStep，按顺序执行。
 */
public class Plan implements Serializable {
    private static final long serialVersionUID = 1L;
    private String originalGoal;
    private List<PlanStep> steps = new ArrayList<>();
    private int stepIndex;
    private Status status = Status.PLANNING;

    public Plan() {
    }

    public Plan(String originalGoal) {
        this.originalGoal = originalGoal;
    }

    // builder
    public static Builder builder() {
        return new Builder();
    }

    // 步骤管理
    public void addStep(PlanStep step) {
        steps.add(step);
    }

    public List<PlanStep> getSteps() {
        return steps;
    }

    public void setSteps(List<PlanStep> steps) {
        this.steps = steps;
    }

    /**
     * 返回当前待执行的步骤（如果没有更多步骤则返回 null）
     */
    public PlanStep getCurrentStep() {
        return stepIndex < steps.size() ? steps.get(stepIndex) : null;
    }

    /**
     * 推进到下一步，如果已到最后一步则标记为 COMPLETED。
     */
    public void advance() {
        stepIndex++;
        if (stepIndex >= steps.size()) {
            status = Status.COMPLETED;
        }
    }

    public boolean isDone() {
        return status == Status.COMPLETED || status == Status.FAILED;
    }

    // getter / setter
    public String getOriginalGoal() {
        return originalGoal;
    }

    public void setOriginalGoal(String originalGoal) {
        this.originalGoal = originalGoal;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        PLANNING,
        EXECUTING,
        COMPLETED,
        FAILED
    }

    public static class Builder {
        private String originalGoal;
        private List<PlanStep> steps = new ArrayList<>();

        public Builder originalGoal(String v) {
            this.originalGoal = v;
            return this;
        }

        public Builder steps(List<PlanStep> v) {
            this.steps = v;
            return this;
        }

        public Builder addStep(PlanStep step) {
            this.steps.add(step);
            return this;
        }

        public Plan build() {
            Plan plan = new Plan(originalGoal);
            plan.setSteps(steps);
            return plan;
        }
    }
}