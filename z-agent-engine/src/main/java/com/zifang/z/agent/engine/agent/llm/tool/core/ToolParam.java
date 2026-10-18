package com.zifang.z.agent.engine.agent.llm.tool.core;

/**
 * Tool parameter definition.
 */
public class ToolParam {

    private String name;
    private String type;
    private boolean required;
    private String desc;

    public ToolParam() {
    }

    public ToolParam(String name, String type, boolean required, String desc) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
