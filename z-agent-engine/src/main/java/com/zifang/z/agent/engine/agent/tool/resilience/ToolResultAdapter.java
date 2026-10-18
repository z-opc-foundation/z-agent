package com.zifang.z.agent.engine.agent.tool.resilience;

import com.zifang.z.agent.engine.agent.react.Toolkit.ToolResult;

/**
 * 将 {@link ToolExecutionResult} 适配为 {@link ToolResult}。
 *
 * <p>用于在 ReActAgent 的 {@link com.zifang.z.agent.engine.agent.react.Toolkit} 调用链中，
 * 将弹性封装的执行结果（{@link ToolExecutionResult}，由 ResilientToolExecutor 返回的 String）
 * 转换为 Agent 层级可见的 {@link ToolResult}。
 *
 * <p>转换规则：
 * <ul>
 *   <li>success=true → result=output, error=null</li>
 *   <li>success=false → result=null, error=error.getMessage() 或 "unknown error"</li>
 * </ul>
 */
public final class ToolResultAdapter {

    private ToolResultAdapter() {
        // utility class
    }

    /**
     * 将 ToolExecutionResult 适配为 Toolkit.ToolResult。
     *
     * @param r 执行结果包装（不为 null）
     * @return 对应的 Toolkit.ToolResult
     */
    public static ToolResult adapt(ToolExecutionResult r) {
        if (r == null) {
            return ToolResult.error("null execution result");
        }
        if (r.isSuccess()) {
            return ToolResult.success(r.getOutput());
        } else {
            String errorMsg = (r.getError() != null)
                    ? r.getError().getMessage()
                    : "unknown error";
            return ToolResult.error(errorMsg);
        }
    }
}