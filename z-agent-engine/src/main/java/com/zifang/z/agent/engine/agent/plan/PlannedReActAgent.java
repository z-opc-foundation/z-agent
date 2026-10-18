package com.zifang.z.agent.engine.agent.plan;

import com.zifang.z.agent.engine.agent.plan.model.Plan;
import com.zifang.z.agent.engine.agent.plan.model.PlanStep;
import com.zifang.z.agent.engine.agent.react.Agent;
import com.zifang.z.agent.engine.agent.react.ReActAgent;
import com.zifang.z.agent.engine.agent.react.StreamEvent;
import com.zifang.z.agent.engine.agent.skill.Skill;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;

/**
 * 支持 Planning 的 ReActAgent 变体。
 * <p>
 * 执行流程：
 * 1. 调用 planner.needsPlanning() 判断是否需要规划
 * 2. 如果需要：调用 planner.decompose() → 按 Plan 步骤逐一执行
 * 3. 如果不需要：走原有 ReAct Loop
 */
public class PlannedReActAgent implements Agent {

    private final ReActAgent delegate;
    private final Planner planner;
    private final List<Skill> availableSkills;

    public PlannedReActAgent(ReActAgent delegate, Planner planner, List<Skill> availableSkills) {
        this.delegate = delegate;
        this.planner = planner;
        this.availableSkills = availableSkills;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public Flux<StreamEvent> stream(String input) {
        if (planner != null && planner.needsPlanning(input)) {
            return executeWithPlan(input);
        }
        return delegate.stream(input);
    }

    /**
     * 通过规划执行复杂目标。
     * 按顺序执行每个计划步骤，将每步结果注入到 delegate 供下一步使用。
     */
    private Flux<StreamEvent> executeWithPlan(String input) {
        return Flux.create(sink -> {
            try {
                sink.next(new StreamEvent.ThoughtDelta("[规划中...]"));

                Plan plan = planner.decompose(input, availableSkills, null);
                if (plan.getSteps().isEmpty()) {
                    // 解析失败，降级到普通 ReAct
                    delegate.stream(input).subscribe(
                            e -> sink.next(e),
                            err -> sink.error(err),
                            () -> sink.complete()
                    );
                    return;
                }

                sink.next(new StreamEvent.ThoughtDelta("[计划制定完成，共" + plan.getSteps().size() + "步]"));

                // 顺序执行每个步骤，使用递归迭代避免嵌套订阅地狱
                executeStepsSequentially(sink, plan, 0);

            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    /**
     * 顺序执行计划步骤。
     * 每步执行完成后进入下一步，全部完成后发送 Done 事件。
     */
    private void executeStepsSequentially(FluxSink<StreamEvent> sink, Plan plan, int stepIndex) {
        if (sink.isCancelled() || stepIndex >= plan.getSteps().size()) {
            // 已完成或已取消
            if (!sink.isCancelled() && stepIndex >= plan.getSteps().size()) {
                String lastResult = plan.getSteps().isEmpty() ? ""
                        : plan.getSteps().get(plan.getSteps().size() - 1).getResult();
                sink.next(new StreamEvent.Done(lastResult, plan.getSteps().size(), null, null));
            }
            sink.complete();
            return;
        }

        PlanStep step = plan.getSteps().get(stepIndex);
        sink.next(new StreamEvent.StepStart(stepIndex + 1));
        sink.next(new StreamEvent.ThoughtDelta("[执行步骤" + (stepIndex + 1) + ": " + step.getGoal() + "]"));

        // 构建执行 skill 的输入
        String skillInput = "请执行 skill: " + step.getSkillName() + ", 目标: " + step.getGoal();

        // 订阅 delegate 的 stream，只取第一个 Done 事件
        delegate.stream(skillInput)
                .ofType(StreamEvent.Done.class)
                .take(1)
                .subscribe(
                        done -> {
                            // 标记步骤完成
                            planner.markStepCompleted(plan, stepIndex, done.reply);
                            sink.next(new StreamEvent.FinalDelta(done.reply));

                            // 递归执行下一步
                            executeStepsSequentially(sink, plan, stepIndex + 1);
                        },
                        err -> {
                            planner.markStepCompleted(plan, stepIndex, "ERROR: " + err.getMessage());
                            sink.next(new StreamEvent.ErrorEvent(err));
                            sink.complete();
                        }
                );
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }
}