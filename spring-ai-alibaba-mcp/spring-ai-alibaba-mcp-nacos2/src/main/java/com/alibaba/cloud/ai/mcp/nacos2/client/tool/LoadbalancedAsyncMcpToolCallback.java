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

package com.alibaba.cloud.ai.mcp.nacos2.client.tool;

import com.alibaba.cloud.ai.mcp.nacos2.client.transport.LoadbalancedMcpAsyncClient;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Map;

/**
 * @author yingzi
 * @since 2025/5/6:14:38
 */
public class LoadbalancedAsyncMcpToolCallback implements ToolCallback {

	private final LoadbalancedMcpAsyncClient mcpClient;

	private final McpSchema.Tool tool;

	public LoadbalancedAsyncMcpToolCallback(LoadbalancedMcpAsyncClient mcpClient, McpSchema.Tool tool) {
		this.mcpClient = mcpClient;
		this.tool = tool;
	}

	public ToolDefinition getToolDefinition() {
		return ToolDefinition.builder()
			.name(McpToolUtils.prefixedToolName(this.mcpClient.getServiceName(), this.tool.name()))
			.description(this.tool.description())
			.inputSchema(ModelOptionsUtils.toJsonString(this.tool.inputSchema()))
			.build();
	}

	public String call(String functionInput) {
		Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
		return (String) this.mcpClient.callTool(new McpSchema.CallToolRequest(this.tool.name(), arguments))
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
