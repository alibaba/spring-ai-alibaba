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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;

import com.alibaba.cloud.ai.mcp.nacos2.client.transport.LoadbalancedMcpAsyncClient;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.util.Assert;

/**
 * @author yingzi
 * @since 2025/5/6:14:27
 */
public class LoadbalancedAsyncMcpToolCallbackProvider implements ToolCallbackProvider {

	private final List<LoadbalancedMcpAsyncClient> mcpClients;

	private final BiPredicate<McpAsyncClient, McpSchema.Tool> toolFilter;

	public LoadbalancedAsyncMcpToolCallbackProvider(BiPredicate<McpAsyncClient, McpSchema.Tool> toolFilter,
			List<LoadbalancedMcpAsyncClient> mcpClients) {
		Assert.notNull(mcpClients, "MCP clients must not be null");
		Assert.notNull(toolFilter, "Tool filter must not be null");
		this.mcpClients = mcpClients;
		this.toolFilter = toolFilter;
	}

	public LoadbalancedAsyncMcpToolCallbackProvider(List<LoadbalancedMcpAsyncClient> mcpClients) {
		this((mcpClient, tool) -> {
			return true;
		}, mcpClients);
	}

	@Override
	public ToolCallback[] getToolCallbacks() {
		List<ToolCallback> toolCallbackList = new ArrayList();
		Iterator var2 = this.mcpClients.iterator();

		while (var2.hasNext()) {
			LoadbalancedMcpAsyncClient mcpClient = (LoadbalancedMcpAsyncClient) var2.next();
			ToolCallback[] toolCallbacks = (ToolCallback[]) mcpClient.listTools().map((response) -> {
				return (ToolCallback[]) response.tools().stream().filter((tool) -> {
					return this.toolFilter.test(mcpClient.getMcpAsyncClient(), tool);
				}).map((tool) -> {
					return new LoadbalancedAsyncMcpToolCallback(mcpClient, tool);
				}).toArray((x$0) -> {
					return new ToolCallback[x$0];
				});
			}).block();
			this.validateToolCallbacks(toolCallbacks);
			toolCallbackList.addAll(List.of(toolCallbacks));
		}

		return (ToolCallback[]) toolCallbackList.toArray(new ToolCallback[0]);
	}

	private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
		List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
		if (!duplicateToolNames.isEmpty()) {
			throw new IllegalStateException(
					"Multiple tools with the same name (%s)".formatted(String.join(", ", duplicateToolNames)));
		}
	}

}
