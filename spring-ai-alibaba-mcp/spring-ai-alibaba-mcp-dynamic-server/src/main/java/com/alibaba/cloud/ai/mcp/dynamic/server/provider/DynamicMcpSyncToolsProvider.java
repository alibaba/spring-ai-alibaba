package com.alibaba.cloud.ai.mcp.dynamic.server.provider;

import com.alibaba.cloud.ai.mcp.dynamic.server.callback.DynamicNacosToolCallback;
import com.alibaba.cloud.ai.mcp.dynamic.server.definiation.DynamicNacosToolDefinition;
import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.ai.mcp.McpToolUtils;

public class DynamicMcpSyncToolsProvider implements DynamicMcpToolsProvider {

	private final McpSyncServer mcpSyncServer;

	public DynamicMcpSyncToolsProvider(final McpSyncServer mcpSyncServer) {
		this.mcpSyncServer = mcpSyncServer;
	}

	@Override
	public void addTool(final DynamicNacosToolDefinition toolDefinition) {
		DynamicNacosToolCallback dynamicNacosToolCallback = new DynamicNacosToolCallback(toolDefinition);
		mcpSyncServer.addTool(McpToolUtils.toSyncToolRegistration(dynamicNacosToolCallback));
	}

	@Override
	public void removeTool(final String toolName) {
		mcpSyncServer.removeTool(toolName);
	}

}
