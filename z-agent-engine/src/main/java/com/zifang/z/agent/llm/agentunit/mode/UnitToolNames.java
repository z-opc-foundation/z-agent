package com.zifang.z.agent.llm.agentunit.mode;

import java.util.Arrays;
import java.util.List;

/**
 * Central registry for tool names available per agent mode.
 */
public class UnitToolNames {

    private UnitToolNames() {
    }

    public static List<String> coreNames() {
        return Arrays.asList("read_file", "write_file", "list_dir", "search_code", "run_command", "web_fetch");
    }
}
