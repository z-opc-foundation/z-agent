package com.zifang.z.agent.engine.agent.skill;

import com.zifang.util.json.JsonUtil;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Skill 体系单元测试：覆盖 Skill / SelectedSkill / ExecutionPlan / SkillLoader。
 */
public class SkillTest {

    // ==================== Skill ====================

    @Test
    public void testSkill_basicGettersAndSetters() {
        Skill s = new Skill();
        s.setName("translate");
        s.setDescription("Translate text to target language");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("text", "string");
        params.put("target_lang", "string");
        s.setParameters(params);
        s.setRequired(Arrays.asList("text", "target_lang"));
        s.setComposite(false);

        assertEquals("translate", s.getName());
        assertEquals("Translate text to target language", s.getDescription());
        assertEquals(2, s.getParameters().size());
        assertEquals(2, s.getRequired().size());
        assertFalse(s.isComposite());
    }

    @Test
    public void testSkill_compositeFlag() {
        Skill s = new Skill();
        s.setName("research_report");
        s.setComposite(true);
        assertTrue(s.isComposite());
    }

    @Test
    public void testSkill_isCompositeJsonProperty() throws Exception {
        // 验证序列化后的 JSON 键名是 is_composite
        Skill s = new Skill();
        s.setName("x");
        s.setComposite(true);
        String json = JsonUtil.toJsonExcludingNulls(s);
        assertTrue("expected is_composite in JSON, got: " + json, json.contains("is_composite"));
    }

    /**
     * 2026-10-04 补：<b>反序列化</b>这个方向才是真正要紧的那个，原来只测了序列化。
     *
     * <p>SkillLoader 用的 JSON 反序列化<b>忽略未知字段</b>，
     * 所以键名对不上时<b>不报错</b>，那个键被静默丢弃。实测：{@code z-agent/skills/} 下
     * 31 个 json 全写 {@code "is_composite"}，而字段名是 {@code isComposite} ⇒ 31 个全部
     * "加载成功"却 isComposite 恒 false，"composite 技能 ⇒ LLM 拆解"那条能力配文件根本点不着。
     *
     * <p>所以这里用<b>加载器真正用的那个 JsonUtil</b> 来测：
     * 只有这样才复现真实路径上的静默丢弃。
     */
    @Test
    public void testSkill_isComposite能从磁盘JSON读回来() throws Exception {
        Skill s = JsonUtil.fromJson("{\"name\":\"x\",\"is_composite\":true}", Skill.class);
        assertTrue("真实加载路径上 is_composite 必须能读回来。读成 false 说明键被静默丢弃，"
                + "而 AgentEngine:45 正靠它决定要不要拆解技能", s.isComposite());
    }

    /** 同一条契约的另一头：序列化回去也得是 is_composite，不能变成 composite。 */
    @Test
    public void testSkill_序列化只出is_composite不出composite() throws Exception {
        Skill s = new Skill();
        s.setName("x");
        s.setComposite(true);
        String json = JsonUtil.toJsonExcludingNulls(s);
        assertTrue("应含 is_composite, got: " + json, json.contains("\"is_composite\""));
        assertFalse("不应同时冒出裸 composite（两个名字并存会让消费方不知道读哪个）, got: " + json,
                json.contains("\"composite\""));
    }

    // ==================== SelectedSkill ====================

    @Test
    public void testSelectedSkill_constructor() {
        Map<String, Object> params = new HashMap<>();
        params.put("text", "hello");
        SelectedSkill ss = new SelectedSkill("translate", params);
        assertEquals("translate", ss.getSkill());
        assertEquals("hello", ss.getParams().get("text"));
    }

    @Test
    public void testSelectedSkill_defaultConstructorAndSetters() {
        SelectedSkill ss = new SelectedSkill();
        ss.setSkill("summarize");
        ss.setParams(Collections.singletonMap("max_words", 100));
        assertEquals("summarize", ss.getSkill());
        assertEquals(100, ss.getParams().get("max_words"));
    }

