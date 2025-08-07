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

package com.alibaba.cloud.ai.example.manus.coordinator.tool;

/**
 * 计划协调工具 POJO
 *
 * 简化的工具类，只包含基本的工具信息，业务逻辑由 CoordinatorService 处理
 */
public class CoordinatorTool {

	/**
	 * 端点地址
	 */
	private String endpoint;

	/**
	 * 工具名称
	 */
	private String toolName = "coordinator";

	/**
	 * 工具描述
	 */
	private String toolDescription = "计划协调工具 - 执行计划模板并返回结果";

	/**
	 * 工具Schema
	 */
	private String toolSchema = """
			{
				"$schema": "http://json-schema.org/draft-07/schema#",
				"type": "object",
				"properties": {
					"planTemplateId": {
						"type": "string",
						"description": "计划模板ID"
					},
					"parameters": {
						"type": "object",
						"description": "执行参数"
					}
				},
				"required": ["planTemplateId"]
			}
			""";

	/**
	 * 获取端点地址
	 * @return 端点地址
	 */
	public String getEndpoint() {
		return endpoint;
	}

	/**
	 * 设置端点地址
	 * @param endpoint 端点地址
	 */
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	/**
	 * 获取工具名称
	 * @return 工具名称
	 */
	public String getToolName() {
		return toolName;
	}

	/**
	 * 设置工具名称
	 * @param toolName 工具名称
	 */
	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	/**
	 * 获取工具描述
	 * @return 工具描述
	 */
	public String getToolDescription() {
		return toolDescription;
	}

	/**
	 * 设置工具描述
	 * @param toolDescription 工具描述
	 */
	public void setToolDescription(String toolDescription) {
		this.toolDescription = toolDescription;
	}

	/**
	 * 获取工具Schema
	 * @return 工具Schema
	 */
	public String getToolSchema() {
		return toolSchema;
	}

	/**
	 * 设置工具Schema
	 * @param toolSchema 工具Schema
	 */
	public void setToolSchema(String toolSchema) {
		this.toolSchema = toolSchema;
	}

}
