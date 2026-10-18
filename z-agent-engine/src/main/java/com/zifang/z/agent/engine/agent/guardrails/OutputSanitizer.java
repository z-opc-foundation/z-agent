package com.zifang.z.agent.engine.agent.guardrails;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * LLM 输出净化器。
 * <p>
 * 防止：
 * 1. 提示词注入（Prompt Injection）
 * 2. 敏感信息泄露（邮箱、手机号、密码等）
 * 3. XSS/HTML 注入（用于 Web 展示时）
 * 4. 恶意 tool call 构造
 */
public class OutputSanitizer {

    private final List<Pattern> blockPatterns = new ArrayList<>();  // 命中即阻断
    private final List<Pattern> redactPatterns = new ArrayList<>(); // 替换为 [REDACTED]

    public OutputSanitizer() {
        // 默认阻止模式（常见注入）
        blockPatterns.add(Pattern.compile("(?i)ignore.*previous.*instructions", Pattern.CASE_INSENSITIVE));
        blockPatterns.add(Pattern.compile("(?i)disregard.*system", Pattern.CASE_INSENSITIVE));
        blockPatterns.add(Pattern.compile("(?i)you.*are.*now.*(a|an)\\s+", Pattern.CASE_INSENSITIVE));
        blockPatterns.add(Pattern.compile("(?i)\\b\\w+\\s*=\\s*function\\s*\\(", Pattern.CASE_INSENSITIVE)); // JS 函数注入

        // 默认脱敏模式
        redactPatterns.add(Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")); // email
        redactPatterns.add(Pattern.compile("\\b\\d{3}-\\d{4}-\\d{4}\\b")); // 手机号 xxx-xxxx-xxxx
        redactPatterns.add(Pattern.compile("\\b\\d{11,}\\b")); // 长数字（可能是密码/token）
        redactPatterns.add(Pattern.compile("(?i)password[\\s]*:[\\s]*[^\\s,]+")); // password: xxx
        redactPatterns.add(Pattern.compile("(?i)api[\\s_-]?key[\\s]*:[\\s]*[^\\s,]+")); // api_key: xxx
    }

    /**
     * 净化输出文本。
     *
     * @param output 原始 LLM 输出
     * @return 净化后的文本
     */
    public String sanitize(String output) {
        if (output == null || output.isEmpty()) {
            return output;
        }

        String result = output;

        // 1. 敏感信息脱敏
        for (Pattern p : redactPatterns) {
            result = p.matcher(result).replaceAll("[REDACTED]");
        }

        // 2. 注入检测
        for (Pattern p : blockPatterns) {
            if (p.matcher(result).find()) {
                return "[内容已被拦截，检测到潜在注入攻击]";
            }
        }

        // 3. HTML 转义（防止 XSS）
        result = escapeHtml(result);

        return result;
    }

    /**
     * 检测输出是否包含恶意内容（不进行替换，只报告）
     */
    public ValidationResult checkForInjection(String output) {
        if (output == null || output.isEmpty()) {
            return ValidationResult.ok();
        }

        List<String> issues = new ArrayList<>();

        for (Pattern p : blockPatterns) {
            if (p.matcher(output).find()) {
                issues.add("检测到提示词注入模式: " + p.pattern());
            }
        }

        return issues.isEmpty() ? ValidationResult.ok() : ValidationResult.error(issues);
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return null;
        }

        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 添加自定义拦截模式
     */
    public void addBlockPattern(String regex) {
        blockPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
    }

    /**
     * 添加自定义脱敏模式
     */
    public void addRedactPattern(String regex) {
        redactPatterns.add(Pattern.compile(regex));
    }
}