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
package com.alibaba.cloud.ai.examples.documentation.graph.core;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import reactor.core.publisher.Flux;

/**
 * MCP 节点示例
 * 演示如何为指定节点分配 MCP 工具
 */
public class McpNodeExample {

	/**
	 * 配置 MCP 节点
	 */
	public static void configureMcpNode(ChatClient.Builder chatClientBuilder, Set<ToolCallback> toolCallbacks) {
		McpNode mcpNode = new McpNode(chatClientBuilder, toolCallbacks);
		System.out.println("MCP node configured successfully");
	}

	public static void main(String[] args) {
		System.out.println("=== MCP 节点示例 ===\n");

		try {
			// 示例: 配置 MCP 节点（需要 ChatClient 和 ToolCallbacks）
			System.out.println("示例: 配置 MCP 节点");
			System.out.println("注意: 此示例需要 ChatClient 和 ToolCallbacks，跳过执行");
			// configureMcpNode(ChatClient.builder(...), toolCallbacks);
			System.out.println();

			System.out.println("所有示例执行完成");
			System.out.println("提示: 请配置 ChatClient 和 ToolCallbacks 后运行完整示例");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * MCP 节点实现
	 */
	public static class McpNode implements NodeAction {

		private static final String NODENAME = "mcp-node";

		private final ChatClient chatClient;

		public McpNode(ChatClient.Builder chatClientBuilder, Set<ToolCallback> toolCallbacks) {
			// 为节点配置 MCP 工具
			this.chatClient = chatClientBuilder
					.defaultToolCallbacks(toolCallbacks.toArray(ToolCallback[]::new))
					.build();
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String query = state.value("query", "");
			Flux<String> streamResult = chatClient.prompt(query).stream().content();
			String result = streamResult.reduce("", (acc, item) -> acc + item).block();

			HashMap<String, Object> resultMap = new HashMap<>();
			resultMap.put("mcpcontent", result);

			return resultMap;
		}
	}
}

