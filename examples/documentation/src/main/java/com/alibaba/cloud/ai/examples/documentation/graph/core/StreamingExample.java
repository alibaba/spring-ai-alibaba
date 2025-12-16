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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * 流式输出示例
 * 演示如何在 Spring AI Alibaba Graph 中实现流式输出
 */
public class StreamingExample {

	/**
	 * 使用 StateGraph 实现流式输出的完整示例
	 *
	 * @param chatClientBuilder ChatClient 构建器
	 * @throws GraphStateException 图执行异常
	 */
	public static void streamLLMTokens(ChatClient.Builder chatClientBuilder) throws GraphStateException {
		// 定义状态策略
		KeyStrategyFactory keyStrategyFactory = () -> {
			Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
			keyStrategyMap.put("query", new AppendStrategy());
			keyStrategyMap.put("messages", new AppendStrategy());
			keyStrategyMap.put("result", new AppendStrategy());
			return keyStrategyMap;
		};

		// 创建流式节点
		StreamingNode streamingNode = new StreamingNode(chatClientBuilder, "streaming_node");

		// 创建处理节点
		ProcessStreamingNode processNode = new ProcessStreamingNode();

		// 构建图
		StateGraph stateGraph = new StateGraph(keyStrategyFactory)
				.addNode("streaming_node", AsyncNodeAction.node_async(streamingNode))
				.addNode("process_node", AsyncNodeAction.node_async(processNode))
				.addEdge(START, "streaming_node")
				.addEdge("streaming_node", "process_node")
				.addEdge("process_node", END);

		// 编译图
		CompiledGraph graph = stateGraph.compile(
				CompileConfig.builder()
						.build()
		);

		// 创建配置
		RunnableConfig config = RunnableConfig.builder()
				.threadId("streaming_thread")
				.build();

		// 使用流式方式执行图
		System.out.println("开始流式输出...\n");

		graph.stream(Map.of("query", "请用一句话介绍 Spring AI"), config)
				.doOnNext(output -> {
					// 处理流式输出
					if (output instanceof StreamingOutput<?> streamingOutput) {
						// 流式输出块
						String chunk = streamingOutput.chunk();
						if (chunk != null && !chunk.isEmpty()) {
							System.out.print(chunk); // 实时打印流式内容
						}
					}
					else {
						// 普通节点输出
						String nodeId = output.node();
						Map<String, Object> state = output.state().data();
						System.out.println("\n节点 '" + nodeId + "' 执行完成");
						if (state.containsKey("result")) {
							System.out.println("最终结果: " + state.get("result"));
						}
					}
				})
				.doOnComplete(() -> {
					System.out.println("\n\n流式输出完成");
				})
				.doOnError(error -> {
					System.err.println("流式输出错误: " + error.getMessage());
				})
				.blockLast(); // 阻塞等待流完成
	}

	public static void main(String[] args) {
		System.out.println("=== 流式输出示例 ===\n");

		try {
			// 示例 1: 使用 Spring AI 的流式 LLM tokens（需要 ChatClient）
			System.out.println("示例 1: 使用 Spring AI 的流式 LLM tokens");
			System.out.println("注意: 此示例需要 ChatClient，跳过执行");
			System.out.println("使用方法: streamLLMTokens(ChatClient.builder()...)");
			// streamLLMTokens(ChatClient.builder()...);
			System.out.println();

			System.out.println("所有示例执行完成");
			System.out.println("提示: 请配置 ChatClient 后运行完整示例");
		}
		catch (Exception e) {
			System.err.println("执行示例时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static class StreamingNode implements NodeAction {

		private final ChatClient chatClient;
		private final String nodeId;

		public StreamingNode(ChatClient.Builder chatClientBuilder, String nodeId) {
			this.chatClient = chatClientBuilder.build();
			this.nodeId = nodeId;
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String query = (String) state.value("query").orElse("");

			// 获取流式响应
			Flux<ChatResponse> chatResponseFlux = chatClient.prompt()
					.user(query)
					.stream()
					.chatResponse();

			return Map.of("messages", chatResponseFlux);
		}
	}

	/**
	 * 处理流式输出的节点 - 接收并处理流式响应
	 */
	public static class ProcessStreamingNode implements NodeAction {

		@Override
		public Map<String, Object> apply(OverAllState state) {
			// 从状态中获取流式响应结果
			Object messages = state.value("messages").orElse("");
			String result = "流式响应已处理完成: " + messages;
			return Map.of("result", result);
		}
	}
}

