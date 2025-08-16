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

import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerCallToolResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerDetail;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpServerGetToolsRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.mcp.McpTool;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.core.base.entity.McpServerEntity;

import java.util.List;

/**
 * Title: CreateDate: 2025/4/24 14:56
 *
 * @author guning.lt
 * @since 1.0.0.3
 **/
public interface McpServerService {

	/**
	 * Creates a new MCP server with the provided configuration.
	 * @param detail Detailed configuration of the MCP server
	 * @return server code
	 */
	String createMcp(McpServerDetail detail);

	/**
	 * Updates an existing MCP server with new configuration.
	 * @param detail Updated configuration for the MCP server
	 */
	void updateMcp(McpServerDetail detail);

	/**
	 * Deletes an MCP server by marking it as deleted.
	 * @param serverCode MCP Server code of the server to delete
	 */
	void deleteMcp(String serverCode);

	/**
	 * Get details of an MCP server by its server code.
	 * @param serverCode Unique identifier of the server
	 * @param needTools Whether to include tool information
	 * @return MCP server details
	 */
	McpServerDetail getMcp(String serverCode, boolean needTools);

	/**
	 * Get a paginated list of MCP servers based on query conditions.
	 * @param query Query request object
	 * @return Paginated list of MCP server details
	 */
	PagingList<McpServerDetail> list(McpQuery query);

	/**
	 * Get a list of MCP servers by their server codes.
	 * @param query Query request object containing server codes
	 * @return List of MCP server details
	 */
	List<McpServerDetail> listByCodes(McpQuery query);

	/**
	 * Get an MCP server entity by its workspace ID and server code.
	 * @param workspaceId Workspace identifier
	 * @param serverCode Server's unique code
	 * @param status Optional status filter
	 * @return MCP server entity
	 */
	McpServerEntity getMcpByCode(String workspaceId, String serverCode, Integer status);

	/**
	 * GET tools available on the specified MCP server.
	 * @param request Request object containing workspace and server information
	 * @return Result containing list of tools or error
	 */
	Result<List<McpTool>> getTools(McpServerGetToolsRequest request);

	/**
	 * Calls a specific tool on the MCP server.
	 * @param request Request object containing tool execution parameters
	 * @return Result containing tool response or error
	 */
	Result<McpServerCallToolResponse> callTool(McpServerCallToolRequest request);

}
