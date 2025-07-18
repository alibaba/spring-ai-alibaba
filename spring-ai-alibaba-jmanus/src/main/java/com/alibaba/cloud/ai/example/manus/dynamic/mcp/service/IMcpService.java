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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.service;

import java.io.IOException;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpConfigRequestVO;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServiceEntity;

/**
 * MCP service interface, providing MCP service management functionality
 */
public interface IMcpService {

	/**
	 * Add MCP server
	 * @param mcpConfig MCP configuration
	 * @throws IOException IO exception
	 */
	void addMcpServer(McpConfigRequestVO mcpConfig) throws IOException;

	/**
	 * Insert or update MCP repository
	 * @param mcpConfigVO MCP configuration VO
	 * @return MCP configuration entity list
	 * @throws IOException IO exception
	 */
	List<McpConfigEntity> insertOrUpdateMcpRepo(McpConfigRequestVO mcpConfigVO) throws IOException;

	/**
	 * Remove MCP server
	 * @param id server ID
	 */
	void removeMcpServer(long id);

	/**
	 * Remove MCP server
	 * @param mcpServerName server name
	 */
	void removeMcpServer(String mcpServerName);

	/**
	 * Get MCP server list
	 * @return MCP configuration entity list
	 */
	List<McpConfigEntity> getMcpServers();

	/**
	 * Get function callbacks
	 * @param planId plan ID
	 * @return MCP service entity list
	 */
	List<McpServiceEntity> getFunctionCallbacks(String planId);

	/**
	 * Close MCP service for the specified plan
	 * @param planId plan ID
	 */
	void close(String planId);

}
