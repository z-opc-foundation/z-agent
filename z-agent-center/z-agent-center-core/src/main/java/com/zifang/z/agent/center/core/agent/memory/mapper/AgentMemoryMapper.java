package com.zifang.z.agent.center.core.agent.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.agent.center.core.agent.memory.entity.AgentMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * FEATURE013 T2.3: Agent 长期记忆 Mapper.
 */
@Mapper
public interface AgentMemoryMapper extends BaseMapper<AgentMemory> {

    /**
     * 按 user + (app or global) 检索 Top-N 记忆 (按 importance + decay_score 复合排序).
     * 简版: 仅 MySQL LIKE 检索关键词, 后续可接向量 / FULLTEXT.
     */
    @Select("SELECT * FROM z_agent_memory " +
            "WHERE is_deleted = 0 " +
            "  AND user_id = #{userId} " +
            "  AND (app_code = #{appCode} OR app_code IS NULL OR app_code = '') " +
            "  AND (decay_score > 0.05) " +
            "  AND (keywords LIKE CONCAT('%', #{query}, '%') OR content LIKE CONCAT('%', #{query}, '%') OR #{query} = '' OR #{query} IS NULL) " +
            "ORDER BY (importance * decay_score) DESC, gmt_create DESC " +
            "LIMIT #{limit}")
    List<AgentMemory> searchRelevant(@Param("userId") String userId,
                                     @Param("appCode") String appCode,
                                     @Param("query") String query,
                                     @Param("limit") int limit);

    /**
     * 按 user + app 拉取所有未删除记忆 (管理后台用).
     */
    @Select("SELECT * FROM z_agent_memory " +
            "WHERE is_deleted = 0 " +
            "  AND user_id = #{userId} " +
            "  AND (app_code = #{appCode} OR (app_code IS NULL AND #{appCode} = '')) " +
            "ORDER BY gmt_create DESC " +
            "LIMIT #{limit}")
    List<AgentMemory> listByUser(@Param("userId") String userId,
                                 @Param("appCode") String appCode,
                                 @Param("limit") int limit);

    /**
     * 周期衰减: 把所有 decay_score *= factor, clamp 到 [0, 1].
     */
    @Update("UPDATE z_agent_memory " +
            "SET decay_score = GREATEST(0, LEAST(1, decay_score * #{factor})), " +
            "    gmt_modified = NOW() " +
            "WHERE is_deleted = 0")
    int decayAll(@Param("factor") float factor);

    /**
     * 命中后刷新: decay_score 重置到 1, access_count +1, last_accessed_at = NOW().
     */
    @Update("UPDATE z_agent_memory " +
            "SET decay_score = 1.0, " +
            "    access_count = access_count + 1, " +
            "    last_accessed_at = NOW(), " +
            "    gmt_modified = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int touchOnHit(@Param("id") Long id);

    /**
     * 软删.
     */
    @Update("UPDATE z_agent_memory SET is_deleted = 1, gmt_modified = NOW() WHERE id = #{id}")
    int softDelete(@Param("id") Long id);
}
