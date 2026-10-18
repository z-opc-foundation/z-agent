package com.zifang.z.agent.llm.builtins.extensions.skills;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Catalog skill registry — discovers and manages agent skills from the filesystem.
 */
public class CatalogSkillRegistry {

    private final Path skillsRoot;
    private final List<Object> skills = new ArrayList<>();

    public CatalogSkillRegistry(Path skillsRoot) {
        this.skillsRoot = skillsRoot;
    }

    public List<Object> listSkills() {
        return Collections.unmodifiableList(skills);
    }

    public Object findSkill(String name) {
        for (Object skill : skills) {
            if (name.equals(skill.toString())) {
                return skill;
            }
        }
        return null;
    }
}
