package com.zifang.z.agent.center.core.agent.textgen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.agent.center.core.agent.textgen.entity.AgentTextGenResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * FEATURE013 T3.4: Agent 文本生成器 每行结果 Mapper
 */
@Mapper
public interface AgentTextGenResultMapper extends BaseMapper<AgentTextGenResult> {
}
