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
package com.alibaba.cloud.ai.manus.mcp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigEntity;
import com.alibaba.cloud.ai.manus.mcp.model.po.McpConfigStatus;

/**
 * McpConfig
 */
@Repository
public interface McpConfigRepository extends JpaRepository<McpConfigEntity, Long> {

	McpConfigEntity findByMcpServerName(String mcpServerName);

	/**
	 * Query MCP configuration list by status
	 * @param status MCP configuration status
	 * @return List of MCP configurations that meet the criteria
	 */
	List<McpConfigEntity> findByStatus(McpConfigStatus status);

}
