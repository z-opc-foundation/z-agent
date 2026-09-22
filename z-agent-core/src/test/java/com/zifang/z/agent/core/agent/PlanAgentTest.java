package com.zifang.z.agent.core.agent;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlanAgentTest {

    @Test
    public void parse_numbered_list() {
        String text = "1. 分析需求\n2. 设计方案\n3. 实施\n4. 验证";
        List<String> steps = PlanAgent.parsePlan(text);
        assertEquals(4, steps.size());
        assertEquals("分析需求", steps.get(0));
        assertEquals("设计方案", steps.get(1));
        assertEquals("实施", steps.get(2));
        assertEquals("验证", steps.get(3));
    }

    @Test
    public void parse_dot_and_chinese_paren() {
        String text = "1) 第一步\n2) 第二步\n3) 第三步";
        List<String> steps = PlanAgent.parsePlan(text);
        assertEquals(3, steps.size());
        assertEquals("第一步", steps.get(0));
        assertEquals("第二步", steps.get(1));
        assertEquals("第三步", steps.get(2));
    }

    @Test
    public void parse_handles_fullwidth_paren() {
        String text = "1、收集资料\n2、撰写文档\n3、审校发布";
        List<String> steps = PlanAgent.parsePlan(text);
        assertEquals(3, steps.size());
        assertEquals("收集资料", steps.get(0));
    }

    @Test
    public void parse_no_numbered_returns_full_text_as_one_step() {
        String text = "直接执行任务, 不分步";
        List<String> steps = PlanAgent.parsePlan(text);
        assertEquals(1, steps.size());
        assertEquals("直接执行任务, 不分步", steps.get(0));
    }

    @Test
    public void parse_null_returns_empty() {
        List<String> steps = PlanAgent.parsePlan(null);
        assertTrue(steps.isEmpty());
    }

    @Test
    public void parse_empty_returns_empty() {
        List<String> steps = PlanAgent.parsePlan("");
        assertTrue(steps.isEmpty());
    }

    @Test
    public void parse_partial_numbered_only_takes_numbered_lines() {
        String text = "以下是步骤:\n1. 准备\n2. 执行\n末尾说明.";
        List<String> steps = PlanAgent.parsePlan(text);
        assertEquals(2, steps.size());
        assertEquals("准备", steps.get(0));
        assertEquals("执行", steps.get(1));
    }
}