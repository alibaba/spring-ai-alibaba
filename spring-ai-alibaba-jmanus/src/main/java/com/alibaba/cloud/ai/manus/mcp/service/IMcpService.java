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
package com.alibaba.cloud.ai.manus.mcp.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigStatus;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServerRequestVO;
import com.alibaba.cloud.ai.manus.mcp.model.vo.McpServiceEntity;

/**
 * MCP service interface (refactored) defining core business methods for MCP services
 */
public interface IMcpService {

	/**
	 * Batch save MCP server configurations
	 * @param configJson MCP configuration JSON string
	 * @return Configuration entity list
	 * @throws IOException IO exception
	 */
	List<McpConfigEntity> saveMcpServers(String configJson) throws IOException;

	/**
	 * Save single MCP server configuration
	 * @param requestVO MCP server form request
	 * @return Configuration entity
	 * @throws IOException IO exception
	 */
	McpConfigEntity saveMcpServer(McpServerRequestVO requestVO) throws IOException;

	/**
	 * Delete MCP server
	 * @param id MCP server ID
	 */
	void removeMcpServer(long id);

	/**
	 * Delete MCP server
	 * @param mcpServerName MCP server name
	 */
	void removeMcpServer(String mcpServerName);

	/**
	 * Get all MCP server configurations
	 * @return MCP configuration entity list
	 */
	List<McpConfigEntity> getMcpServers();

	/**
	 * Find MCP configuration by ID
	 * @param id MCP configuration ID
	 * @return Optional MCP configuration entity
	 */
	Optional<McpConfigEntity> findById(Long id);

	/**
	 * Get MCP service entity list
	 * @param planId Plan ID
	 * @return MCP service entity list
	 */
	List<McpServiceEntity> getFunctionCallbacks(String planId);

	/**
	 * Close MCP services for specified plan
	 * @param planId Plan ID
	 */
	void close(String planId);

	/**
	 * Update MCP server status
	 * @param id MCP server ID
	 * @param status Target status
	 * @return true if updated successfully, false otherwise
	 */
	boolean updateMcpServerStatus(Long id, McpConfigStatus status);

}
