package com.zifang.z.agent.engine.agent.plan;

import com.zifang.z.agent.engine.agent.plan.model.Plan;
import com.zifang.z.agent.engine.agent.plan.model.PlanStep;
import com.zifang.z.agent.engine.agent.skill.Skill;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * LlmPlanner 单元测试。
 * 测试 needsPlanning 启发式判断、markStepCompleted 逻辑。
 * decompose 依赖真实 LLM 调用，测试通过 mock 或降级路径覆盖。
 */
public class LlmPlannerTest {

    // ===== needsPlanning tests =====

    // Note: Java String.length() counts each Chinese character as 1 code point.
    // The threshold is 30 chars; ensure test goals exceed it.

    @Test
    public void testNeedsPlanning_longGoal_returnsTrue() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        // 35 Chinese chars — exceeds 30-char threshold
        assertTrue(planner.needsPlanning(
                "请帮我分析过去一周的销售数据，生成详细的对比报告并发给团队所有成员"));
    }

    @Test
    public void testNeedsPlanning_longGoalChinese_returnsTrue() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        // 31 Chinese chars — exceeds 30-char threshold (> 30)
        assertTrue(planner.needsPlanning(
                "帮我整理本月所有客户反馈信息，生成优先级列表并推送通知到群里哦"));
    }

    @Test
    public void testNeedsPlanning_shortGoal_returnsFalse() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        assertFalse(planner.needsPlanning("天气怎么样"));
        assertFalse(planner.needsPlanning("告诉我今天的日期"));
    }

    @Test
    public void testNeedsPlanning_nullGoal_returnsFalse() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        assertFalse(planner.needsPlanning(null));
    }

    @Test
    public void testNeedsPlanning_queryStyle_returnsFalse() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        assertFalse(planner.needsPlanning("查询u-9921用户的邮箱"));
        assertFalse(planner.needsPlanning("what is the capital of France"));
        assertFalse(planner.needsPlanning("find the error log"));
        assertFalse(planner.needsPlanning("get the current status"));
        assertFalse(planner.needsPlanning("show me the report"));
        assertFalse(planner.needsPlanning("帮我查一下"));
        assertFalse(planner.needsPlanning("请问现在几点"));
    }

    @Test
    public void testNeedsPlanning_exactThreshold_returnsFalse() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        // 30 字符刚好边界，算简单目标
        assertFalse(planner.needsPlanning("今天天气怎么样啊啊啊啊啊啊啊"));
    }

    // ===== markStepCompleted tests =====

    @Test
    public void testMarkStepCompleted_updatesPlanStep() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        Plan plan = new Plan("test");
        plan.addStep(PlanStep.builder().order(0).goal("step1").skillName("s1").params(null).build());

        planner.markStepCompleted(plan, 0, "step1 result");

        assertTrue(plan.getSteps().get(0).isCompleted());
        assertEquals("step1 result", plan.getSteps().get(0).getResult());
    }

    @Test
    public void testMarkStepCompleted_multipleSteps() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        Plan plan = new Plan("multi-step test");
        plan.addStep(PlanStep.builder().order(0).goal("step1").skillName("s1").params(null).build());
        plan.addStep(PlanStep.builder().order(1).goal("step2").skillName("s2").params(null).build());
        plan.addStep(PlanStep.builder().order(2).goal("step3").skillName("s3").params(null).build());

        planner.markStepCompleted(plan, 1, "step2 result");

        assertFalse(plan.getSteps().get(0).isCompleted());
        assertTrue(plan.getSteps().get(1).isCompleted());
        assertEquals("step2 result", plan.getSteps().get(1).getResult());
        assertFalse(plan.getSteps().get(2).isCompleted());
    }

    @Test
    public void testMarkStepCompleted_invalidIndex_ignores() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        Plan plan = new Plan("test");
        plan.addStep(PlanStep.builder().order(0).goal("step1").skillName("s1").params(null).build());

        // 无异常，不修改任何状态
        planner.markStepCompleted(plan, 5, "result");
        planner.markStepCompleted(plan, -1, "result");

        assertFalse(plan.getSteps().get(0).isCompleted());
        assertNull(plan.getSteps().get(0).getResult());
    }

    @Test
    public void testMarkStepCompleted_nullPlan_ignores() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        // 无异常
        planner.markStepCompleted(null, 0, "result");
    }

    // ===== Plan / PlanStep tests =====

    @Test
    public void testPlan_advance_updatesStatus() {
        Plan plan = new Plan("test");
        plan.addStep(PlanStep.builder().order(0).goal("s1").skillName("sk").params(null).build());
        plan.addStep(PlanStep.builder().order(1).goal("s2").skillName("sk").params(null).build());

        assertEquals(Plan.Status.PLANNING, plan.getStatus());

        plan.advance(); // currentStep = 1, not done yet
        assertEquals(Plan.Status.PLANNING, plan.getStatus());

        plan.advance(); // currentStep = 2, >= size -> COMPLETED
        assertEquals(Plan.Status.COMPLETED, plan.getStatus());
        assertTrue(plan.isDone());
    }

    @Test
    public void testPlan_getCurrentStep() {
        Plan plan = new Plan("test");
        plan.addStep(PlanStep.builder().order(0).goal("step0").skillName("sk").params(null).build());
        plan.addStep(PlanStep.builder().order(1).goal("step1").skillName("sk").params(null).build());

        assertEquals("step0", plan.getCurrentStep().getGoal());
        plan.advance();
        assertEquals("step1", plan.getCurrentStep().getGoal());
        plan.advance();
        assertNull(plan.getCurrentStep());
    }

    @Test
    public void testPlanStep_builder() {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");

        PlanStep step = PlanStep.builder()
                .order(3)
                .goal("分析数据")
                .skillName("analyze")
                .params(params)
                .build();

        assertEquals(3, step.getOrder());
        assertEquals("分析数据", step.getGoal());
        assertEquals("analyze", step.getSkillName());
        assertEquals("value", step.getParams().get("key"));
        assertFalse(step.isCompleted());
        assertNull(step.getResult());
    }

    @Test
    public void testPlan_builder() {
        List<PlanStep> steps = new ArrayList<>();
        steps.add(PlanStep.builder().order(0).goal("s1").skillName("sk").params(null).build());
        steps.add(PlanStep.builder().order(1).goal("s2").skillName("sk").params(null).build());

        Plan plan = Plan.builder()
                .originalGoal("complex goal")
                .steps(steps)
                .build();

        assertEquals("complex goal", plan.getOriginalGoal());
        assertEquals(2, plan.getSteps().size());
        assertEquals(Plan.Status.PLANNING, plan.getStatus());
    }

    // ===== Fallback plan test (LLM unavailable) =====

    @Test
    public void testDecompose_llmUnavailable_returnsFallbackPlan() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        // null LLMClient 会导致 blockLast 抛出异常，降级到 fallback
        Plan plan = planner.decompose("帮我做某事", new ArrayList<Skill>(), null);

        assertNotNull(plan);
        assertEquals("帮我做某事", plan.getOriginalGoal());
        assertFalse(plan.getSteps().isEmpty());
        assertEquals(1, plan.getSteps().size());
        // fallback plan skillName 为 unknown
        assertEquals("unknown", plan.getSteps().get(0).getSkillName());
    }

    @Test
    public void testDecompose_emptySkills_returnsFallbackPlan() {
        LlmPlanner planner = new LlmPlanner(null, "ollama", "llama3.2");
        Plan plan = planner.decompose("复杂目标", new ArrayList<Skill>(), null);

        assertNotNull(plan);
        assertFalse(plan.getSteps().isEmpty());
    }
}