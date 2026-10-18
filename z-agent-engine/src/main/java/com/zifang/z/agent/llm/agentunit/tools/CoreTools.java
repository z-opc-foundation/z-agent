package com.zifang.z.agent.llm.agentunit.tools;

import com.zifang.z.agent.kernel.tool.BaseTool;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core tool factory — provides the default set of tools for agent workspace operations.
 */
public class CoreTools {

    private CoreTools() {
    }

    public static List<BaseTool> forWorkspace(Path workspaceRoot) {
        List<BaseTool> tools = new ArrayList<>();
        // Core tools would be created here
        return Collections.unmodifiableList(tools);
    }
}
