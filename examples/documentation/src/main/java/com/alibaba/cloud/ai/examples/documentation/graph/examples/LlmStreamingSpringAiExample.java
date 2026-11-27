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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.examples.documentation.graph.core.StreamingExample;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Spring AI Alibaba LLM 流式集成示例
 * 演示如何在 Spring AI Alibaba Graph 中使用 LLM 流式输出功能
 */
public class LlmStreamingSpringAiExample {

	/**
	 * 使用流式 ChatClient
	 */
	public static void useStreamingChatClient(ChatClient chatClient) {
		// 使用流式输出
		Flux<ChatResponse> flux = chatClient.prompt()
				.user("tell me a joke")
				.stream()
				.chatResponse();

		// 订阅流式响应
		flux.subscribe(
				response -> {
					String content = response.getResult().getOutput().getText();
					System.out.print(content);
				},
				error -> System.err.println("Error: " + error.getMessage()),
				() -> System.out.println("\nStream completed")
		);
	}

	/**
	 * 使用 Reactor 的阻塞式处理
	 */
	public static void useBlockingStreaming(ChatClient chatClient) {
		Flux<ChatResponse> flux = chatClient.prompt()
				.user("tell me a joke")
				.stream()
				.chatResponse();

		// 使用 Reactor 的阻塞式处理
		flux.collectList().block().forEach(response -> {
			System.out.println("Received: " + response.getResult().getOutput().getText());
		});
	}

	public static void streamGraph(ChatClient.Builder builder, String query) throws GraphStateException {
		var graph = new StateGraph(() -> Map.of("messages", new AppendStrategy()))
				.addNode("answer", node_async(new StreamingAgentNode(builder)))
				.addEdge(START, "answer").addEdge("answer", END).compile();

		graph.stream(Map.of("query", query))
				.doOnNext(output -> {
					if (output instanceof StreamingOutput<?> chunk && chunk.chunk() != null) {
						System.out.print(chunk.chunk());
					}
				})
				.blockLast(Duration.ofSeconds(60));
		System.out.println();
	}

	public static void main(String[] args) throws GraphStateException {
		if (!Arrays.asList(args).contains("--live")) {
			StreamingExample.main(args);
			return;
		}
		var apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("Set AI_DASHSCOPE_API_KEY before using --live");
		}
		var api = DashScopeApi.builder().apiKey(apiKey).build();
		var model = DashScopeChatModel.builder().dashScopeApi(api).build();
		streamGraph(ChatClient.builder(model), "用一句话介绍 Spring AI");
	}

	/**
	 * 在 Graph 节点中使用流式输出
	 */
	public static class StreamingAgentNode implements NodeAction {

		private final ChatClient chatClient;

		public StreamingAgentNode(ChatClient.Builder builder) {
			this.chatClient = builder.build();
		}

		@Override
		public Map<String, Object> apply(OverAllState state) {
			String userMessage = (String) state.value("query").orElse("Hello");

			// 使用流式输出
			var responses = chatClient.prompt()
					.user(userMessage)
					.stream()
					.chatResponse();

			return Map.of("messages", responses);
		}
	}
}

