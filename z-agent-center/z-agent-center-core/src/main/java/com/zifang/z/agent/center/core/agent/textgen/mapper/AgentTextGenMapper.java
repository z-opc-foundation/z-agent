package com.zifang.z.agent.center.core.agent.textgen.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.agent.center.core.agent.textgen.entity.AgentTextGenResult;
import com.zifang.z.agent.center.core.agent.textgen.entity.AgentTextGenTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * FEATURE013 T3.4: Agent 文本生成器 Mapper
 */
@Mapper
public interface AgentTextGenMapper extends BaseMapper<AgentTextGenTask> {

    @Select("SELECT * FROM z_agent_text_gen_result " +
            "WHERE task_id = #{taskId} ORDER BY row_index ASC")
    List<AgentTextGenResult> selectResultsByTaskId(@Param("taskId") Long taskId);
}
