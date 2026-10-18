package com.zifang.z.agent.center.core.agent.flow.node;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.agent.center.core.agent.flow.FlowContext;
import com.zifang.z.agent.center.core.agent.flow.FlowNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板节点 - 数据转换和模板渲染
 * 支持变量替换、循环、条件表达式
 */
public class TemplateNode implements FlowNode {

    private static final Logger log = LogManager.getLogger(TemplateNode.class);

    private final String nodeId;
    private final String template;
    private final Map<String, Object> config = new HashMap<>();

    /**
     * 输出变量名（如果不指定则直接返回渲染结果）
     */
    private String outputVarName;

    public TemplateNode(String nodeId, String template) {
        this.nodeId = nodeId;
        this.template = template;
    }

    @Override
    public String getType() {
        return TYPE_TRANSFORM;
    }

    @Override
    public Object execute(Map<String, Object> inputs, FlowContext context) {
        log.info("TemplateNode[{}] processing template", nodeId);

        try {
            String result = renderTemplate(template, inputs, context);

            // 尝试解析为JSON
            Object parsedResult = tryParseJson(result);

            Map<String, Object> output = new HashMap<>();
            output.put("_raw", result);
            output.put("_parsed", parsedResult);

            // 如果指定了输出变量名，将结果存入context
            if (outputVarName != null) {
                context.setVariable(outputVarName, parsedResult != null ? parsedResult : result);
            }

            return output;

        } catch (Exception e) {
            log.error("TemplateNode[{}] template processing failed: {}", nodeId, e.getMessage(), e);
            throw new RuntimeException("Template processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * 渲染模板
     */
    private String renderTemplate(String template, Map<String, Object> inputs, FlowContext context) {
        if (template == null) {
            return "";
        }

        String result = template;

        // 处理条件表达式 {% if condition %}...{% endif %}
        result = processConditionals(result, inputs, context);

        // 处理循环 {% for item in items %}...{% endfor %}
        result = processLoops(result, inputs, context);

        // 处理变量替换 {variableName}} 或 {expression}}
        result = replaceVariables(result, inputs, context);

        return result;
    }

    /**
     * 处理条件表达式
     * 支持: {% if varName %}...{% endif %}
     * {% if varName == "value" %}...{% endif %}
     * {% if varName > 10 %}...{% endif %}
     * {% if cond1 && cond2 %}...{% endif %}
     */
    private String processConditionals(String template, Map<String, Object> inputs, FlowContext context) {
        Pattern ifPattern = Pattern.compile("\\{%\\s*if\\s+(.+?)s*%\\}(.+?)\\{%\\s*endif\\s*%\\}", Pattern.DOTALL);
        Matcher matcher = ifPattern.matcher(template);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String content = matcher.group(2);

            boolean result = evaluateCondition(condition, inputs, context);
            String replacement = result ? content : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 处理循环
     * 支持: {% for item in items %} {item.name}} {% endfor %}
     */
    private String processLoops(String template, Map<String, Object> inputs, FlowContext context) {
        Pattern forPattern = Pattern.compile("\\{%\\s*for\\s+(\\w+)\\s+in\\s+(\\S+?)s*%\\}(.+?)\\{%\\s*endfor\\s*%\\}", Pattern.DOTALL);
        Matcher matcher = forPattern.matcher(template);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String itemName = matcher.group(1);
            String collectionPath = matcher.group(2);
            String loopContent = matcher.group(3);

            Object collection = resolvePath(collectionPath, inputs, context);
            StringBuilder loopResult = new StringBuilder();

            if (collection instanceof Collection) {
                int index = 0;
                for (Object item : (Collection<?>) collection) {
                    String itemContent = loopContent;
                    // 替换循环变量
                    itemContent = itemContent.replace("{" + itemName + "}}", String.valueOf(item));
                    // 替换索引 {forloop.index}}
                    itemContent = itemContent.replace("{forloop.index}}", String.valueOf(index));
                    itemContent = itemContent.replace("{forloop.first}}", String.valueOf(index == 0));
                    itemContent = itemContent.replace("{forloop.last}}", String.valueOf(index == ((Collection<?>) collection).size() - 1));

                    // 处理嵌套的变量引用
                    if (item instanceof Map) {
                        itemContent = replaceVariables(itemContent, Collections.singletonMap(itemName, item), context);
                    }

                    loopResult.append(itemContent);
                    index++;
                }
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(loopResult.toString()));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 替换变量
     */
    private String replaceVariables(String template, Map<String, Object> inputs, FlowContext context) {
        if (template == null) {
            return "";
        }


        String result = template;

        // 处理 {varName}} 格式
        Pattern varPattern = Pattern.compile("\\{\\{([^}]+?)\\}\\}");
        Matcher matcher = varPattern.matcher(template);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String path = matcher.group(1).trim();
            Object value = resolvePath(path, inputs, context);
            String replacement = value != null ? escapeForReplacement(String.valueOf(value)) : "";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(String condition, Map<String, Object> inputs, FlowContext context) {
        if (condition == null || condition.trim().isEmpty()) {
            return false;
        }

        String cond = condition.trim();

        // 处理逻辑运算符
        if (cond.contains("&&")) {
            String[] parts = cond.split("&&");
            for (String part : parts) {
                if (!evaluateCondition(part.trim(), inputs, context)) {
                    return false;
                }
            }
            return true;
        }

        if (cond.contains("||")) {
            String[] parts = cond.split("\\|\\|");
            for (String part : parts) {
                if (evaluateCondition(part.trim(), inputs, context)) {
                    return true;
                }
            }
            return false;
        }

        // 处理比较操作符
        for (String op : Arrays.asList(">=", "<=", "!=", "==", ">", "<")) {
            int idx = cond.indexOf(op);
            if (idx > 0) {
                String left = cond.substring(0, idx).trim();
                String right = cond.substring(idx + op.length()).trim();
                return compare(left, op, right, inputs, context);
            }
        }

        // 简单变量检查
        Object value = resolvePath(cond, inputs, context);
        return value != null && !value.equals(false) && !value.equals("false") && !value.equals("0") && !value.equals("");
    }

    private boolean compare(String left, String op, String right, Map<String, Object> inputs, FlowContext context) {
        Object leftVal = resolvePath(left, inputs, context);
        Object rightVal = resolveValue(right, inputs, context);

        if (leftVal == null || rightVal == null) {
            return false;
        }

        switch (op) {
            case "==":
                return leftVal.toString().equals(rightVal.toString());
            case "!=":
                return !leftVal.toString().equals(rightVal.toString());
            case ">":
                return compareNumeric(leftVal, rightVal) > 0;
            case "<":
                return compareNumeric(leftVal, rightVal) < 0;
            case ">=":
                return compareNumeric(leftVal, rightVal) >= 0;
            case "<=":
                return compareNumeric(leftVal, rightVal) <= 0;
            default:
                return false;
        }
    }

    private int compareNumeric(Object left, Object right) {
        try {
            double l = Double.parseDouble(left.toString());
            double r = Double.parseDouble(right.toString());
            return Double.compare(l, r);
        } catch (NumberFormatException e) {
            return left.toString().compareTo(right.toString());
        }
    }

    /**
     * 解析路径（如 user.name.items[0].value）
     */
    private Object resolvePath(String path, Map<String, Object> inputs, FlowContext context) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = null;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // 处理数组索引 items[0]
            int bracketIdx = part.indexOf('[');
            if (bracketIdx >= 0) {
                String arrayName = part.substring(0, bracketIdx);
                String indexStr = part.substring(bracketIdx + 1, part.length() - 1);

                // 获取数组
                if (current == null) {
                    current = getValueFromSources(arrayName, inputs, context);
                } else if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(arrayName);
                }

                // 获取索引
                if (current instanceof List) {
                    try {
                        int idx = Integer.parseInt(indexStr.trim());
                        current = ((List<?>) current).get(idx);
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        return null;
                    }
                }
            } else {
                // 普通属性
                if (current == null) {
                    current = getValueFromSources(part, inputs, context);
                } else if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return null;
                }
            }

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    private Object getValueFromSources(String varName, Map<String, Object> inputs, FlowContext context) {
        if (inputs.containsKey(varName)) {
            return inputs.get(varName);
        }
        Object ctxVal = context.getVariable(varName);
        if (ctxVal != null) {
            return ctxVal;
        }
        return null;
    }

    private Object resolveValue(String expr, Map<String, Object> inputs, FlowContext context) {
        // 去除引号
        if ((expr.startsWith("\"") && expr.endsWith("\"")) ||
                (expr.startsWith("'") && expr.endsWith("'"))) {
            return expr.substring(1, expr.length() - 1);
        }

        // 尝试解析为数字
        try {
            if (expr.contains(".")) {
                return Double.parseDouble(expr);
            }
            return Long.parseLong(expr);
        } catch (NumberFormatException e) {
            // 不是数字
        }

        // 作为变量引用
        return resolvePath(expr, inputs, context);
    }

    private String escapeForReplacement(String value) {
        return value.replace("\\", "\\\\")
                .replace("$", "\\$")
                .replace("{", "\\{")
                .replace("}", "\\}");
    }

    private Object tryParseJson(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String trimmed = value.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                return JsonUtil.toPlainValue(JsonUtil.parse(value));
            } catch (Exception e) {
                // 不是有效JSON
            }
        }
        return null;
    }

    // ==================== 配置方法 ====================

    public TemplateNode withOutputVarName(String varName) {
        this.outputVarName = varName;
        return this;
    }

    public TemplateNode withConfig(String key, Object value) {
        this.config.put(key, value);
        return this;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> fullConfig = new HashMap<>(this.config);
        fullConfig.put("template", template);
        fullConfig.put("outputVarName", outputVarName);
        return fullConfig;
    }

    @Override
    public boolean validate() {
        if (template == null || template.trim().isEmpty()) {
            log.error("TemplateNode[{}] template cannot be empty", nodeId);
            return false;
        }
        return true;
    }

    // Getters
    public String getNodeId() {
        return nodeId;
    }

    public String getTemplate() {
        return template;
    }

    public String getOutputVarName() {
        return outputVarName;
    }
}