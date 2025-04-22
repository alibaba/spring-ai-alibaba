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
		mcpAsyncServer.addTool(McpToolUtils.toAsyncToolRegistration(dynamicNacosToolCallback)).block();
	}

	@Override
	public void removeTool(final String toolName) {
		mcpAsyncServer.removeTool(toolName).block();
	}

}
