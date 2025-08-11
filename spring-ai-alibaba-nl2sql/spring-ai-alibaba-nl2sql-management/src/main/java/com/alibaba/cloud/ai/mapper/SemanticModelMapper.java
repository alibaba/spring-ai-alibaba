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
 * 语义模型 Mapper 接口
 *
 * @author Alibaba Cloud AI
 */
@Mapper
public interface SemanticModelMapper extends BaseMapper<SemanticModel> {

	/**
	 * 根据智能体ID查询语义模型列表
	 */
	@Select("SELECT * FROM semantic_model WHERE agent_id = #{agentId} ORDER BY created_time DESC")
	List<SemanticModel> selectByAgentId(@Param("agentId") Long agentId);

	/**
	 * 根据关键词搜索语义模型
	 */
	@Select("SELECT * FROM semantic_model WHERE " + "agent_field_name LIKE CONCAT('%', #{keyword}, '%') OR "
			+ "field_description LIKE CONCAT('%', #{keyword}, '%') OR "
			+ "field_synonyms LIKE CONCAT('%', #{keyword}, '%') " + "ORDER BY created_time DESC")
	List<SemanticModel> searchByKeyword(@Param("keyword") String keyword);

	/**
	 * 批量启用字段
	 */
	@Update("UPDATE semantic_model SET enabled = 1 WHERE id = #{id}")
	int enableById(@Param("id") Long id);

	/**
	 * 批量禁用字段
	 */
	@Update("UPDATE semantic_model SET enabled = 0 WHERE id = #{id}")
	int disableById(@Param("id") Long id);

	/**
	 * 根据智能体ID和启用状态查询语义模型
	 */
	@Select("SELECT * FROM semantic_model WHERE agent_id = #{agentId} AND enabled = #{enabled} ORDER BY created_time DESC")
	List<SemanticModel> selectByAgentIdAndEnabled(@Param("agentId") Long agentId, @Param("enabled") Boolean enabled);

}