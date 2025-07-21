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

package com.alibaba.cloud.ai.mcp.gateway.core;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * MCP Gateway 工具管理抽象接口 定义了工具的添加、删除、查询等核心功能
 */
public interface McpGatewayToolManager {

	/**
	 * 添加工具
	 * @param toolDefinition 工具定义
	 */
	void addTool(McpGatewayToolDefinition toolDefinition);

	/**
	 * 删除工具
	 * @param toolName 工具名称
	 */
	void removeTool(String toolName);

	/**
	 * 获取所有工具
	 * @return 工具列表
	 */
	List<ToolCallback> getAllTools();

	/**
	 * 根据名称获取工具
	 * @param toolName 工具名称
	 * @return 工具回调
	 */
	ToolCallback getTool(String toolName);

	/**
	 * 检查工具是否存在
	 * @param toolName 工具名称
	 * @return 是否存在
	 */
	boolean hasTool(String toolName);

	/**
	 * 清空所有工具
	 */
	void clearAllTools();

}
