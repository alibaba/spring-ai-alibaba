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

package com.alibaba.cloud.ai.mcp.gateway.nacos.provider;

import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolDefinition;
import com.alibaba.cloud.ai.mcp.gateway.core.McpGatewayToolManager;
import com.alibaba.cloud.ai.mcp.gateway.nacos.callback.NacosMcpGatewayToolCallback;
import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.ai.mcp.McpToolUtils;

import java.util.HashMap;
import java.util.Map;

public class NacosMcpSyncGatewayToolsProvider implements McpGatewayToolManager {

	private final McpSyncServer mcpSyncServer;

	private final Map<String, NacosMcpGatewayToolCallback> toolCallbacks = new HashMap<>();

	public NacosMcpSyncGatewayToolsProvider(final McpSyncServer mcpSyncServer) {
		this.mcpSyncServer = mcpSyncServer;
	}

	@Override
	public void addTool(final McpGatewayToolDefinition toolDefinition) {
		try {
			removeTool(toolDefinition.name());
		}
		catch (Exception e) {
			// Ignore exception
		}
		NacosMcpGatewayToolCallback toolCallback = new NacosMcpGatewayToolCallback(toolDefinition);
		toolCallbacks.put(toolDefinition.name(), toolCallback);
		mcpSyncServer.addTool(McpToolUtils.toSyncToolSpecification(toolCallback));
	}

	@Override
	public void removeTool(final String toolName) {
		NacosMcpGatewayToolCallback toolCallback = toolCallbacks.remove(toolName);
		if (toolCallback != null) {
			toolCallback.close();
		}
		mcpSyncServer.removeTool(toolName);
	}

}
