package com.zifang.z.agent.center.core.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zifang.util.core.lang.RandomUtil;
import com.zifang.z.agent.center.core.app.entity.AgentAppGroup;
import com.zifang.z.agent.center.core.app.mapper.AgentAppGroupMapper;
import com.zifang.z.agent.center.core.app.service.AgentAppGroupService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AgentAppGroupServiceImpl implements AgentAppGroupService {

    @Resource(name = "agentAppGroupMapper")
    private AgentAppGroupMapper groupMapper;

    @Override
    public List<AgentAppGroup> listTree(String tenantCode) {
        LambdaQueryWrapper<AgentAppGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentAppGroup::getTenantCode, tenantCode != null ? tenantCode : "default")
                .orderByAsc(AgentAppGroup::getSortOrder);
        return groupMapper.selectList(wrapper);
    }

    @Override
    public AgentAppGroup getByCode(String groupCode) {
        return groupMapper.selectOne(new LambdaQueryWrapper<AgentAppGroup>()
                .eq(AgentAppGroup::getGroupCode, groupCode));
    }

    @Override
    public AgentAppGroup create(AgentAppGroup group) {
        if (group.getGroupCode() == null || group.getGroupCode().isEmpty()) {
            group.setGroupCode("grp_" + RandomUtil.uuidShort(12));
        }
        groupMapper.insert(group);
        return group;
    }

    @Override
    public AgentAppGroup update(AgentAppGroup group) {
        groupMapper.updateById(group);
        return group;
    }

    @Override
    public void delete(String groupCode) {
        groupMapper.delete(new LambdaQueryWrapper<AgentAppGroup>()
                .eq(AgentAppGroup::getGroupCode, groupCode));
    }
}