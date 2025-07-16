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

package com.alibaba.cloud.ai.mcp.nacos.gateway.provider;

import com.alibaba.cloud.ai.mcp.nacos.gateway.callback.DynamicNacosToolCallback;
import com.alibaba.cloud.ai.mcp.nacos.gateway.definition.NacosMcpGatewayToolDefinition;
import io.modelcontextprotocol.server.McpAsyncServer;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;

public class NacosMcpAsyncGatewayToolsProvider implements NacosMcpGatewayToolsProvider {

	private final McpAsyncServer mcpAsyncServer;

	public NacosMcpAsyncGatewayToolsProvider(final McpAsyncServer mcpAsyncServer) {
		this.mcpAsyncServer = mcpAsyncServer;
	}

	@Override
	public void addTool(final NacosMcpGatewayToolDefinition toolDefinition) {
		try {
			removeTool(toolDefinition.name());
		}
		catch (Exception e) {
			// Ignore exception
		}
		ToolCallback toolCallback = new DynamicNacosToolCallback(toolDefinition);
		mcpAsyncServer.addTool(McpToolUtils.toAsyncToolSpecification(toolCallback)).block();
	}

	@Override
	public void removeTool(final String toolName) {
		mcpAsyncServer.removeTool(toolName).block();
	}

}
