package com.zifang.z.agent.center.web.api;

import com.zifang.util.core.meta.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Agent 知识检索")
@RestController
@RequestMapping("/api/knowledge-search")
public class KnowledgeSearchController {

    private static final Logger logger = LogManager.getLogger(KnowledgeSearchController.class);

    @Operation(summary = "存活探针")
    @GetMapping("/ping")
    public String ping() {
        return "knowledge-search-pong";
    }

    @Operation(summary = "列出检索历史 (makeApi 友好别名)")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listAlias(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        // 当前为兜底返回; 真实实现待 knowledge 模块完整集成后接入
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 0);
        row.put("keyword", keyword == null ? "" : keyword);
        row.put("hitCount", 0);
        out.add(row);
        return Result.success(out);
    }

    @Operation(summary = "分页别名 (makeApi 友好)")
    @GetMapping("/page")
    public Map<String, Object> pageAlias(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("total", 0);
        resp.put("list", new ArrayList<>());
        resp.put("records", new ArrayList<>());
        resp.put("page", pageNum);
        resp.put("size", pageSize);
        return resp;
    }
}

