/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * MCP 节点示例
 * 演示如何为指定节点分配 MCP 工具
 */
public class McpNodeExample {

	public static CompiledGraph createGraph(ChatClient.Builder builder, ToolCallbackProvider provider,
			Set<String> allowedTools) throws GraphStateException {
		var tools = Arrays.stream(provider.getToolCallbacks())
				.filter(tool -> allowedTools.contains(tool.getToolDefinition().name()))
				.collect(Collectors.toUnmodifiableSet());
		var selectedNames = tools.stream().map(tool -> tool.getToolDefinition().name()).collect(Collectors.toSet());
		if (!selectedNames.equals(allowedTools) || tools.isEmpty()) {
			throw new IllegalArgumentException("Check the allowed MCP tool names");
		}
		return new StateGraph()
				.addNode("mcp", node_async(new McpNode(builder.clone(), tools)))
				.addEdge(START, "mcp").addEdge("mcp", END).compile();
	}

	public static String runQuery(CompiledGraph graph, String query) {
		return graph.invoke(Map.of("query", query)).orElseThrow().value("mcpcontent", String.class).orElseThrow();
	}

	/**
	 * MCP 节点实现
	 */
	public static class McpNode implements NodeAction {

		private final ChatClient chatClient;

		public McpNode(ChatClient.Builder chatClientBuilder, Set<ToolCallback> toolCallbacks) {
			// 为节点配置 MCP 工具
			this.chatClient = chatClientBuilder
					.defaultTools(toolCallbacks)
					.build();
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String query = state.value("query", String.class).orElseThrow();
			String result = chatClient.prompt(query).call().content();
			return Map.of("mcpcontent", result);
		}
	}
}

