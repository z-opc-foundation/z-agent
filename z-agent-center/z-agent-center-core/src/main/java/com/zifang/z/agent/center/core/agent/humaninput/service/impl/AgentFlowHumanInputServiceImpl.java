package com.zifang.z.agent.center.core.agent.humaninput.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.agent.center.core.agent.humaninput.entity.AgentFlowHumanInput;
import com.zifang.z.agent.center.core.agent.humaninput.mapper.AgentFlowHumanInputMapper;
import com.zifang.z.agent.center.core.agent.humaninput.service.AgentFlowHumanInputService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FEATURE013 T3.5: Human-in-the-Loop Service 实现
 *
 * <p>提供"暂停-等待"语义的 PENDING 请求生命周期管理:
 * <ul>
 *     <li>createPending: Flow 中 HumanInputNode 调用, 落 PENDING 行</li>
 *     <li>listPending: 收件箱 (按 assignee 过滤或全部)</li>
 *     <li>listByExecution: 流程回溯 (列出某次 execution 的所有请求, 含已决策)</li>
 *     <li>decide: 用户在 UI 上点击通过/拒绝后, 原子更新 (CAS: status = 'PENDING' → APPROVED/REJECTED)</li>
 * </ul>
 */
@Service
public class AgentFlowHumanInputServiceImpl implements AgentFlowHumanInputService {

    private static final Logger log = LogManager.getLogger(AgentFlowHumanInputServiceImpl.class);

    @Resource
    private AgentFlowHumanInputMapper mapper;

    @Override
    public AgentFlowHumanInput getById(Long id) {
        if (id == null) {
            return null;
        }

        return mapper.selectById(id);
    }

    @Override
    public AgentFlowHumanInput getByCode(String requestCode) {
        if (requestCode == null || requestCode.isEmpty()) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<AgentFlowHumanInput>()
                .eq(AgentFlowHumanInput::getRequestCode, requestCode)
                .last("LIMIT 1"));
    }

    @Override
    public AgentFlowHumanInput createPending(AgentFlowHumanInput request) {
        if (request == null) {
            throw new IllegalArgumentException("request 不能为空");
        }
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("prompt 必填 (要展示给审批人)");
        }
        // requestCode 缺省自动生成
        if (request.getRequestCode() == null || request.getRequestCode().trim().isEmpty()) {
            String code = "hi_" + RandomUtil.uuidShort(12);
            request.setRequestCode(code);
        } else {
            // 唯一性校验
            if (getByCode(request.getRequestCode()) != null) {
                throw new IllegalStateException("requestCode 已存在: " + request.getRequestCode());
            }
        }
        // 默认值
        if (request.getStatus() == null) request.setStatus("PENDING");

        if (request.getPriority() == null) request.setPriority("NORMAL");

        LocalDateTime now = LocalDateTime.now();
        request.setGmtCreate(now);
        request.setGmtModified(now);
        if (request.getId() == null) {
            mapper.insert(request);
        } else {
            mapper.updateById(request);
        }
        log.info("[HumanInput] createPending id={} requestCode={} flow={} node={} assignee={} priority={}",
                request.getId(), request.getRequestCode(), request.getFlowCode(),
                request.getNodeId(), request.getAssignee(), request.getPriority());
        return request;
    }

    @Override
    public List<AgentFlowHumanInput> listPending(String assignee, int limit) {
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }

        if (assignee == null || assignee.trim().isEmpty()) {
            return mapper.selectPendingAll(limit);
        }
        return mapper.selectPendingByAssignee(assignee.trim(), limit);
    }

    @Override
    public List<AgentFlowHumanInput> listByExecution(String executionId) {
        if (executionId == null || executionId.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<AgentFlowHumanInput>()
                .eq(AgentFlowHumanInput::getExecutionId, executionId)
                .orderByAsc(AgentFlowHumanInput::getGmtCreate));
    }

    @Override
    public AgentFlowHumanInput decide(Long id, String status, String decidedBy,
                                      String resultJson, String resultText, String comments) {
        if (id == null) {
            throw new IllegalArgumentException("id 必填");
        }

        if (status == null) {
            throw new IllegalArgumentException("status 必填");
        }

        String upper = status.toUpperCase();
        if (!("APPROVED".equals(upper) || "REJECTED".equals(upper) || "CANCELLED".equals(upper))) {
            throw new IllegalArgumentException("status 必须是 APPROVED / REJECTED / CANCELLED, 当前: " + status);
        }
        if (decidedBy == null || decidedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("decidedBy 必填");
        }
        int rows = mapper.decide(id, upper, decidedBy.trim(),
                resultJson == null ? "" : resultJson,
                resultText == null ? "" : resultText,
                comments == null ? "" : comments);
        if (rows == 0) {
            // 状态不是 PENDING (可能已被决策或取消)
            AgentFlowHumanInput cur = mapper.selectById(id);
            if (cur == null) {
                throw new IllegalArgumentException("请求不存在: id=" + id);
            }
            throw new IllegalStateException("请求已决策, 当前状态: " + cur.getStatus() + ", 不能重复决策");
        }
        log.info("[HumanInput] decide id={} status={} decidedBy={} resultText.len={}",
                id, upper, decidedBy, resultText == null ? 0 : resultText.length());
        return mapper.selectById(id);
    }
}
