package com.zifang.z.agent.llm.agent.tools.workspace;

import com.zifang.z.agent.kernel.tool.BaseTool;
import com.zifang.z.agent.kernel.tool.ToolResult;
import com.zifang.z.agent.kernel.tool.ToolArguments;

import java.util.Map;

/**
 * Stub — 文件写入工具基类。
 * z-team GitTrackedTools 引用此类做 instanceof 检查。
 */
public class WriteFileTool extends BaseTool {

    protected WriteFileTool() {
        super("write_file", "stub write file tool");
    }

    @Override
    protected ToolResult doExecute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        return ToolResult.text("stub");
    }
}
