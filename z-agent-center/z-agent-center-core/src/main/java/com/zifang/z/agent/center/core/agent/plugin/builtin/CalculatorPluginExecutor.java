package com.zifang.z.agent.center.core.agent.plugin.builtin;

import com.zifang.z.agent.center.core.agent.plugin.executor.BeanPluginExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * FEATURE013 T2.1: 内置插件 #4 — 计算器
 *
 * <pre>
 * 输入: { "expression": "1+2*3" }
 * 输出: { "result": 7 }
 * </pre>
 *
 * <p>支持 + - * / ( ) 四则运算 + 括号. 不支持变量/函数.
 *
 * <p>实现: 简易递归下降解析器, 避免引入第三方数学表达式库. 安全性:
 * 数字字面量 + 操作符 + 括号, 无字符串/函数调用入口.
 */
@Component("calculatorPluginExecutor")
public class CalculatorPluginExecutor implements BeanPluginExecutor {

    private String expr;
    private int pos;

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        String expression = String.valueOf(inputs.get("expression"));
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("expression is empty");
        }
        if (!expression.matches("[0-9+\\-*/().\\s]+")) {
            throw new IllegalArgumentException("非法字符: 只支持数字 + - * / ( ) . 空格");
        }
        this.expr = expression.replaceAll("\\s+", "");
        this.pos = 0;
        double result = parseExpression();
        if (pos < expr.length()) {
            throw new IllegalArgumentException("解析未完成 @pos=" + pos);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("result", result);
        return out;
    }

    private double parseExpression() {
        double value = parseTerm();
        while (pos < expr.length()) {
            char op = expr.charAt(pos);
            if (op == '+' || op == '-') {
                pos++;
                double rhs = parseTerm();
                value = (op == '+') ? value + rhs : value - rhs;
            } else {
                break;
            }
        }
        return value;
    }

    private double parseTerm() {
        double value = parseFactor();
        while (pos < expr.length()) {
            char op = expr.charAt(pos);
            if (op == '*' || op == '/') {
                pos++;
                double rhs = parseFactor();
                if (op == '/') {
                    if (rhs == 0) {
                        throw new ArithmeticException("除零错误");
                    }

                    value = value / rhs;
                } else {
                    value = value * rhs;
                }
            } else {
                break;
            }
        }
        return value;
    }

    private double parseFactor() {
        if (pos >= expr.length()) {
            throw new IllegalStateException("unexpected end @pos=" + pos);
        }
        char c = expr.charAt(pos);
        if (c == '(') {
            pos++;
            double v = parseExpression();
            expect(')');
            return v;
        }
        if (c == '+' || c == '-') {
            pos++;
            return (c == '-') ? -parseFactor() : parseFactor();
        }
        return parseNumber();
    }

    private double parseNumber() {
        int start = pos;
        boolean hasDot = false;
        while (pos < expr.length()) {
            char c = expr.charAt(pos);
            if (c == '.') {
                if (hasDot) {
                    throw new IllegalStateException("重复小数点 @pos=" + pos);
                }

                hasDot = true;
                pos++;
            } else if (c >= '0' && c <= '9') {
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) {
            throw new IllegalStateException("期望数字 @pos=" + pos + ", got '" + expr.charAt(pos) + "'");
        }
        return Double.parseDouble(expr.substring(start, pos));
    }

    private void expect(char c) {
        if (pos >= expr.length() || expr.charAt(pos) != c) {
            throw new IllegalStateException("期望 '" + c + "' @pos=" + pos);
        }
        pos++;
    }
}
