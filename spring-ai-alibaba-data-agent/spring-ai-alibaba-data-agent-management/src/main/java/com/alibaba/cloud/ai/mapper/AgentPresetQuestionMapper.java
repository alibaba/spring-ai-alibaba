/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.mapper;
import com.alibaba.cloud.ai.entity.AgentPresetQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * AgentPresetQuestion Mapper Interface
 *
 * @author Alibaba Cloud AI
 */
public interface AgentPresetQuestionMapper extends BaseMapper<AgentPresetQuestion> {

    @Select("""
            SELECT * FROM agent_preset_question
            WHERE agent_id = #{agentId} AND is_active = 1
            ORDER BY sort_order ASC, id ASC
            """)
    List<AgentPresetQuestion> selectByAgentId(@Param("agentId") Long agentId);

    @Insert({
        "INSERT INTO agent_preset_question (agent_id, question, sort_order, is_active, create_time, update_time)",
        "VALUES (#{agentId}, #{question}, #{sortOrder}, #{isActive}, NOW(), NOW())"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AgentPresetQuestion question);

    @Update({
        "UPDATE agent_preset_question",
        "SET question = #{question}, sort_order = #{sortOrder}, is_active = #{isActive}, update_time = NOW()",
        "WHERE id = #{id}"
    })
    int update(AgentPresetQuestion question);

    @Delete("DELETE FROM agent_preset_question WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM agent_preset_question WHERE agent_id = #{agentId}")
    int deleteByAgentId(@Param("agentId") Long agentId);
}
