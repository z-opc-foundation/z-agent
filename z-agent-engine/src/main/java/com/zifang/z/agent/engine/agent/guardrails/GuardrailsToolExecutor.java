package com.zifang.z.agent.engine.agent.guardrails;

import com.zifang.z.agent.engine.agent.llm.tool.core.Tool;
import com.zifang.z.agent.engine.agent.llm.tool.core.ToolExecutor;

import java.util.Map;

/**
 * 带 Guardrails 防护的 ToolExecutor 装饰器。
 * <p>
 * 执行流程：
 * 1. 校验参数（ToolParamValidator）
 * 2. 执行工具
 * 3. 净化输出（OutputSanitizer）
 * 4. 记录校验/净化结果
 */
public class GuardrailsToolExecutor implements ToolExecutor {
    private final ToolExecutor delegate;
    private final ToolParamValidator validator;
    private final OutputSanitizer sanitizer;

    public GuardrailsToolExecutor(ToolExecutor delegate) {
        this(delegate, new ToolParamValidator(), new OutputSanitizer());
    }

    public GuardrailsToolExecutor(ToolExecutor delegate,
                                  ToolParamValidator validator, OutputSanitizer sanitizer) {
        this.delegate = delegate;
        this.validator = validator;
        this.sanitizer = sanitizer;
    }

    @Override
    public String execute(Map<String, Object> args) {
        return execute(null, args);
    }

    /**
     * 带工具定义的执行（用于参数校验）
     */
    public String execute(Tool tool, Map<String, Object> args) {
        // 1. 参数校验
        if (tool != null) {
            ValidationResult vr = validator.validate(tool, args);
            if (!vr.isValid()) {
                throw new SecurityException("Tool 参数校验失败: " + vr.getErrorMessage());
            }
        }

        // 2. 执行工具
        String rawResult = delegate.execute(args);

        // 3. 输出净化
        String cleanResult = sanitizer.sanitize(rawResult);

        return cleanResult;
    }

    public ValidationResult validateOnly(Tool tool, Map<String, Object> args) {
        return validator.validate(tool, args);
    }

    public String sanitizeOnly(String output) {
        return sanitizer.sanitize(output);
    }
}