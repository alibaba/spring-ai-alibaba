/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.documentation.framework.tutorials.mcp.baizhi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.util.StringUtils;

/**
 * A deliberately narrow MCP tool callback for the Baizhi example.
 *
 * <p>
 * Unlike {@code FunctionToolCallback.inputType(Map.class)} in the generic tutorial, the
 * tool definition is created from the {@link McpSchema.Tool} returned by discovery, so
 * the remote input schema reaches the model unchanged.
 *
 * <p>
 * Result semantics are explicit: text content is joined and returned as the actual tool
 * output; an MCP error result or a transport failure is raised as a
 * {@link ToolExecutionException} instead of being turned into a successful string; an
 * unsupported non-text result is rejected with a clear message instead of being silently
 * dropped.
 *
 * @author Spring AI Alibaba
 */
final class TextOnlyMcpToolCallback implements ToolCallback {

	private static final int ERROR_PREVIEW_LIMIT = 500;

	private final McpSyncClient mcpClient;

	private final McpSchema.Tool tool;

	private final ToolDefinition toolDefinition;

	TextOnlyMcpToolCallback(McpSyncClient mcpClient, McpSchema.Tool tool) {
		this.mcpClient = mcpClient;
		this.tool = tool;
		// Keep the discovered input schema instead of declaring Map.class.
		this.toolDefinition = McpToolUtils.createToolDefinition(tool.name(), tool);
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

	@Override
	public String call(String toolInput) {
		Map<String, Object> arguments;
		try {
			arguments = StringUtils.hasText(toolInput) ? ModelOptionsUtils.jsonToMap(toolInput) : Map.of();
		}
		catch (RuntimeException ex) {
			throw new ToolExecutionException(this.toolDefinition, ex);
		}

		McpSchema.CallToolResult result;
		try {
			result = this.mcpClient.callTool(new McpSchema.CallToolRequest(this.tool.name(), arguments));
		}
		catch (RuntimeException ex) {
			throw new ToolExecutionException(this.toolDefinition, ex);
		}

		if (Boolean.TRUE.equals(result.isError())) {
			throw new ToolExecutionException(this.toolDefinition,
					new IllegalStateException("MCP tool '" + this.tool.name() + "' returned an error result: "
							+ preview(result.content())));
		}

		List<String> textParts = new ArrayList<>();
		List<String> unsupportedTypes = new ArrayList<>();
		for (McpSchema.Content content : result.content()) {
			if (content instanceof McpSchema.TextContent textContent) {
				textParts.add(textContent.text());
			}
			else {
				unsupportedTypes.add(content.type());
			}
		}
		if (!unsupportedTypes.isEmpty()) {
			throw new ToolExecutionException(this.toolDefinition,
					new IllegalStateException("MCP tool '" + this.tool.name()
							+ "' returned unsupported non-text content " + unsupportedTypes
							+ "; this example only handles text results"));
		}
		if (textParts.isEmpty() && result.structuredContent() != null) {
			throw new ToolExecutionException(this.toolDefinition,
					new IllegalStateException("MCP tool '" + this.tool.name()
							+ "' returned only structuredContent; this example only handles text results"));
		}
		return String.join("\n", textParts);
	}

	private static String preview(List<McpSchema.Content> content) {
		String value = String.valueOf(content);
		return value.length() <= ERROR_PREVIEW_LIMIT ? value
				: value.substring(0, ERROR_PREVIEW_LIMIT) + "... (truncated)";
	}

}
