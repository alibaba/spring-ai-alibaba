package com.alibaba.cloud.ai.mcp.dynamic.server.provider;

import com.alibaba.cloud.ai.mcp.dynamic.server.callback.DynamicNacosToolCallback;
import com.alibaba.cloud.ai.mcp.dynamic.server.definiation.DynamicNacosToolDefinition;
import io.modelcontextprotocol.server.McpAsyncServer;
import org.springframework.ai.mcp.McpToolUtils;

public class DynamicMcpAsyncToolsProvider implements DynamicMcpToolsProvider {

	private final McpAsyncServer mcpAsyncServer;

	public DynamicMcpAsyncToolsProvider(final McpAsyncServer mcpAsyncServer) {
		this.mcpAsyncServer = mcpAsyncServer;
	}

	@Override
	public void addTool(final DynamicNacosToolDefinition toolDefinition) {
		DynamicNacosToolCallback dynamicNacosToolCallback = new DynamicNacosToolCallback(toolDefinition);
		mcpAsyncServer.addTool(McpToolUtils.toAsyncToolRegistration(dynamicNacosToolCallback)).block();
	}

	@Override
	public void removeTool(final String toolName) {
		mcpAsyncServer.removeTool(toolName).block();
	}

}
