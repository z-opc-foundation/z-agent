package com.zifang.z.agent.center.core.agent.flow.node;

import com.zifang.z.agent.center.core.agent.flow.FlowContext;
import com.zifang.z.agent.center.core.agent.flow.FlowNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 条件节点 - 根据条件表达式选择执行分支
 * 支持多个条件分支和默认分支
 */
public class ConditionNode implements FlowNode {

    private static final Logger log = LogManager.getLogger(ConditionNode.class);

    private final String nodeId;
    private final Map<String, String> conditions = new LinkedHashMap<>();  // branchName -> expression
    private final Map<String, Object> config = new HashMap<>();
    private String defaultBranch;
    /**
     * 表达式求值器
     */
    private ExpressionEvaluator evaluator;

    public ConditionNode(String nodeId) {
        this.nodeId = nodeId;
    }

    public ConditionNode(String nodeId, ExpressionEvaluator evaluator) {
        this.nodeId = nodeId;
        this.evaluator = evaluator;
    }

    @Override
    public String getType() {
        return TYPE_CONDITION;
    }

    @Override
    public Object execute(Map<String, Object> inputs, FlowContext context) {
        log.info("ConditionNode[{}] evaluating conditions", nodeId);

        // 获取表达式求值器
        if (evaluator == null) {
            evaluator = new DefaultExpressionEvaluator();
        }

        // 评估每个条件，找到第一个满足的分支
        for (Map.Entry<String, String> entry : conditions.entrySet()) {
            String branchName = entry.getKey();
            String expression = entry.getValue();

            try {
                boolean result = evaluator.evaluate(expression, inputs, context);
                if (result) {
                    log.info("ConditionNode[{}] matched branch: {} (expression: {})", nodeId, branchName, expression);
                    context.setVariable("_condition_branch", branchName);
                    return createBranchResult(branchName, true);
                }
            } catch (Exception e) {
                log.warn("ConditionNode[{}] failed to evaluate expression '{}': {}", nodeId, expression, e.getMessage());
            }
        }

        // 返回默认分支
        if (defaultBranch != null) {
            log.info("ConditionNode[{}] using default branch: {}", nodeId, defaultBranch);
            context.setVariable("_condition_branch", defaultBranch);
            return createBranchResult(defaultBranch, false);
        }

        log.warn("ConditionNode[{}] no branch matched and no default branch configured", nodeId);
        return createBranchResult(null, false);
    }

    private Map<String, Object> createBranchResult(String branchName, boolean matched) {
        Map<String, Object> result = new HashMap<>();
        result.put("branch", branchName);
        result.put("matched", matched);
        return result;
    }

    /**
     * 添加条件分支
     */
    public ConditionNode addCondition(String branchName, String expression) {
        this.conditions.put(branchName, expression);
        return this;
    }

    /**
     * 设置默认分支
     */
    public ConditionNode withDefaultBranch(String branchName) {
        this.defaultBranch = branchName;
        return this;
    }

    /**
     * 设置自定义表达式求值器
     */
    public ConditionNode withEvaluator(ExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
        return this;
    }

    public ConditionNode withConfig(String key, Object value) {
        this.config.put(key, value);
        return this;
    }

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> fullConfig = new HashMap<>(this.config);
        fullConfig.put("conditions", new LinkedHashMap<>(conditions));
        fullConfig.put("defaultBranch", defaultBranch);
        return fullConfig;
    }

    @Override
    public boolean validate() {
        if (conditions.isEmpty() && defaultBranch == null) {
            log.error("ConditionNode[{}] must have at least one condition or a default branch", nodeId);
            return false;
        }
        return true;
    }

    // Getters
    public String getNodeId() {
        return nodeId;
    }

    public Map<String, String> getConditions() {
        return new LinkedHashMap<>(conditions);
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    // ==================== 表达式求值器接口 ====================

    /**
     * 表达式求值器接口
     */
    public interface ExpressionEvaluator {
        boolean evaluate(String expression, Map<String, Object> inputs, FlowContext context);
    }

    /**
     * 默认表达式求值器 - 支持简单的EL表达式
     * 支持的操作符: ==, !=, >, <, >=, <=, &&, ||, !, contains, startsWith, endsWith
     */
    private static class DefaultExpressionEvaluator implements ExpressionEvaluator {

        @Override
        public boolean evaluate(String expression, Map<String, Object> inputs, FlowContext context) {
            if (expression == null || expression.trim().isEmpty()) {
                return false;
            }

            String expr = expression.trim();

            // 处理逻辑运算符
            if (expr.contains("&&")) {
                String[] parts = expr.split("&&");
                for (String part : parts) {
                    if (!evaluate(part.trim(), inputs, context)) {
                        return false;
                    }
                }
                return true;
            }

            if (expr.contains("||")) {
                String[] parts = expr.split("\\|\\|");
                for (String part : parts) {
                    if (evaluate(part.trim(), inputs, context)) {
                        return true;
                    }
                }
                return false;
            }

            // 处理 NOT
            if (expr.startsWith("!")) {
                return !evaluate(expr.substring(1).trim(), inputs, context);
            }

            // 处理比较操作符
            for (String op : Arrays.asList(">=", "<=", "!=", "==", ">", "<", "contains", "startsWith", "endsWith")) {
                int idx = expr.indexOf(op);
                if (idx > 0) {
                    String left = expr.substring(0, idx).trim();
                    String right = expr.substring(idx + op.length()).trim();
                    return compare(left, op, right, inputs, context);
                }
            }

            // 简单变量引用
            Object value = resolveValue(expr, inputs, context);
            return value != null && !value.equals(false) && !value.equals("false") && !value.equals("0");
        }

        private boolean compare(String left, String op, String right, Map<String, Object> inputs, FlowContext context) {
            Object leftVal = resolveValue(left, inputs, context);
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
                case "contains":
                    return leftVal.toString().contains(rightVal.toString());
                case "startsWith":
                    return leftVal.toString().startsWith(rightVal.toString());
                case "endsWith":
                    return leftVal.toString().endsWith(rightVal.toString());
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

        private Object resolveValue(String expr, Map<String, Object> inputs, FlowContext context) {
            // 去除引号
            if ((expr.startsWith("\"") && expr.endsWith("\"")) ||
                    (expr.startsWith("'") && expr.endsWith("'"))) {
                return expr.substring(1, expr.length() - 1);
            }

            // 尝试从inputs获取
            if (inputs.containsKey(expr)) {
                return inputs.get(expr);
            }

            // 尝试从context获取
            Object ctxVal = context.getVariable(expr);
            if (ctxVal != null) {
                return ctxVal;
            }

            // 尝试作为嵌套路径解析 (e.g., "user.name")
            if (expr.contains(".")) {
                String[] parts = expr.split("\\.", 2);
                Object val = inputs.containsKey(parts[0]) ? inputs.get(parts[0]) : context.getVariable(parts[0]);
                if (val instanceof Map) {
                    return ((Map<?, ?>) val).get(parts[1]);
                }
            }

            // 返回字符串本身（作为字面量）
            return expr;
        }
    }
}