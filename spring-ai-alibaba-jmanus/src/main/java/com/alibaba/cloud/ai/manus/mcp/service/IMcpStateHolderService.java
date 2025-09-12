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

import com.alibaba.cloud.ai.manus.mcp.model.vo.McpState;

/**
 * MCP state holder service interface, managing MCP states
 */
public interface IMcpStateHolderService {

	/**
	 * Get MCP state
	 * @param key state key
	 * @return MCP state
	 */
	McpState getMcpState(String key);

	/**
	 * Set MCP state
	 * @param key state key
	 * @param state MCP state
	 */
	void setMcpState(String key, McpState state);

	/**
	 * Remove MCP state
	 * @param key state key
	 */
	void removeMcpState(String key);

}
