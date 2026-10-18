package com.zifang.z.agent.center.core.agent.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zifang.z.agent.center.core.agent.conversation.dto.ChatDto;
import com.zifang.z.agent.center.core.agent.conversation.entity.AgentConversation;
import com.zifang.z.agent.center.core.agent.conversation.mapper.AgentConversationMapper;
import com.zifang.z.agent.center.core.agent.conversation.service.AgentConversationService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service("agentConversationSvc")
@Primary
public class AgentConversationServiceImpl extends ServiceImpl<AgentConversationMapper, AgentConversation> implements AgentConversationService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<ChatDto> listRespByInstance(String instanceCode, Integer limit) {
        return listByInstance(instanceCode, limit).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public boolean saveEntity(AgentConversation conversation) {
        if (conversation.getId() == null) {
            conversation.setGmtCreate(LocalDateTime.now());
            return this.baseMapper.insert(conversation) > 0;
        } else {
            return this.baseMapper.updateById(conversation) > 0;
        }
    }

    @Override
    public List<AgentConversation> listByInstance(String instanceCode, int limit) {
        LambdaQueryWrapper<AgentConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConversation::getInstanceCode, instanceCode);
        wrapper.orderByDesc(AgentConversation::getGmtCreate);
        wrapper.last("LIMIT " + limit);
        return this.list(wrapper);
    }

    @Override
    public List<AgentConversation> listByConversationCode(String conversationCode) {
        LambdaQueryWrapper<AgentConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentConversation::getConversationCode, conversationCode);
        wrapper.orderByAsc(AgentConversation::getGmtCreate);
        return this.list(wrapper);
    }

    private ChatDto toDto(AgentConversation c) {
        if (c == null) {
            return null;
        }

        ChatDto dto = new ChatDto();
        BeanUtils.copyProperties(c, dto);
        dto.setGmtCreate(c.getGmtCreate() != null ? c.getGmtCreate().format(DF) : null);
        return dto;
    }
}
