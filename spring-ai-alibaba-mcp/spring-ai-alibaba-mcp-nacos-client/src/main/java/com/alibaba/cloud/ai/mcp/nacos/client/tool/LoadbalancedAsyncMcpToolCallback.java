package com.alibaba.cloud.ai.mcp.nacos.client.tool;

import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpAsyncClient;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

/**
 * @author yingzi
 * @date 2025/5/6:14:38
 */
public class LoadbalancedAsyncMcpToolCallback implements ToolCallback {

	private final LoadbalancedMcpAsyncClient mcpClients;

	private final McpSchema.Tool tool;

	public LoadbalancedAsyncMcpToolCallback(LoadbalancedMcpAsyncClient mcpClient, McpSchema.Tool tool) {
		this.mcpClients = mcpClient;
		this.tool = tool;
	}

	public ToolDefinition getToolDefinition() {
		return ToolDefinition.builder()
			.name(McpToolUtils.prefixedToolName(this.mcpClients.getClientInfo().name(), this.tool.name()))
			.description(this.tool.description())
			.inputSchema(ModelOptionsUtils.toJsonString(this.tool.inputSchema()))
			.build();
	}

	public String call(String functionInput) {
		Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
		return (String) this.mcpClients.callTool(new McpSchema.CallToolRequest(this.tool.name(), arguments))
			.map((response) -> {
				if (response.isError() != null && response.isError()) {
					throw new IllegalStateException("Error calling tool: " + String.valueOf(response.content()));
				}
				else {
					return ModelOptionsUtils.toJsonString(response.content());
				}
			})
			.block();
	}

	public String call(String toolArguments, ToolContext toolContext) {
		return this.call(toolArguments);
	}

}
