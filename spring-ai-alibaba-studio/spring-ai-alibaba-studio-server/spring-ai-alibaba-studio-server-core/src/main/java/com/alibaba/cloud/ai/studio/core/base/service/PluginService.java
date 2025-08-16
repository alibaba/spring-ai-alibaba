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

import com.alibaba.cloud.ai.studio.runtime.enums.ToolTestStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Plugin;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Tool;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.ToolQuery;

import java.util.List;

/**
 * Service interface for managing plugins and tools. Provides CRUD operations and
 * management functions for plugins and their associated tools.
 *
 * @since 1.0.0.3
 */
public interface PluginService {

	/**
	 * Creates a new plugin
	 * @return the ID of the created plugin
	 */
	String createPlugin(Plugin plugin);

	/**
	 * Updates an existing plugin
	 */
	void updatePlugin(Plugin plugin);

	/**
	 * Deletes a plugin by its ID
	 */
	void deletePlugin(String pluginId);

	/**
	 * Retrieves a plugin by its ID
	 */
	Plugin getPlugin(String pluginId);

	/**
	 * Lists plugins with pagination support
	 */
	PagingList<Plugin> listPlugins(BaseQuery query);

	/**
	 * Creates a new tool
	 * @return the ID of the created tool
	 */
	String createTool(Tool tool);

	/**
	 * Updates an existing tool
	 */
	void updateTool(Tool tool);

	/**
	 * Deletes a tool by its ID
	 */
	void deleteTool(String toolId);

	/**
	 * Retrieves a tool by its ID
	 */
	Tool getTool(String toolId);

	/**
	 * Lists tools with pagination and filtering support
	 */
	PagingList<Tool> listTools(ToolQuery query);

	/**
	 * Retrieves multiple tools by their IDs
	 */
	List<Tool> getTools(List<String> toolIds);

	/**
	 * Updates the enabled status of a tool
	 */
	void updateEnableStatus(String toolId, Boolean enabled);

	/**
	 * Updates the test status of a tool
	 */
	void updateTestStatus(String toolId, ToolTestStatus testStatus);

	/**
	 * Publishes a tool
	 */
	void publishTool(String toolId);

}
