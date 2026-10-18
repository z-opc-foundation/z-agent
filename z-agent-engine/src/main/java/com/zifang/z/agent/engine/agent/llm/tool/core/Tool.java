package com.zifang.z.agent.engine.agent.llm.tool.core;

import java.util.List;

/**
 * Tool definition for function calling.
 */
public class Tool {

    private String name;
    private String desc;
    private List<ToolParam> params;

    public Tool() {
    }

    public Tool(String name, String desc, List<ToolParam> params) {
        this.name = name;
        this.desc = desc;
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<ToolParam> getParams() {
        return params;
    }

    public void setParams(List<ToolParam> params) {
        this.params = params;
    }

    /**
     * Generate tool description text.
     */
    public String toDesc() {
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(name).append("：").append(desc).append("\n");
        if (params != null && !params.isEmpty()) {
            sb.append("  参数：\n");
            for (ToolParam p : params) {
                sb.append("    - ").append(p.getName()).append("（")
                        .append(p.getType()).append(p.isRequired() ? ", 必填" : "").append("）：")
                        .append(p.getDesc()).append("\n");
            }
        }
        return sb.toString();
    }
}
