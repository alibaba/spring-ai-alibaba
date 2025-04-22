package com.alibaba.cloud.ai.mcp.dynamic.server.provider;

import org.springframework.ai.tool.definition.ToolDefinition;

public interface DynamicMcpToolsProvider {

	void addTool(final ToolDefinition toolDefinition);

	void removeTool(final String toolName);

}
