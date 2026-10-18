package com.zifang.z.agent.engine.agent.plan;

import com.zifang.z.agent.engine.agent.plan.model.Plan;
import com.zifang.z.agent.engine.agent.skill.Skill;

import java.util.List;

/**
 * 规划器接口。给定用户目标和可用 skills，输出执行计划。
 * LLM 驱动实现：通过 LLM 调用生成结构化 plan JSON。
 */
public interface Planner {

    /**
     * 将复杂目标分解为步骤计划。
     *
     * @param goal            用户目标
     * @param availableSkills 可用的 skills 列表
     * @param memoryContext   记忆上下文（可选，可为 null）
     * @return 分解后的执行计划
     */
    Plan decompose(String goal, List<Skill> availableSkills, String memoryContext);

    /**
     * 更新计划状态（标记某步骤完成）。
     *
     * @param plan      执行计划
     * @param stepIndex 步骤索引
     * @param result    执行结果
     */
    void markStepCompleted(Plan plan, int stepIndex, String result);

    /**
     * 判断目标是否需要规划（过于简单直接 ReAct）。
     *
     * @param goal 用户目标
     * @return true 如果需要规划，false 直接走 ReAct
     */
    boolean needsPlanning(String goal);
}