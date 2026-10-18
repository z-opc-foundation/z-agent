package com.zifang.z.agent.center.core.agent.trigger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.agent.center.core.agent.trigger.entity.AgentTrigger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FEATURE013 T3.1: Agent 定时触发 Mapper
 */
@Mapper
public interface AgentTriggerMapper extends BaseMapper<AgentTrigger> {

    @Select("SELECT * FROM z_agent_trigger " +
            "WHERE is_deleted = 0 AND status = 'ACTIVE' " +
            "  AND next_fire_time IS NOT NULL AND next_fire_time <= #{now} " +
            "ORDER BY next_fire_time ASC " +
            "LIMIT 200")
    List<AgentTrigger> selectDueTriggers(@Param("now") LocalDateTime now);

    @Select("SELECT * FROM z_agent_trigger " +
            "WHERE is_deleted = 0 AND status = 'ACTIVE' " +
            "ORDER BY gmt_create DESC " +
            "LIMIT 200")
    List<AgentTrigger> selectActiveTriggers();

    @Update("UPDATE z_agent_trigger " +
            "SET next_fire_time = #{nextFireTime}, gmt_modified = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int updateNextFireTime(@Param("id") Long id, @Param("nextFireTime") LocalDateTime nextFireTime);

    @Update("UPDATE z_agent_trigger " +
            "SET last_fire_time = #{fireTime}, last_fire_status = #{status}, " +
            "    last_fire_message = #{message}, fire_count = fire_count + 1, " +
            "    gmt_modified = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int recordFireResult(@Param("id") Long id, @Param("fireTime") LocalDateTime fireTime, @Param("status") String status, @Param("message") String message);
}
