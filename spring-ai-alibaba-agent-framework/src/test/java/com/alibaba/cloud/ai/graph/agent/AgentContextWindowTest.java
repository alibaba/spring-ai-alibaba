/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.cloud.ai.graph.agent.context.ContextWindowExceededException;
import com.alibaba.cloud.ai.graph.agent.context.ContextWindowPolicy;
import com.alibaba.cloud.ai.graph.agent.interceptor.modelfallback.ModelFallbackInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.modelretry.ModelRetryInterceptor;
import com.openai.errors.OpenAIServiceException;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentContextWindowTest {

	private static final String RAW_RESULT = "Archived result text. ".repeat(40);

	private static final Duration STREAM_TIMEOUT = Duration.ofSeconds(10);

	private static final String OVERFLOW = """
			{"error":{"message":"Context too long","type":"invalid_request_error","param":"messages",\
			"code":"context_length_exceeded"}}
			""";

	private static final String COMPLETION = """
			{"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"gpt-4o-mini",\
			"choices":[{"index":0,"message":{"role":"assistant","content":"done"},"finish_reason":"stop"}]}
			""";

	private static final String STREAM_COMPLETION = """
			data: {"id":"chatcmpl-test","object":"chat.completion.chunk","created":1,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{"role":"assistant","content":"done"},"finish_reason":null}]}

			data: {"id":"chatcmpl-test","object":"chat.completion.chunk","created":1,"model":"gpt-4o-mini","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

			data: [DONE]

			""";

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shrinksRejectedNativeModelRequestsOnce(boolean stream) throws Exception {
		List<String> requests = new CopyOnWriteArrayList<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/chat/completions", exchange -> {
			try (exchange) {
				requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
				boolean first = requests.size() == 1;
				String payload = first ? OVERFLOW : (stream ? STREAM_COMPLETION : COMPLETION);
				exchange.getResponseHeaders()
					.set("Content-Type", !first && stream ? "text/event-stream" : "application/json");
				byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(first ? 400 : 200, bytes.length);
				exchange.getResponseBody().write(bytes);
			}
		});
		server.start();
		try {
			ChatModel model = OpenAiChatModel.builder()
				.options(OpenAiChatOptions.builder()
					.baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
					.apiKey("fixture")
					.model("gpt-4o-mini")
					.maxRetries(0)
					.build())
				.build();
			var policy = ContextWindowPolicy.builder(10_000)
				.inputOverflowDetector(error -> error instanceof OpenAIServiceException service
						&& service.code().filter("context_length_exceeded"::equals).isPresent())
				.build();
			var fallbackCalls = new AtomicInteger();
			var fallback = ModelFallbackInterceptor.builder().addFallbackModel(prompt -> {
				fallbackCalls.incrementAndGet();
				return response("fallback");
			}).build();
			var agent = ReactAgent.builder().name("context").model(model).systemPrompt("system instruction")
				.contextWindowPolicy(policy).interceptors(fallback).build();
			if (stream) {
				assertThat(agent.stream(history()).blockLast(STREAM_TIMEOUT)).isNotNull();
			}
			else {
				assertThat(agent.call(history()).getText()).isEqualTo("done");
			}

			assertThat(requests).hasSize(2);
			assertThat(requests.get(0)).contains(RAW_RESULT);
			assertThat(requests.get(1)).doesNotContain(RAW_RESULT)
				.contains("[Earlier tool result omitted]", "system instruction", "new question", "\"tool_call_id\":\"old\"");
			assertThat(requests.get(1).length()).isLessThan(requests.get(0).length());
			assertThat(fallbackCalls).hasValue(0);
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void blocksAnOversizedToolSchemaBeforeCallingTheModel() {
		var calls = new AtomicInteger();
		var model = new ChatModel() {
			@Override
			public ToolCallingChatOptions getOptions() {
				return ToolCallingChatOptions.builder().build();
			}

			@Override
			public ChatResponse call(Prompt prompt) {
				calls.incrementAndGet();
				return response("unexpected");
			}
		};
		var tool = new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name("large_tool").description("Tool")
					.inputSchema("{\"type\":\"object\",\"description\":\"" + "detail ".repeat(120) + "\"}").build();
			}

			@Override
			public String call(String input) {
				return "unused";
			}
		};
		var policy = ContextWindowPolicy.builder(60).build();
		var agent = ReactAgent.builder().name("context").model(model).tools(tool).contextWindowPolicy(policy).build();

		assertThatThrownBy(() -> agent.call("question")).isInstanceOf(ContextWindowExceededException.class);
		assertThat(calls).hasValue(0);
	}

	@Test
	void anUnshrinkableRequestBypassesOrdinaryModelRetry() {
		var calls = new AtomicInteger();
		ChatModel model = prompt -> {
			calls.incrementAndGet();
			throw new IllegalStateException("input limit");
		};
		var retry = ModelRetryInterceptor.builder().maxAttempts(3).initialDelay(0)
			.retryableExceptionPredicate(error -> true).build();
		var agent = ReactAgent.builder().name("context").model(model).contextWindowPolicy(overflowPolicy())
			.interceptors(retry).build();

		assertThatThrownBy(() -> agent.call("current question")).isInstanceOf(ContextWindowExceededException.class);
		assertThat(calls).hasValue(1);
	}

	@Test
	void aSecondInputRejectionRetainsTheCauseAndEndsRecovery() {
		var calls = new AtomicInteger();
		ChatModel model = prompt -> {
			calls.incrementAndGet();
			throw new IllegalStateException("input limit");
		};
		var agent = ReactAgent.builder().name("context").model(model).contextWindowPolicy(overflowPolicy()).build();

		assertThatThrownBy(() -> agent.call(history())).isInstanceOf(ContextWindowExceededException.class)
			.hasRootCauseMessage("input limit");
		assertThat(calls).hasValue(2);
	}

	@Test
	void editsOlderToolResultsBeforeDispatchAndRetainsTheCurrentTurn() throws Exception {
		var currentResult = "current tool output";
		var messages = new ArrayList<>(history());
		var call = new AssistantMessage.ToolCall("current", "function", "lookup", "{}");
		messages.add(AssistantMessage.builder().toolCalls(List.of(call)).build());
		messages.add(ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse("current", "lookup", currentResult))).build());
		ChatModel model = prompt -> {
			var results = prompt.getInstructions().stream()
				.filter(ToolResponseMessage.class::isInstance)
				.map(ToolResponseMessage.class::cast)
				.flatMap(message -> message.getResponses().stream())
				.toList();
			assertThat(results).extracting(ToolResponseMessage.ToolResponse::id).containsExactly("old", "current");
			assertThat(results).extracting(ToolResponseMessage.ToolResponse::responseData)
				.containsExactly("[Earlier tool result omitted]", currentResult);
			return response("done");
		};
		var policy = ContextWindowPolicy.builder(1_000).targetInputTokens(70).build();
		var agent = ReactAgent.builder().name("context").model(model).contextWindowPolicy(policy).build();

		assertThat(agent.call(messages).getText()).isEqualTo("done");
	}

	@Test
	void neverReplaysAStreamAfterOutput() {
		var calls = new AtomicInteger();
		var model = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				throw new AssertionError("Expected a streaming call");
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				calls.incrementAndGet();
				return Flux.concat(Flux.just(response("partial")), Flux.error(new IllegalStateException("input limit")));
			}
		};
		var agent = ReactAgent.builder().name("context").model(model).contextWindowPolicy(overflowPolicy()).build();

		assertThatThrownBy(() -> agent.stream(history()).blockLast(STREAM_TIMEOUT))
			.hasRootCauseMessage("input limit");
		assertThat(calls).hasValue(1);
	}

	private static ContextWindowPolicy overflowPolicy() {
		return ContextWindowPolicy.builder(10_000)
			.inputOverflowDetector(error -> "input limit".equals(error.getMessage()))
			.build();
	}

	private static List<Message> history() {
		var call = new AssistantMessage.ToolCall("old", "function", "lookup", "{}");
		return List.of(new UserMessage("old question"), AssistantMessage.builder().toolCalls(List.of(call)).build(),
				ToolResponseMessage.builder().responses(List.of(
						new ToolResponseMessage.ToolResponse("old", "lookup", RAW_RESULT))).build(),
				new AssistantMessage("old answer"), new UserMessage("new question"));
	}

	private static ChatResponse response(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

}
