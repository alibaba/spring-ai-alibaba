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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ChatClientToolCallTest {

	private DashScopeChatModel chatModel;

	public static class CalculatorTool implements BiFunction<String, ToolContext, String> {

		private int callCount = 0;

		@Override
		public String apply(String expression, ToolContext toolContext) {
			callCount++;
			try {
				String[] parts = expression.split("\\+");
				if (parts.length == 2) {
					int a = Integer.parseInt(parts[0].trim());
					int b = Integer.parseInt(parts[1].trim());
					int result = a + b;
					return String.valueOf(result);
				}
			}
			catch (Exception e) {
				// Calculation failed
			}

			return "Unable to calculate expression";
		}

		public int getCallCount() {
			return callCount;
		}
	}

	@BeforeEach
	void setUp() {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();

		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	void testCallWithToolCall() {
		CalculatorTool tool = new CalculatorTool();
		ToolCallback toolCallback = FunctionToolCallback.builder("calculator", tool)
			.description("Execute simple addition calculation, format: 'a + b'")
			.inputType(String.class)
			.build();

		ChatClient chatClient = ChatClient.builder(chatModel).defaultToolCallbacks(toolCallback).build();
		String result = chatClient.prompt().user("帮我计算 123 加 456 等于多少？").call().content();

		assertNotNull(result, "Result should not be null");
		assertTrue(tool.getCallCount() > 0, "Tool should be called at least once");
		System.out.println(result);
	}

	@Test
	void testStreamWithToolCall() {
		CalculatorTool tool = new CalculatorTool();
		ToolCallback toolCallback = FunctionToolCallback.builder("calculator", tool)
			.description("Execute simple addition calculation, format: 'a + b'")
			.inputType(String.class)
			.build();

		ChatClient chatClient = ChatClient.builder(chatModel).defaultToolCallbacks(toolCallback).build();

		AtomicInteger chunkCount = new AtomicInteger(0);
		AtomicInteger toolCallChunkCount = new AtomicInteger(0);
		StringBuilder fullContent = new StringBuilder();

		Flux<ChatResponse> responseFlux = chatClient.prompt()
			.user("帮我计算 123 加 456 等于多少？")
			.stream()
			.chatResponse();

		responseFlux.doOnNext(response -> {
			chunkCount.incrementAndGet();
			if (response.getResult() != null && response.getResult().getOutput() != null) {
				AssistantMessage message = response.getResult().getOutput();

				if (message.getText() != null && !message.getText().isEmpty()) {
					fullContent.append(message.getText());
				}

				if (message.hasToolCalls()) {
					toolCallChunkCount.incrementAndGet();
				}
			}
		})
		.doOnComplete(() -> {
			System.out.println(fullContent.toString());
		})
		.doOnError(error -> {
			error.printStackTrace();
		})
		.blockLast();

		assertTrue(chunkCount.get() > 0, "Should receive at least one chunk");
	}

	@Test
	void testCompareCallAndStream() {
		String query = "请计算 100 加 200 等于多少？";

		CalculatorTool callTool = new CalculatorTool();
		ToolCallback callToolCallback = FunctionToolCallback.builder("calculator", callTool)
			.description("Execute simple addition calculation, format: 'a + b'")
			.inputType(String.class)
			.build();

		ChatClient callClient = ChatClient.builder(chatModel).defaultToolCallbacks(callToolCallback).build();
		String callResult = callClient.prompt().user(query).call().content();

		CalculatorTool streamTool = new CalculatorTool();
		ToolCallback streamToolCallback = FunctionToolCallback.builder("calculator", streamTool)
			.description("Execute simple addition calculation, format: 'a + b'")
			.inputType(String.class)
			.build();

		ChatClient streamClient = ChatClient.builder(chatModel).defaultToolCallbacks(streamToolCallback).build();

		StringBuilder streamResult = new StringBuilder();
		AtomicInteger streamChunks = new AtomicInteger(0);

		streamClient.prompt().user(query).stream().chatResponse().doOnNext(response -> {
			streamChunks.incrementAndGet();
			if (response.getResult() != null && response.getResult().getOutput() != null) {
				AssistantMessage message = response.getResult().getOutput();
				if (message.getText() != null) {
					streamResult.append(message.getText());
				}
			}
		}).blockLast();

		System.out.println("call() result: " + callResult);
		System.out.println("stream() result: " + streamResult.toString());

		assertNotNull(callResult, "Call result should not be null");
		assertNotNull(streamResult.toString(), "Stream result should not be null");
		assertTrue(callTool.getCallCount() > 0 || streamTool.getCallCount() > 0,
			"At least one tool should be invoked");
	}

}
