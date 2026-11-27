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

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.context.ContextWindowPolicy;
import com.alibaba.cloud.ai.graph.agent.context.ModelCapacity;
import com.alibaba.cloud.ai.graph.agent.interceptor.modelfallback.ModelFallbackInterceptor;
import com.alibaba.cloud.ai.graph.agent.model.ModelRoutingPolicy;
import com.alibaba.cloud.ai.graph.agent.model.ModelTarget;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentModelRoutingTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void fallbackUsesItsOwnCapacityAndNativeOptionsWithTheSameRequestAssembly(boolean stream) throws Exception {
		var primary = new StubModel(ToolCallingChatOptions.builder().model("primary").maxTokens(80).build(),
				prompt -> { throw new AssertionError("The primary request exceeds its input budget"); });
		var smaller = new StubModel(ToolCallingChatOptions.builder().model("too-small").build(),
				prompt -> { throw new AssertionError("The fallback request exceeds its distinct input limit"); });
		var requests = new AtomicInteger();
		var calls = new AtomicInteger();
		var large = new StubModel(OpenAiChatOptions.builder().model("fallback-native").maxTokens(40).build(), prompt -> {
			calls.incrementAndGet();
			assertThat(prompt.getOptions()).isInstanceOf(OpenAiChatOptions.class);
			assertThat(prompt.getOptions().getModel()).isEqualTo("fallback-native");
			assertThat(prompt.getInstructions()).extracting(Message::getText)
				.contains("client instructions", "agent instructions", "advisor instructions", "question ".repeat(35));
			ToolCallingChatOptions options = (ToolCallingChatOptions) prompt.getOptions();
			assertThat(options.getToolCallbacks()).extracting(tool -> tool.getToolDefinition().name()).contains("lookup");
			return answer("fallback");
		});
		var lookup = FunctionToolCallback.builder("lookup", (Map<String, Object> input) -> "result")
			.description("Look up an item").inputType(Map.class).build();
		var baseClient = ChatClient.builder(primary).defaultSystem("client instructions").defaultAdvisors(
			new BaseAdvisor() {
				@Override
				public String getName() {
					return "test-instructions";
				}

				@Override
				public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
					requests.incrementAndGet();
					var messages = new ArrayList<>(request.prompt().getInstructions());
					messages.add(0, new SystemMessage("advisor instructions"));
					return new ChatClientRequest(new Prompt(messages, request.prompt().getOptions()), request.context());
				}

				@Override
				public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
					return response;
				}

				@Override
				public int getOrder() {
					return 0;
				}
			}).build();
		var fallback = ModelFallbackInterceptor.builder()
			.addFallbackModel(smaller, capacity(1_500, 50, 100))
			.addFallbackModel(large, capacity(3_000, 0, 100)).build();
		var agent = ReactAgent.builder().name("routing").chatClient(baseClient).systemPrompt("agent instructions")
			.tools(lookup).contextWindowPolicy(capacity(150, 0, 40)).interceptors(fallback).build();

		if (stream) {
			assertThat(agent.stream("question ".repeat(35)).blockLast(TIMEOUT)).isNotNull();
		}
		else {
			assertThat(agent.call("question ".repeat(35)).getText()).isEqualTo("fallback");
		}
		assertThat(calls).hasValue(1);
		assertThat(requests).hasValue(3);
	}

	@Test
	void reroutesAnOrdinaryStreamingFailureBeforeOutput() throws Exception {
		var primary = new StubModel(ChatOptions.builder().build(), prompt -> {
			throw new IllegalStateException("provider failed");
		});
		var calls = new AtomicInteger();
		var fallback = new StubModel(ChatOptions.builder().build(), prompt -> {
			calls.incrementAndGet();
			return answer("fallback");
		});
		var agent = ReactAgent.builder().name("routing").model(primary)
			.interceptors(ModelFallbackInterceptor.builder().addFallbackModel(fallback).build()).build();

		assertThat(agent.stream("question").blockLast(TIMEOUT)).isNotNull();
		assertThat(calls).hasValue(1);
	}

	@Test
	void propagationAfterStreamOutputDoesNotStartAFallback() {
		var calls = new AtomicInteger();
		var primary = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				throw new AssertionError("Expected streaming");
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.concat(Flux.just(answer("partial")), Flux.error(new IllegalStateException("provider failed")));
			}
		};
		var agent = ReactAgent.builder().name("routing").model(primary)
			.interceptors(ModelFallbackInterceptor.builder().addFallbackModel(prompt -> {
				calls.incrementAndGet();
				return answer("fallback");
			}).build()).build();

		assertThatThrownBy(() -> agent.stream("question").blockLast(TIMEOUT)).hasMessageContaining("provider failed");
		assertThat(calls).hasValue(0);
	}

	@Test
	void persistsTheSelectedRouteAcrossToolApprovalAndSelectsAgainOnTheNextRun(@TempDir Path directory) throws Exception {
		var selections = new AtomicInteger();
		var toolCalls = new AtomicInteger();
		var tool = FunctionToolCallback.builder("approve", (Map<String, Object> input) -> {
			toolCalls.incrementAndGet();
			return "approved";
		}).description("Confirm a tool").inputType(Map.class)
			.toolMetadata(ToolMetadata.builder().requiresConfirmation(true).build()).build();
		var routed = new StubModel(ToolCallingChatOptions.builder().model("routed").build(), prompt -> {
			if (prompt.getInstructions().stream().anyMatch(ToolResponseMessage.class::isInstance)) {
				return answer("complete");
			}
			var call = new AssistantMessage.ToolCall("approval", "function", "approve", "{}");
			return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(call)).build())));
		});
		var defaultModel = new StubModel(ChatOptions.builder().build(), prompt -> answer("default response"));
		var policy = ModelRoutingPolicy.builder("default").route("default", new ModelTarget(defaultModel))
			.route("routed", new ModelTarget(routed)).selector(request -> {
				selections.incrementAndGet();
				return request.getMessages().stream().filter(UserMessage.class::isInstance).reduce((a, b) -> b)
					.map(Message::getText).filter("run approved tool"::equals).map(text -> "routed").orElse("default");
			}).build();
		var config = RunnableConfig.builder().threadId("thread").build();
		var first = ReactAgent.builder().name("routing").modelRouting(policy).tools(tool)
			.saver(FileSystemSaver.builder().targetFolder(directory).build()).build();
		var pause = (InterruptionMetadata) first.stream("run approved tool", config).blockLast(TIMEOUT);
		assertThat(toolCalls).hasValue(0);
		var feedback = InterruptionMetadata.builder(pause).addToolFeedback(ToolFeedback.builder(pause.toolFeedbacks().get(0))
			.result(FeedbackResult.APPROVED).build()).build();
		var resumed = RunnableConfig.builder(config).addHumanFeedback(feedback).build();
		var restarted = ReactAgent.builder().name("routing").modelRouting(policy).tools(tool)
			.saver(FileSystemSaver.builder().targetFolder(directory).build()).build();

		assertThat(restarted.stream(Map.of(), resumed).blockLast(TIMEOUT).state().<List<Message>>value("messages").orElseThrow())
			.extracting(Message::getText).contains("complete");
		assertThat(selections).hasValue(1);
		assertThat(toolCalls).hasValue(1);
		assertThat(restarted.call("use default route", config).getText()).isEqualTo("default response");
		assertThat(selections).hasValue(2);
	}

	private static ContextWindowPolicy capacity(int window, int input, int reserve) {
		return ContextWindowPolicy.builder(new ModelCapacity(window, input, reserve)).build();
	}

	private static ChatResponse answer(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private record StubModel(ChatOptions options, Function<Prompt, ChatResponse> fn) implements ChatModel {

		@Override
		public ChatOptions getOptions() {
			return this.options;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			return this.fn.apply(prompt);
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.defer(() -> Flux.just(call(prompt)));
		}

	}

}
