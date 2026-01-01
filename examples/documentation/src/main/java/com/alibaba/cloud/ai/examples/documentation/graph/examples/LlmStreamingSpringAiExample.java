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
import com.alibaba.cloud.ai.graph.action.NodeAction;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.Map;

import reactor.core.publisher.Flux;

/**
 * Spring AI Alibaba LLM 流式集成示例
 * 演示如何�?Spring AI Alibaba Graph 中使�?LLM 流式输出功能
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

	public static void main(String[] args) {
		System.out.println("=== Spring AI Alibaba LLM 流式集成示例 ===\n");

		try {
			// 示例 1: 使用流式 ChatClient（需�?ChatClient�?
			System.out.println("示例 1: 使用流式 ChatClient");
			System.out.println("注意: 此示例需�?ChatClient，跳过执�?);
			// useStreamingChatClient(chatClient);
			System.out.println();

			// 示例 2: 使用 Reactor 的阻塞式处理（需�?ChatClient�?
			System.out.println("示例 2: 使用 Reactor 的阻塞式处理");
			System.out.println("注意: 此示例需�?ChatClient，跳过执�?);
			// useBlockingStreaming(chatClient);
			System.out.println();

			System.out.println("所有示例执行完�?);
			System.out.println("提示: 请配�?ChatClient 后运行完整示�?);
		}
		catch (Exception e) {
			System.err.println("执行示例时出�? " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * �?Graph 节点中使用流式输�?
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
			Flux<String> contentFlux = chatClient.prompt()
					.user(userMessage)
					.stream()
					.content();

			return Map.of("answer", contentFlux);
		}
	}
}

