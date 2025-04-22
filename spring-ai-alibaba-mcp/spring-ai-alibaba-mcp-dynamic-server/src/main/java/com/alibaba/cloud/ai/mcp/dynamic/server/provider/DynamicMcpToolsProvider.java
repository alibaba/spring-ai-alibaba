package com.alibaba.cloud.ai.mcp.dynamic.server.provider;

import com.alibaba.cloud.ai.mcp.dynamic.server.definiation.DynamicNacosToolDefinition;

public interface DynamicMcpToolsProvider {

	void addTool(final DynamicNacosToolDefinition toolDefinition);

	void removeTool(final String toolName);

}
