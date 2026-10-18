package com.zifang.z.agent.engine.agent;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.engine.agent.llm.model.define.Model;
import com.zifang.z.agent.engine.agent.llm.model.define.ModelMessage;
import com.zifang.z.agent.engine.agent.skill.ExecutionPlan;
import com.zifang.z.agent.engine.agent.skill.SelectedSkill;
import com.zifang.z.agent.engine.agent.skill.Skill;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JDK1.8 兼容 - 核心执行引擎（Composite Skill拆解 + 原子Skill执行）
 */
public class AgentEngine {

    private final List<Skill> skills;
    private final Model model;

    // JDK1.8 构造器
    public AgentEngine(List<Skill> skills, Model model) {
        this.skills = skills;
        this.model = model;
    }

    public void runTask2(String userIntent) throws Exception {
        // 1. 让模型选择合适的Skill
        List<ModelMessage> selectPrompt = buildSelectPrompt2(userIntent);
        String selectJson = model.chat(selectPrompt);
        SelectedSkill selectedSkill = JsonUtil.fromJson(selectJson, SelectedSkill.class);
        System.out.println("【步骤1】选中Skill：" + selectedSkill.getSkill());

        // 2. 查找Skill定义
        Skill targetSkill = findSkillByName(selectedSkill.getSkill());
        if (targetSkill == null) {
            throw new RuntimeException("未找到Skill：" + selectedSkill.getSkill());
        }

        // 3. 如果是Composite Skill → 拆解为原子Skill
        if (targetSkill.isComposite()) {
            List<ModelMessage> decomposePrompt = buildDecomposePrompt2(selectedSkill);
            String decomposeJson = model.chat(decomposePrompt);
            ExecutionPlan executionPlan = JsonUtil.fromJson(decomposeJson, ExecutionPlan.class);
            System.out.println("【步骤2】拆解为子Skill数量：" + executionPlan.getDecompose().size());

            // 4. 依次执行原子Skill
            for (SelectedSkill step : executionPlan.getDecompose()) {
                executeAtomicSkill(step);
            }
        } else {
            // 直接执行原子Skill
            executeAtomicSkill(selectedSkill);
        }

        System.out.println("【步骤3】任务执行完成！");
    }


    private List<ModelMessage> buildSelectPrompt2(String userIntent) throws Exception {

        StringBuilder system = new StringBuilder();
        system.append("你是技能选择器，仅输出JSON格式结果，不要多余文字！\n");
        system.append("=== 可用技能列表 ===\n");
        system.append(JsonUtil.toJsonExcludingNulls(skills)).append("\n");

        system.append("=== 输出格式 ===\n");
        system.append("{\"skill\":\"技能名称\",\"params\":{\"参数名\":\"参数值\"}}");

        StringBuilder user = new StringBuilder();
        user.append("用户意图: \n");
        user.append(userIntent).append("\n");

        return Arrays.asList(
                ModelMessage.of("system", system.toString()),
                ModelMessage.of("user", user.toString())
        );
    }


    private List<ModelMessage> buildDecomposePrompt2(SelectedSkill selectedSkill) throws Exception {
        // 过滤出所有原子Skill（非Composite）
        List<Skill> atomicSkills = skills.stream()
                .filter(skill -> !skill.isComposite())
                .collect(Collectors.toList());

        StringBuilder system = new StringBuilder();
        system.append("你是技能拆解器，仅输出JSON格式结果，不要多余文字！\n");
        system.append("=== 可用原子技能 ===\n");
        system.append(JsonUtil.toJsonExcludingNulls(atomicSkills)).append("\n");
        system.append("=== 输出格式 ===\n");
        system.append("{\"skill\":\"复合技能名\",\"params\":{},\"decompose\":[{\"skill\":\"原子技能名\",\"params\":{}}]}");

        StringBuilder user = new StringBuilder();
        user.append("=== 用户调用的复合技能 ===\n");
        user.append(JsonUtil.toJsonExcludingNulls(selectedSkill)).append("\n");

        return Arrays.asList(
                ModelMessage.of("system", system.toString()),
                ModelMessage.of("user", user.toString())
        );
    }

    /**
     * 执行原子Skill（本地操作）
     */
    private void executeAtomicSkill(SelectedSkill step) {
        System.out.println("【执行原子Skill】：" + step.getSkill() + " | 参数：" + step.getParams());
        try {
            switch (step.getSkill()) {
                case "write_file":
                    // 写入文件（JDK1.8 Files API）
                    String path = step.getParams().get("path").toString();
                    String content = step.getParams().get("content").toString();
                    java.nio.file.Files.write(
                            java.nio.file.Paths.get(path),
                            content.getBytes("UTF-8")
                    );
                    System.out.println("文件写入成功：" + path);
                    break;

                case "run_command":
                    // 执行命令行（JDK1.8 ProcessBuilder）
                    String command = step.getParams().get("command").toString();

                    // 关键：适配不同系统的 Python 命令
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        // Windows 系统：用 py 或 python.exe 完整路径
                        command = command.replace("python ", "py ");
                    } else if (os.contains("mac") || os.contains("linux")) {
                        // Mac/Linux 系统：用 python3
                        command = command.replace("python ", "python3 ");
                    }

                    ProcessBuilder pb = new ProcessBuilder(command.split(" "));
                    pb.inheritIO(); // 输出重定向到控制台
                    Process process = pb.start();
                    process.waitFor(); // 等待执行完成
                    System.out.println("命令执行完成：" + command);
                    break;

                default:
                    System.err.println("不支持的原子Skill：" + step.getSkill());
            }
        } catch (Exception e) {
            System.err.println("执行原子Skill失败：" + step.getSkill());
            e.printStackTrace();
        }
    }

    /**
     * 根据名称查找Skill（JDK1.8 Stream）
     */
    private Skill findSkillByName(String skillName) {
        return skills.stream()
                .filter(skill -> skillName.equals(skill.getName()))
                .findFirst()
                .orElse(null);
    }

}