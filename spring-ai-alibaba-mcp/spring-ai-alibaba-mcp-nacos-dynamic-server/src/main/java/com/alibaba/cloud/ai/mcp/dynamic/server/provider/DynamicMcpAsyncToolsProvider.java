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

package com.alibaba.cloud.ai.mcp.dynamic.server.provider;

import com.alibaba.cloud.ai.mcp.dynamic.server.callback.DynamicNacosToolCallback;
import io.modelcontextprotocol.server.McpAsyncServer;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.definition.ToolDefinition;

public class DynamicMcpAsyncToolsProvider implements DynamicMcpToolsProvider {

	private final McpAsyncServer mcpAsyncServer;

	public DynamicMcpAsyncToolsProvider(final McpAsyncServer mcpAsyncServer) {
		this.mcpAsyncServer = mcpAsyncServer;
	}

	@Override
	public void addTool(final ToolDefinition toolDefinition) {
		DynamicNacosToolCallback dynamicNacosToolCallback = new DynamicNacosToolCallback(toolDefinition);
		try {
			removeTool(toolDefinition.name());
		}
		catch (Exception e) {
			// Ignore exception
		}
		// Register the tool with the McpAsyncServer
		mcpAsyncServer.addTool(McpToolUtils.toAsyncToolRegistration(dynamicNacosToolCallback)).block();
	}

	@Override
	public void removeTool(final String toolName) {
		mcpAsyncServer.removeTool(toolName).block();
	}

}
