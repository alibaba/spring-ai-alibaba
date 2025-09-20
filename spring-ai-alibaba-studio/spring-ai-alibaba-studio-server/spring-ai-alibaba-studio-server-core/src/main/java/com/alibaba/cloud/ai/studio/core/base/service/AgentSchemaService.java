/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.base.service;

import com.alibaba.cloud.ai.studio.core.base.entity.AgentSchemaEntity;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Service interface for agent schema CRUD operations.
 *
 * @since 1.0.0.3
 */
public interface AgentSchemaService extends IService<AgentSchemaEntity> {

	/**
	 * Create a new agent schema
	 * @param agentSchemaEntity the agent schema entity to create
	 * @return the created agent schema entity
	 */
	AgentSchemaEntity createAgentSchema(AgentSchemaEntity agentSchemaEntity);

	/**
	 * Update an existing agent schema
	 * @param agentSchemaEntity the agent schema entity to update
	 * @return the updated agent schema entity
	 */
	AgentSchemaEntity updateAgentSchema(AgentSchemaEntity agentSchemaEntity);

	/**
	 * Delete an agent schema by ID
	 * @param id the agent schema ID
	 */
	void deleteAgentSchema(Long id);

	/**
	 * Get an agent schema by ID
	 * @param id the agent schema ID
	 * @return the agent schema entity
	 */
	AgentSchemaEntity getAgentSchemaById(Long id);

	/**
	 * Get agent schemas by workspace ID
	 * @param workspaceId the workspace ID
	 * @return list of agent schema entities
	 */
	List<AgentSchemaEntity> getAgentSchemasByWorkspaceId(String workspaceId);

	/**
	 * Get agent schemas by workspace ID with pagination
	 * @param workspaceId the workspace ID
	 * @param page the page information
	 * @return paginated list of agent schema entities
	 */
	PagingList<AgentSchemaEntity> getAgentSchemasByWorkspaceId(String workspaceId, Page<AgentSchemaEntity> page);

	/**
	 * Get agent schemas by name
	 * @param name the agent name
	 * @param workspaceId the workspace ID
	 * @return list of agent schema entities
	 */
	List<AgentSchemaEntity> getAgentSchemasByName(String name, String workspaceId);

	/**
	 * Enable or disable an agent schema
	 * @param id the agent schema ID
	 * @param enabled whether to enable the agent
	 */
	void setAgentSchemaEnabled(Long id, Boolean enabled);

}
