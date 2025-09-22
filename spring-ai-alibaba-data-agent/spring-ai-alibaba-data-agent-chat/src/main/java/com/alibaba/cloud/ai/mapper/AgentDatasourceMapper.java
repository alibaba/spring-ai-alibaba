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

import com.alibaba.cloud.ai.entity.AgentDatasource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Agent Data Source Association Mapper Interface
 *
 * @author Alibaba Cloud AI
 */
@Mapper
public interface AgentDatasourceMapper extends BaseMapper<AgentDatasource> {

	/**
	 * Query associated data sources by agent ID (including data source details)
	 */
	@Select("SELECT ad.*, d.name, d.type, d.host, d.port, d.database_name, "
			+ "d.connection_url, d.username, d.password, d.status, d.test_status, d.description "
			+ "FROM agent_datasource ad " + "LEFT JOIN datasource d ON ad.datasource_id = d.id "
			+ "WHERE ad.agent_id = #{agentId} " + "ORDER BY ad.create_time DESC")
	List<AgentDatasource> selectByAgentIdWithDatasource(@Param("agentId") Integer agentId);

	/**
	 * Query associated data sources by agent ID
	 */
	@Select("SELECT * FROM agent_datasource WHERE agent_id = #{agentId} ORDER BY create_time DESC")
	List<AgentDatasource> selectByAgentId(@Param("agentId") Integer agentId);

	/**
	 * Query association by agent ID and data source ID
	 */
	@Select("SELECT * FROM agent_datasource WHERE agent_id = #{agentId} AND datasource_id = #{datasourceId}")
	AgentDatasource selectByAgentIdAndDatasourceId(@Param("agentId") Integer agentId,
			@Param("datasourceId") Integer datasourceId);

	/**
	 * Disable all data sources for an agent
	 */
	@Update("UPDATE agent_datasource SET is_active = 0 WHERE agent_id = #{agentId}")
	int disableAllByAgentId(@Param("agentId") Integer agentId);

	/**
	 * Count the number of enabled data sources for an agent (excluding the specified data
	 * source)
	 */
	@Select("SELECT COUNT(*) FROM agent_datasource WHERE agent_id = #{agentId} AND is_active = 1 AND datasource_id != #{excludeDatasourceId}")
	int countActiveByAgentIdExcluding(@Param("agentId") Integer agentId,
			@Param("excludeDatasourceId") Integer excludeDatasourceId);

}