    // ==================== ExecutionPlan ====================

    @Test
    public void testExecutionPlan_basicGettersAndSetters() {
        ExecutionPlan plan = new ExecutionPlan();
        plan.setSkill("research_report");
        plan.setParams(Collections.singletonMap("topic", "AI"));
        List<SelectedSkill> decompose = Arrays.asList(
                new SelectedSkill("search", Collections.singletonMap("q", "AI")),
                new SelectedSkill("summarize", Collections.singletonMap("max", 500)));
        plan.setDecompose(decompose);

        assertEquals("research_report", plan.getSkill());
        assertEquals("AI", plan.getParams().get("topic"));
        assertEquals(2, plan.getDecompose().size());
    }

    @Test
    public void testExecutionPlan_atomicSkill_hasNoDecompose() {
        ExecutionPlan plan = new ExecutionPlan();
        plan.setSkill("translate");
        plan.setParams(Collections.singletonMap("text", "hi"));
        assertNull(plan.getDecompose());
    }

    // ==================== SkillLoader ====================

    @Test
    public void testSkillLoader_nonExistentDir_returnsEmpty() {
        SkillLoader loader = new SkillLoader();
        List<Skill> skills = loader.loadSkills("/tmp/__nonexistent_dir__");
        assertNotNull(skills);
        assertTrue(skills.isEmpty());
    }

    @Test
    public void testSkillLoader_validDir_loadsAllJsonFiles() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("skill-test-");
        java.nio.file.Files.write(tmp.resolve("translate.json"),
                "{\"name\":\"translate\",\"description\":\"Translate text\"}".getBytes());
        java.nio.file.Files.write(tmp.resolve("summarize.json"),
                "{\"name\":\"summarize\",\"description\":\"Summarize text\",\"is_composite\":false}".getBytes());
        java.nio.file.Files.write(tmp.resolve("README.md"), "# not a skill".getBytes());

        try {
            SkillLoader loader = new SkillLoader();
            List<Skill> skills = loader.loadSkills(tmp.toString());
            assertEquals(2, skills.size());
            assertTrue(skills.stream().anyMatch(s -> "translate".equals(s.getName())));
            assertTrue(skills.stream().anyMatch(s -> "summarize".equals(s.getName())));
        } finally {
            java.nio.file.Files.walk(tmp)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            java.nio.file.Files.deleteIfExists(p);
                        } catch (Exception ignore) {
                        }
                    });
        }
    }

    @Test
    public void testSkillLoader_malformedJson_isSkipped() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("skill-test-");
        java.nio.file.Files.write(tmp.resolve("good.json"),
                "{\"name\":\"good\"}".getBytes());
        java.nio.file.Files.write(tmp.resolve("bad.json"),
                "{not valid json".getBytes());

        try {
            SkillLoader loader = new SkillLoader();
            List<Skill> skills = loader.loadSkills(tmp.toString());
            assertEquals(1, skills.size());
            assertEquals("good", skills.get(0).getName());
        } finally {
            java.nio.file.Files.walk(tmp)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            java.nio.file.Files.deleteIfExists(p);
                        } catch (Exception ignore) {
                        }
                    });
        }
    }

    @Test
    public void testSkillLoader_emptyDir_returnsEmpty() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempDirectory("skill-test-empty-");
        try {
            SkillLoader loader = new SkillLoader();
            List<Skill> skills = loader.loadSkills(tmp.toString());
            assertNotNull(skills);
            assertTrue(skills.isEmpty());
        } finally {
            java.nio.file.Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void testSkillLoader_pathIsFile_notADir() throws Exception {
        java.nio.file.Path file = java.nio.file.Files.createTempFile("not-a-dir", ".json");
        try {
            SkillLoader loader = new SkillLoader();
            List<Skill> skills = loader.loadSkills(file.toString());
            assertTrue(skills.isEmpty());
        } finally {
            java.nio.file.Files.deleteIfExists(file);
        }
    }
}
