package com.zifang.z.agent.center.core.agent.humaninput.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.agent.center.core.agent.humaninput.entity.AgentFlowHumanInput;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * FEATURE013 T3.5: Human-in-the-Loop Mapper
 */
@Mapper
public interface AgentFlowHumanInputMapper extends BaseMapper<AgentFlowHumanInput> {

    @Select("SELECT * FROM z_agent_flow_human_input " +
            "WHERE status = 'PENDING' " +
            "  AND (assignee = #{assignee} OR assignee IS NULL OR assignee = '') " +
            "ORDER BY priority DESC, gmt_create ASC " +
            "LIMIT #{limit}")
    List<AgentFlowHumanInput> selectPendingByAssignee(@Param("assignee") String assignee,
                                                      @Param("limit") int limit);

    @Select("SELECT * FROM z_agent_flow_human_input " +
            "WHERE status = 'PENDING' " +
            "ORDER BY priority DESC, gmt_create ASC " +
            "LIMIT #{limit}")
    List<AgentFlowHumanInput> selectPendingAll(@Param("limit") int limit);

    @Update("UPDATE z_agent_flow_human_input " +
            "SET status = #{status}, decided_by = #{decidedBy}, decided_at = NOW(), " +
            "    result_json = #{resultJson}, result_text = #{resultText}, comments = #{comments}, " +
            "    gmt_modified = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int decide(@Param("id") Long id, @Param("status") String status,
               @Param("decidedBy") String decidedBy, @Param("resultJson") String resultJson,
               @Param("resultText") String resultText, @Param("comments") String comments);
}
