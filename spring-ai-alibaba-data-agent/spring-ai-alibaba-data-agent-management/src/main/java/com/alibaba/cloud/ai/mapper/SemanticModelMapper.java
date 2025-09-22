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

import com.alibaba.cloud.ai.entity.SemanticModel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Semantic Model Mapper Interface
 *
 * @author Alibaba Cloud AI
 */
@Mapper
public interface SemanticModelMapper extends BaseMapper<SemanticModel> {

	/**
	 * Query semantic model list by agent ID
	 */
	@Select("SELECT * FROM semantic_model WHERE agent_id = #{agentId} ORDER BY created_time DESC")
	List<SemanticModel> selectByAgentId(@Param("agentId") Long agentId);

	/**
	 * Search semantic models by keyword
	 */
	@Select("SELECT * FROM semantic_model WHERE " + "field_name LIKE CONCAT('%', #{keyword}, '%') OR "
			+ "description LIKE CONCAT('%', #{keyword}, '%') OR " + "synonyms LIKE CONCAT('%', #{keyword}, '%') "
			+ "ORDER BY created_time DESC")
	List<SemanticModel> searchByKeyword(@Param("keyword") String keyword);

	/**
	 * Batch enable fields
	 */
	@Update("UPDATE semantic_model SET status = 1 WHERE id = #{id}")
	int enableById(@Param("id") Long id);

	/**
	 * Batch disable fields
	 */
	@Update("UPDATE semantic_model SET status = 0 WHERE id = #{id}")
	int disableById(@Param("id") Long id);

	/**
	 * Query semantic models by agent ID and enabled status
	 */
	@Select("SELECT * FROM semantic_model WHERE agent_id = #{agentId} AND status = #{enabled} ORDER BY created_time DESC")
	List<SemanticModel> selectByAgentIdAndEnabled(@Param("agentId") Long agentId, @Param("enabled") Boolean enabled);

}
