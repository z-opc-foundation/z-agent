package com.zifang.z.agent.center.core.agent.textgen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.agent.center.core.agent.conversation.entity.AgentConversation;
import com.zifang.z.agent.center.core.agent.instance.entity.AgentInstance;
import com.zifang.z.agent.center.core.agent.instance.service.AgentInstanceService;
import com.zifang.z.agent.center.core.agent.runtime.service.AgentRuntimeService;
import com.zifang.z.agent.center.core.agent.textgen.entity.AgentTextGenResult;
import com.zifang.z.agent.center.core.agent.textgen.entity.AgentTextGenTask;
import com.zifang.z.agent.center.core.agent.textgen.mapper.AgentTextGenMapper;
import com.zifang.z.agent.center.core.agent.textgen.mapper.AgentTextGenResultMapper;
import com.zifang.z.agent.center.core.agent.textgen.service.AgentTextGenService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentTextGenServiceImpl implements AgentTextGenService {

    private static final Logger log = LogManager.getLogger(AgentTextGenServiceImpl.class);

    @Resource
    private AgentTextGenMapper agentTextGenMapper;

    @Resource
    private AgentTextGenResultMapper resultMapper;

    @Resource
    private AgentInstanceService instanceService;

    @Resource
    private AgentRuntimeService agentRuntimeService;

    private static String toJson(Object o) {
        try {
            return JsonUtil.toJsonExcludingNulls(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private static String escapeCsv(List<String> fields) {
        return String.join(",", fields.stream().map(AgentTextGenServiceImpl::escapeCsvVal).toArray(String[]::new));
    }

    private static String escapeCsvVal(String v) {
        if (v == null) {
            return "";
        }

        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    @Override
    public AgentTextGenTask getById(Long id) {
        return id == null ? null : agentTextGenMapper.selectById(id);
    }

    @Override
    public AgentTextGenTask getByCode(String taskCode) {
        if (taskCode == null || taskCode.isEmpty()) {
            return null;
        }
        return agentTextGenMapper.selectOne(new LambdaQueryWrapper<AgentTextGenTask>()
                .eq(AgentTextGenTask::getTaskCode, taskCode)
                .eq(AgentTextGenTask::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    @Override
    public List<AgentTextGenTask> pageList(String keyword, String appCode, String status, int page, int size) {
        if (page < 1) {
            page = 1;
        }

        if (size < 1 || size > 100) {
            size = 20;
        }

        LambdaQueryWrapper<AgentTextGenTask> q = new LambdaQueryWrapper<>();
        q.eq(AgentTextGenTask::getIsDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AgentTextGenTask::getTaskCode, kw)
                    .or().like(AgentTextGenTask::getTaskName, kw));
        }
        if (appCode != null && !appCode.trim().isEmpty()) {
            q.eq(AgentTextGenTask::getAppCode, appCode.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            q.eq(AgentTextGenTask::getStatus, status.trim());
        }
        q.orderByDesc(AgentTextGenTask::getId);
        return agentTextGenMapper.selectPage(new Page<>(page, size), q).getRecords();
    }

    @Override
    public long count(String keyword, String appCode, String status) {
        LambdaQueryWrapper<AgentTextGenTask> q = new LambdaQueryWrapper<>();
        q.eq(AgentTextGenTask::getIsDeleted, 0);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AgentTextGenTask::getTaskCode, kw)
                    .or().like(AgentTextGenTask::getTaskName, kw));
        }
        if (appCode != null && !appCode.trim().isEmpty()) {
            q.eq(AgentTextGenTask::getAppCode, appCode.trim());
        }

        if (status != null && !status.trim().isEmpty()) {
            q.eq(AgentTextGenTask::getStatus, status.trim());
        }

        return agentTextGenMapper.selectCount(q);
    }

    @Override
    public AgentTextGenTask create(AgentTextGenTask task) {
        if (task == null || task.getTaskCode() == null) {
            throw new IllegalArgumentException("taskCode 必填");
        }
        if (getByCode(task.getTaskCode()) != null) {
            throw new IllegalStateException("taskCode 已存在: " + task.getTaskCode());
        }
        if (task.getStatus() == null) task.setStatus("PENDING");

        if (task.getTotalCount() == null) task.setTotalCount(0);

        if (task.getSuccessCount() == null) task.setSuccessCount(0);

        if (task.getFailedCount() == null) task.setFailedCount(0);

        if (task.getOutputFormat() == null) task.setOutputFormat("CSV");

        task.setIsDeleted(0);
        task.setGmtCreate(LocalDateTime.now());
        task.setGmtModified(LocalDateTime.now());
        agentTextGenMapper.insert(task);
        return task;
    }

    @Override
    public boolean delete(Long id) {
        AgentTextGenTask t = agentTextGenMapper.selectById(id);
        if (t == null) {
            return false;
        }

        t.setIsDeleted(1);
        t.setGmtModified(LocalDateTime.now());
        return agentTextGenMapper.updateById(t) > 0;
    }

    @Override
    public List<AgentTextGenResult> listResults(Long taskId) {
        return agentTextGenMapper.selectResultsByTaskId(taskId);
    }

    @Override
    public AgentTextGenTask run(Long taskId) {
        AgentTextGenTask task = agentTextGenMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: id=" + taskId);
        }

        if ("RUNNING".equals(task.getStatus())) {
            throw new IllegalStateException("任务运行中, 不可重复触发");
        }
        // 解析输入
        List<Map<String, Object>> rows;
        try {
            rows = JsonUtil.fromJson(task.getInputData() == null ? "[]" : task.getInputData(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage("input_data 解析失败: " + e.getMessage());
            task.setGmtModified(LocalDateTime.now());
            agentTextGenMapper.updateById(task);
            return task;
        }
        if (rows == null || rows.isEmpty()) {
            task.setStatus("FAILED");
            task.setErrorMessage("input_data 为空");
            task.setGmtModified(LocalDateTime.now());
            agentTextGenMapper.updateById(task);
            return task;
        }
        // 解析模板
        String template = task.getPromptTemplate() == null ? "" : task.getPromptTemplate();
        // 解析变量列表
        List<String> varList = new ArrayList<>();
        if (task.getVariables() != null) {
            try {
                varList = JsonUtil.fromJson(task.getVariables(), new TypeReference<List<String>>() {
                });
            } catch (Exception ignored) {
            }
        }
        // 找实例
        String instanceCode = task.getInstanceCode();
        if (instanceCode == null || instanceCode.isEmpty()) {
            // 找 appCode 下任一 ACTIVE 实例
            List<AgentInstance> insts = instanceService.listAllActive();
            for (AgentInstance i : insts) {
                if (task.getAppCode().equals(i.getAppCode())) {
                    instanceCode = i.getInstanceCode();
                    break;
                }
            }
        }
        if (instanceCode == null) {
            task.setStatus("FAILED");
            task.setErrorMessage("应用 " + task.getAppCode() + " 下无 ACTIVE 实例");
            task.setGmtModified(LocalDateTime.now());
            agentTextGenMapper.updateById(task);
            return task;
        }
        // 开始
        task.setStatus("RUNNING");
        task.setTotalCount(rows.size());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setStartedAt(LocalDateTime.now());
        task.setGmtModified(LocalDateTime.now());
        agentTextGenMapper.updateById(task);

        long taskStart = System.currentTimeMillis();
        int ok = 0, fail = 0;
        StringBuilder csv = new StringBuilder();
        // CSV header: variables + output
        if (!varList.isEmpty()) {
            csv.append(String.join(",", escapeCsv(varList)));
        } else if (!rows.isEmpty() && !rows.get(0).isEmpty()) {
            csv.append(String.join(",", escapeCsv(new ArrayList<>(rows.get(0).keySet()))));
        }
        csv.append(",__output__\n");

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            // 占位符替换
            String prompt = template;
            for (Map.Entry<String, Object> e : row.entrySet()) {
                String v = e.getValue() == null ? "" : String.valueOf(e.getValue());
                prompt = prompt.replace("${" + e.getKey() + "}", v);
            }
            // 调 Agent
            AgentTextGenResult res = new AgentTextGenResult();
            res.setTaskId(taskId);
            res.setRowIndex(i);
            res.setInputJson(toJson(row));
            res.setGmtCreate(LocalDateTime.now());
            long rowStart = System.currentTimeMillis();
            try {
                String uid = "textgen_" + task.getTaskCode();
                String uname = "TextGen [" + task.getTaskName() + "]";
                AgentConversation conv = agentRuntimeService.chat(instanceCode, prompt, uid, uname);
                String out = (conv == null || conv.getAssistantMessage() == null) ? "" : conv.getAssistantMessage();
                String status = "SUCCESS".equals(conv == null ? null : conv.getStatus()) ? "SUCCESS" : "FAILED";
                res.setStatus(status);
                res.setOutputText(out);
                if ("FAILED".equals(status)) {
                    res.setErrorMessage(conv.getErrorMsg() == null ? "未知错误" : conv.getErrorMsg());
                    fail++;
                } else {
                    ok++;
                }
            } catch (Exception e) {
                log.warn("[TextGen] row {} FAIL: {}", i, e.getMessage());
                res.setStatus("FAILED");
                res.setErrorMessage(e.getMessage());
                res.setOutputText("");
                fail++;
            }
            res.setElapsedMs(System.currentTimeMillis() - rowStart);
            resultMapper.insert(res);
            // CSV row
            if (!varList.isEmpty()) {
                List<String> vals = new ArrayList<>();
                for (String v : varList) vals.add(escapeCsvVal(String.valueOf(row.get(v))));
                vals.add(escapeCsvVal(res.getOutputText()));
                csv.append(String.join(",", vals)).append("\n");
            } else {
                List<String> vals = new ArrayList<>();
                for (Object v : row.values()) vals.add(escapeCsvVal(String.valueOf(v)));
                vals.add(escapeCsvVal(res.getOutputText()));
                csv.append(String.join(",", vals)).append("\n");
            }
        }
        long elapsed = System.currentTimeMillis() - taskStart;
        task.setSuccessCount(ok);
        task.setFailedCount(fail);
        task.setStatus(fail == 0 ? "SUCCESS" : (ok == 0 ? "FAILED" : "PARTIAL"));
        task.setElapsedMs(elapsed);
        task.setFinishedAt(LocalDateTime.now());
        task.setOutputCsv(csv.toString());
        task.setGmtModified(LocalDateTime.now());
        agentTextGenMapper.updateById(task);
        log.info("[TextGen] task={} rows={} ok={} fail={} elapsed={}ms",
                task.getTaskCode(), rows.size(), ok, fail, elapsed);
        return task;
    }

    @Override
    public Map<String, Object> createAndRun(String taskName, String appCode, String promptTemplate,
                                            List<Map<String, Object>> rows, String ownerId, String ownerName) {
        String taskCode = "tg_" + RandomUtil.uuidShort(8);
        AgentTextGenTask task = new AgentTextGenTask();
        task.setTaskCode(taskCode);
        task.setTaskName(taskName != null ? taskName : taskCode);
        task.setAppCode(appCode);
        task.setPromptTemplate(promptTemplate);
        task.setOutputFormat("CSV");
        task.setTotalCount(rows == null ? 0 : rows.size());
        try {
            task.setInputData(JsonUtil.toJsonExcludingNulls(rows == null ? new ArrayList<>() : rows));
        } catch (Exception e) {
            throw new RuntimeException("rows 序列化失败: " + e.getMessage(), e);
        }
        // 推断变量名
        if (rows != null && !rows.isEmpty() && !rows.get(0).isEmpty()) {
            try {
                task.setVariables(JsonUtil.toJsonExcludingNulls(new ArrayList<>(rows.get(0).keySet())));
            } catch (Exception ignored) {
            }
        }
        task.setOwnerId(ownerId);
        task.setOwnerName(ownerName);
        create(task);
        run(task.getId());
        Map<String, Object> resp = new HashMap<>();
        resp.put("taskCode", taskCode);
        resp.put("taskId", task.getId());
        resp.put("status", task.getStatus());
        resp.put("totalCount", task.getTotalCount());
        resp.put("successCount", task.getSuccessCount());
        resp.put("failedCount", task.getFailedCount());
        resp.put("elapsedMs", task.getElapsedMs());
        return resp;
    }
}
