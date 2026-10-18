package com.zifang.z.agent.center.core.agent.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.agent.center.core.agent.template.entity.AgentTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * FEATURE013 T3.3: Agent 模板市场 Mapper
 */
@Mapper
public interface AgentTemplateMapper extends BaseMapper<AgentTemplate> {

    @Select("SELECT * FROM z_agent_template " +
            "WHERE is_deleted = 0 AND status = 'PUBLISHED' " +
            "ORDER BY is_official DESC, download_count DESC, rating DESC " +
            "LIMIT #{limit}")
    List<AgentTemplate> selectHot(@Param("limit") int limit);

    @Update("UPDATE z_agent_template " +
            "SET download_count = download_count + 1, gmt_modified = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int incrementDownload(@Param("id") Long id);

    @Update("UPDATE z_agent_template " +
            "SET view_count = view_count + 1, gmt_modified = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int incrementView(@Param("id") Long id);
}
