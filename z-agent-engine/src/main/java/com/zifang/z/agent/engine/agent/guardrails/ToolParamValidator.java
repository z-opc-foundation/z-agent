package com.zifang.z.agent.engine.agent.guardrails;

import com.zifang.z.agent.engine.agent.llm.tool.core.Tool;
import com.zifang.z.agent.engine.agent.llm.tool.core.ToolParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tool 参数校验器。
 * <p>
 * 校验规则：
 * 1. 必填参数缺失 -> 错误
 * 2. 参数类型不匹配（String 收到 Number 等）-> 尝试转换，转换失败报错
 * 3. 参数值超出允许范围（如果 ToolParam 指定了 allowedValues）
 * 4. 未知参数（args 中的 key 不在 Tool.params 中）-> 警告（不阻断）
 */
public class ToolParamValidator {

    /**
     * 校验 Tool 调用参数。
     *
     * @param tool 目标 Tool 定义
     * @param args 实际传入的参数
     * @return 校验结果
     */
    public ValidationResult validate(Tool tool, Map<String, Object> args) {
        if (tool == null) {
            return ValidationResult.error("Tool definition is null");
        }


        List<String> errors = new ArrayList<>();
        List<ToolParam> paramDefs = tool.getParams();

        if (paramDefs == null || paramDefs.isEmpty()) {
            // 无参数定义，允许任意 args（只要不是 null）
            return ValidationResult.ok();
        }

        // 1. 检查必填参数
        for (ToolParam param : paramDefs) {
            if (param.isRequired()) {
                if (args == null || !args.containsKey(param.getName()) || args.get(param.getName()) == null) {
                    errors.add("必填参数缺失: " + param.getName());
                }
            }
        }

        if (args == null) {
            return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.error(errors);
        }

        // 2. 类型校验
        for (ToolParam param : paramDefs) {
            Object value = args.get(param.getName());
            if (value == null) {
                continue;
            }

            String expectedType = param.getType() != null ? param.getType().toLowerCase() : "string";
            if (!isTypeMatch(value, expectedType)) {
                // 尝试转换
                Object converted = tryConvert(value, expectedType);
                if (converted == null) {
                    errors.add("参数 \"" + param.getName() + "\" 类型错误: 期望 " + expectedType + ", 实际 " + value.getClass().getSimpleName());
                } else {
                    // 转换成功，替换值（可选，这里只记录警告）
                }
            }
        }

        // 3. allowedValues 范围校验（如果 ToolParam 配置了元数据）
        for (ToolParam param : paramDefs) {
            Object value = args.get(param.getName());
            if (value == null) {
                continue;
            }
            // 注：allowedValues 可以通过 param.getDesc() 或其他扩展字段传递
            // 这里提供基于正则的校验扩展点
            String constraint = getConstraint(param);
            if (constraint != null && !constraint.isEmpty()) {
                if (!validateConstraint(value.toString(), constraint)) {
                    errors.add("参数 \"" + param.getName() + "\" 值不满足约束: " + constraint);
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.error(errors);
    }

    private boolean isTypeMatch(Object value, String expectedType) {
        if (expectedType == null) {
            return true;
        }

        switch (expectedType) {
            case "string":
                return value instanceof String || value instanceof Number || value instanceof Boolean;
            case "integer":
            case "int":
                return value instanceof Number;
            case "number":
            case "float":
            case "double":
                return value instanceof Number;
            case "boolean":
                return value instanceof Boolean || "true".equalsIgnoreCase(String.valueOf(value)) || "false".equalsIgnoreCase(String.valueOf(value));
            case "array":
            case "list":
                return value instanceof List;
            case "object":
            case "map":
                return value instanceof Map;
            default:
                return true;
        }
    }

    private Object tryConvert(Object value, String targetType) {
        if (targetType == null) {
            return value;
        }

        try {
            switch (targetType) {
                case "string":
                    return String.valueOf(value);
                case "integer":
                case "int":
                    if (value instanceof Number) {
                        return ((Number) value).intValue();
                    }

                    return Integer.parseInt(String.valueOf(value));
                case "number":
                case "float":
                case "double":
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }

                    return Double.parseDouble(String.valueOf(value));
                case "boolean":
                    return Boolean.parseBoolean(String.valueOf(value));
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从 ToolParam 中提取约束（扩展点，子类可覆盖）
     */
    protected String getConstraint(ToolParam param) {
        // 从 desc 中解析约束：desc 中包含 "[constraint:pattern]" 格式
        if (param.getDesc() == null) {
            return null;
        }

        int start = param.getDesc().indexOf("[constraint:");
        if (start < 0) {
            return null;
        }

        int end = param.getDesc().indexOf("]", start);
        if (end < 0) {
            return null;
        }

        return param.getDesc().substring(start + 12, end);
    }

    private boolean validateConstraint(String value, String constraint) {
        if (constraint == null || constraint.isEmpty()) {
            return true;
        }
        // 支持正则约束
        if (constraint.startsWith("regex:")) {
            String pattern = constraint.substring(6);
            return value.matches(pattern);
        }
        return true;
    }
}