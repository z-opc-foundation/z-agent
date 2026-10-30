package com.zifang.z.agent.center.core.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.wf.kernel.persistence.WorkflowPersistencePlugin;
import com.zifang.util.wf.kernel.persistence.WorkflowSnapshot;
import com.zifang.z.agent.center.core.workflow.entity.AgentWorkflowInstance;
import com.zifang.z.agent.center.core.workflow.mapper.AgentWorkflowInstanceMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DbWorkflowPersistencePlugin implements WorkflowPersistencePlugin {

    private static final Logger log = LogManager.getLogger(DbWorkflowPersistencePlugin.class);

    @Resource(name = "agentWorkflowInstanceMapper")
    private AgentWorkflowInstanceMapper instanceMapper;

    @Override
    public void save(WorkflowSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        LambdaQueryWrapper<AgentWorkflowInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentWorkflowInstance::getProcessId, snapshot.getProcessId());
        wrapper.eq(AgentWorkflowInstance::getIsDeleted, 0);

        AgentWorkflowInstance existing = instanceMapper.selectOne(wrapper);

        try {
            String configJson = snapshot.getWorkflowConfigurationJson();
            String snapJson = JsonUtil.toJsonExcludingNulls(snapshot);

            if (existing != null) {
                existing.setStatus(snapshot.getStatus());
                existing.setWorkflowConfig(configJson);
                existing.setSnapshotData(snapJson);
                existing.setCurrentNodeId(snapshot.getCurrentNodeId());
                existing.setErrorMessage(snapshot.getErrorMessage());
                existing.setUpdatedTime(LocalDateTime.now());
                instanceMapper.updateById(existing);
            } else {
                AgentWorkflowInstance inst = new AgentWorkflowInstance();
                inst.setProcessId(snapshot.getProcessId());
                inst.setStatus(snapshot.getStatus());
                inst.setWorkflowConfig(configJson);
                inst.setSnapshotData(snapJson);
                inst.setCurrentNodeId(snapshot.getCurrentNodeId());
                inst.setErrorMessage(snapshot.getErrorMessage());
                inst.setCreatedTime(LocalDateTime.now());
                inst.setUpdatedTime(LocalDateTime.now());
                instanceMapper.insert(inst);
            }
        } catch (RuntimeException e) {
            log.error("Failed to serialize workflow snapshot for process: {}", snapshot.getProcessId(), e);
        }
    }

    @Override
    public WorkflowSnapshot load(String processId) {
        LambdaQueryWrapper<AgentWorkflowInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentWorkflowInstance::getProcessId, processId);
        wrapper.eq(AgentWorkflowInstance::getIsDeleted, 0);

        AgentWorkflowInstance inst = instanceMapper.selectOne(wrapper);
        if (inst == null || inst.getSnapshotData() == null) {
            return null;
        }

        try {
            WorkflowSnapshot snapshot = JsonUtil.fromJson(inst.getSnapshotData(), WorkflowSnapshot.class);
            // Ensure workflow config is set (may not be in snapshot if it was set separately)
            if (snapshot.getWorkflowConfigurationJson() == null && inst.getWorkflowConfig() != null) {
                snapshot.setWorkflowConfigurationJson(inst.getWorkflowConfig());
            }
            return snapshot;
        } catch (RuntimeException e) {
            log.error("Failed to deserialize workflow snapshot for process: {}", processId, e);
            return null;
        }
    }

    @Override
    public void delete(String processId) {
        LambdaQueryWrapper<AgentWorkflowInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentWorkflowInstance::getProcessId, processId);

        AgentWorkflowInstance inst = instanceMapper.selectOne(wrapper);
        if (inst != null) {
            inst.setIsDeleted(1);
            inst.setUpdatedTime(LocalDateTime.now());
            instanceMapper.updateById(inst);
        }
    }

    @Override
    public List<String> listProcessIds() {
        LambdaQueryWrapper<AgentWorkflowInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentWorkflowInstance::getIsDeleted, 0);
        wrapper.select(AgentWorkflowInstance::getProcessId);
        List<AgentWorkflowInstance> list = instanceMapper.selectList(wrapper);
        List<String> ids = new ArrayList<>();
        for (AgentWorkflowInstance inst : list) {
            ids.add(inst.getProcessId());
        }
        return ids;
    }

    public AgentWorkflowInstance getByProcessId(String processId) {
        LambdaQueryWrapper<AgentWorkflowInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentWorkflowInstance::getProcessId, processId);
        wrapper.eq(AgentWorkflowInstance::getIsDeleted, 0);
        return instanceMapper.selectOne(wrapper);
    }
}
