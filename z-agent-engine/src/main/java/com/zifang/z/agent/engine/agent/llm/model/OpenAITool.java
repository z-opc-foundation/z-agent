package com.zifang.z.agent.engine.agent.llm.model;

import com.zifang.util.json.annotation.JsonDeserialize;
import com.zifang.util.json.annotation.JsonSerialize;

import java.util.Map;

/**
 * OpenAI tool definition for Chat Completions API.
 */
public class OpenAITool {

    @JsonSerialize(name = "type")
    @JsonDeserialize(name = "type")
    private String type = "function";

    @JsonSerialize(name = "function")
    @JsonDeserialize(name = "function")
    private Object function;

    public OpenAITool() {
    }

    public OpenAITool(Object function) {
        this.function = function;
    }

    public static OpenAITool of(String name, String description, Map<String, Object> parameters) {
        OpenAIToolFunction f = new OpenAIToolFunction();
        f.name = name;
        f.description = description;
        f.parameters = parameters;
        return new OpenAITool(f);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getFunction() {
        return function;
    }

    public void setFunction(Object function) {
        this.function = function;
    }

    public String getFunctionName() {
        if (function instanceof OpenAIToolFunction) {
            return ((OpenAIToolFunction) function).getName();
        } else if (function instanceof FunctionDef) {
            return ((FunctionDef) function).getName();
        }
        return null;
    }

    public static class Builder {
        private String type = "function";
        private Object function;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder function(Object function) {
            this.function = function;
            return this;
        }

        public OpenAITool build() {
            OpenAITool tool = new OpenAITool();
            tool.type = this.type;
            tool.function = this.function;
            return tool;
        }
    }

    public static class OpenAIToolFunction {
        private String name;
        private String description;
        private Map<String, Object> parameters;

        public OpenAIToolFunction() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    public static class FunctionDef {
        private String name;
        private String description;
        private Map<String, Object> parameters;

        public FunctionDef() {
        }

        public FunctionDef(String name, String description, Map<String, Object> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public static class Builder {
            private String name;
            private String description;
            private Map<String, Object> parameters;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder parameters(Map<String, Object> params) {
                this.parameters = params;
                return this;
            }

            public FunctionDef build() {
                FunctionDef f = new FunctionDef();
                f.name = this.name;
                f.description = this.description;
                f.parameters = this.parameters;
                return f;
            }
        }
    }
}
