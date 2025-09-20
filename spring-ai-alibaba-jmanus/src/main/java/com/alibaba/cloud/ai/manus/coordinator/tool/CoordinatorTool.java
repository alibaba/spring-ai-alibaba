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

package com.alibaba.cloud.ai.manus.coordinator.tool;

/**
 * Plan Coordinator Tool POJO
 *
 * Simplified tool class, only contains basic tool information, business logic is handled
 * by CoordinatorService
 */
public class CoordinatorTool {

	/**
	 * Endpoint address
	 */
	private String endpoint;

	/**
	 * Tool name
	 */
	private String toolName;

	/**
	 * Tool description
	 */
	private String toolDescription;

	/**
	 * Tool schema
	 */
	private String toolSchema;

	/**
	 * Get endpoint address
	 * @return Endpoint address
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * Set endpoint address
	 * @param endpoint Endpoint address
	 */
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	/**
	 * Get tool name
	 * @return Tool name
	 */
	public String getToolName() {
		return toolName;
	}

	/**
	 * Set tool name
	 * @param toolName Tool name
	 */
	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	/**
	 * Get tool description
	 * @return Tool description
	 */
	public String getToolDescription() {
		return toolDescription;
	}

	/**
	 * Set tool description
	 * @param toolDescription Tool description
	 */
	public void setToolDescription(String toolDescription) {
		this.toolDescription = toolDescription;
	}

	/**
	 * Get tool schema
	 * @return Tool schema
	 */
	public String getToolSchema() {
		return toolSchema;
	}

	/**
	 * Set tool schema
	 * @param toolSchema Tool schema
	 */
	public void setToolSchema(String toolSchema) {
		this.toolSchema = toolSchema;
	}

}
