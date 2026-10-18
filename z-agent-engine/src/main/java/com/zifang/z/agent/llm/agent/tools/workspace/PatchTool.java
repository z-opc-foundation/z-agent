package com.zifang.z.agent.llm.agent.tools.workspace;

import com.zifang.z.agent.kernel.tool.BaseTool;
import com.zifang.z.agent.kernel.tool.ToolResult;
import com.zifang.z.agent.kernel.tool.ToolArguments;

import java.util.Map;

/**
 * Stub — 文件补丁工具基类。
 * z-team GitTrackedTools 引用此类做 instanceof 检查。
 */
public class PatchTool extends BaseTool {

    protected PatchTool() {
        super("patch", "stub patch tool");
    }

    @Override
    protected ToolResult doExecute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        return ToolResult.text("stub");
    }
}
