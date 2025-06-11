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
package com.alibaba.cloud.ai.graph.plugin;

import java.util.Map;

/**
 * Interface for graph plugins that can be executed by PluginNode. Each plugin must
 * implement this interface to define its behavior.
 */
public interface GraphPlugin {

	/**
	 * Get the unique identifier of the plugin.
	 * @return the plugin id
	 */
	String getId();

	/**
	 * Get the name of the plugin.
	 * @return the plugin name
	 */
	String getName();

	/**
	 * Get the description of the plugin.
	 * @return the plugin description
	 */
	String getDescription();

	/**
	 * Get the input parameters schema of the plugin.
	 * @return the input parameters schema
	 */
	Map<String, Object> getInputSchema();

	/**
	 * Execute the plugin with the given parameters.
	 * @param params the input parameters
	 * @return the execution result
	 * @throws Exception if execution fails
	 */
	Map<String, Object> execute(Map<String, Object> params) throws Exception;

}