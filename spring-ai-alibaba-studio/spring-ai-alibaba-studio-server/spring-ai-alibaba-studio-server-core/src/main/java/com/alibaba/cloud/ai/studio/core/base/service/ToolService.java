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

import com.alibaba.cloud.ai.studio.core.base.entity.ToolEntity;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Service interface for tool CRUD operations.
 *
 * @since 1.0.0.3
 */
public interface ToolService extends IService<ToolEntity> {

	/**
	 * Create a new tool
	 * @param toolEntity the tool entity to create
	 * @return the created tool entity
	 */
	ToolEntity createTool(ToolEntity toolEntity);

	/**
	 * Update an existing tool
	 * @param toolEntity the tool entity to update
	 * @return the updated tool entity
	 */
	ToolEntity updateTool(ToolEntity toolEntity);

	/**
	 * Delete a tool by ID
	 * @param id the tool ID
	 */
	void deleteTool(Long id);

	/**
	 * Get a tool by ID
	 * @param id the tool ID
	 * @return the tool entity
	 */
	ToolEntity getToolById(Long id);

	/**
	 * Get tools by workspace ID
	 * @param workspaceId the workspace ID
	 * @return list of tool entities
	 */
	List<ToolEntity> getToolsByWorkspaceId(String workspaceId);

	/**
	 * Get tools by workspace ID with pagination
	 * @param workspaceId the workspace ID
	 * @param page the page information
	 * @return paginated list of tool entities
	 */
	PagingList<ToolEntity> getToolsByWorkspaceId(String workspaceId, Page<ToolEntity> page);

	/**
	 * Get tools by name
	 * @param name the tool name
	 * @param workspaceId the workspace ID
	 * @return list of tool entities
	 */
	List<ToolEntity> getToolsByName(String name, String workspaceId);

	/**
	 * Enable or disable a tool
	 * @param id the tool ID
	 * @param enabled whether to enable the tool
	 */
	void setToolEnabled(Long id, Boolean enabled);

	/**
	 * Get tools by plugin ID
	 * @param pluginId the plugin ID
	 * @param workspaceId the workspace ID
	 * @return list of tool entities
	 */
	List<ToolEntity> getToolsByPluginId(String pluginId, String workspaceId);

}
