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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputException;
import com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputStrategy;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.GuardrailRefusal;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.function.FunctionToolCallback;
import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputFormat.RESULT_TOOL_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentStructuredOutputTest {

	private static final String ANSWER = "{\"score\":4}";

	@Test
	void negotiatesRealOpenAiRequestOptionsAgainstAnHttpFixture() throws Exception {
		var requests = new CopyOnWriteArrayList<String>();
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/chat/completions", exchange -> {
			try (exchange) {
				requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
				boolean first = requests.size() == 1;
				String body = first
						? """
							{"error":{"message":"response_format json_schema is not supported for this model",
							  "type":"invalid_request_error","param":"response_format","code":"unsupported_value"}}
							"""
						: """
							{"id":"fixture","object":"chat.completion","created":1,"model":"gpt-4o-mini","choices":[
							  {"index":0,"finish_reason":"tool_calls","message":{"role":"assistant","content":null,
							    "tool_calls":[{"id":"result","type":"function","function":{
							      "name":"__saa_structured_output","arguments":"{\\"score\\":4}"}}]}}]}
							""";
				byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(first ? 400 : 200, bytes.length);
				exchange.getResponseBody().write(bytes);
			}
		});
		server.start();
		try {
			var model = OpenAiChatModel.builder().options(OpenAiChatOptions.builder()
					.baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
					.apiKey("fixture").model("gpt-4o-mini").maxRetries(0).build()).build();
			var agent = ReactAgent.builder().name("native_http").model(model).outputType(Result.class).build();

			assertThat(agent.callStructured("Score this review", Result.class)).isEqualTo(new Result(4));
			assertThat(requests).hasSize(2);
			assertThat(requests.get(0)).contains("\"response_format\"", "\"type\":\"json_schema\"", "\"score\"")
					.doesNotContain(RESULT_TOOL_NAME);
			assertThat(requests.get(1)).contains("\"tools\"", RESULT_TOOL_NAME).doesNotContain("\"response_format\"");
		}
		finally {
			server.stop(0);
		}
	}

	@Test
	void usesNativeSchemaAndRetriesJavaMappingWithoutChangingStrategy() throws Exception {
		var requests = new ArrayList<Prompt>();
		var model = new StubModel(OpenAiChatOptions.builder().model("native").build(), prompt -> {
			requests.add(prompt);
			return textReply(requests.size() == 1 ? "{\"score\":8}" : ANSWER);
		});
		var agent = ReactAgent.builder().name("native_output").model(model).outputType(Result.class).build();

		assertThat(agent.callStructured("Score this review", Result.class)).isEqualTo(new Result(4));
		assertThat(requests).hasSize(2).allSatisfy(prompt -> {
			var options = (OpenAiChatOptions) prompt.getOptions();
			assertThat(options.getOutputSchema()).contains("\"score\"");
			assertThat(toolNames(prompt)).isEmpty();
			assertThat(texts(prompt)).noneMatch(text -> text.contains("\"properties\""));
		});
		assertThat(texts(requests.get(1))).anyMatch(text -> text.contains("could not be mapped to Result"));
	}

	@Test
	void fallsBackToToolsWhenTheProviderRejectsNativeSchema() throws Exception {
		var requests = new ArrayList<Prompt>();
		var model = new StubModel(OpenAiChatOptions.builder().model("rejects-native").build(), prompt -> {
			requests.add(prompt);
			if (requests.size() == 1) {
				throw new IllegalArgumentException("response_format json_schema is not supported for this model");
			}
			assertThat(((OpenAiChatOptions) prompt.getOptions()).getOutputSchema()).isNull();
			assertThat(toolNames(prompt)).containsExactly(RESULT_TOOL_NAME);
			return toolReply(call("result", RESULT_TOOL_NAME, ANSWER));
		});
		var agent = ReactAgent.builder().name("tool_fallback").model(model).outputType(Result.class)
				.structuredOutputMaxRetries(0).build();

		var reply = agent.call("Score this review");
		assertThat(reply.getText()).isEqualTo(ANSWER);
		assertThat(reply.getToolCalls()).isEmpty();
		assertThat(requests).hasSize(2);
	}

	@Test
	void fallsBackToPromptInStreamingAndPublishesOnlyTheValidatedAnswer() throws Exception {
		var requests = new ArrayList<Prompt>();
		var model = new StubModel(OpenAiChatOptions.builder().model("prompt-only").build(), prompt -> {
			requests.add(prompt);
			return switch (requests.size()) {
				case 1 -> throw new IllegalArgumentException("This model does not support JSON Schema");
				case 2 -> throw new IllegalArgumentException("This model does not support tool calling");
				case 3 -> textReply("invalid");
				default -> textReply(ANSWER);
			};
		});
		var agent = ReactAgent.builder().name("prompt_fallback").model(model).outputType(Result.class).build();
		var outputs = agent.stream("Score this review").collectList().block(Duration.ofSeconds(5));

		assertThat(agent.structuredOutput(outputs.get(outputs.size() - 1), Result.class)).contains(new Result(4));
		assertThat(requests).hasSize(4);
		assertThat(texts(requests.get(2))).anyMatch(text -> text.contains("\"score\""));
		assertThat(texts(requests.get(3))).anyMatch(text -> text.contains("single valid JSON value"));
		assertThat(outputs).allSatisfy(output -> {
			var messages = output.state().<List<Message>>value("messages").orElse(List.of());
			assertThat(messages).noneMatch(message -> "invalid".equals(message.getText()));
		});
	}

	@Test
	void validatesToolOutputAfterApprovalWithoutExecutingBusinessCallsDuringCorrection() throws Exception {
		var calls = new AtomicInteger();
		var saveReview = FunctionToolCallback.builder("save_review", (SaveReview input) -> {
			calls.incrementAndGet();
			return "Saved";
		}).description("Persist a review").inputType(SaveReview.class).build();
		var modelCalls = new AtomicInteger();
		var model = new StubModel(ToolCallingChatOptions.builder().build(), prompt -> {
			assertThat(toolNames(prompt)).contains(RESULT_TOOL_NAME);
			return switch (modelCalls.incrementAndGet()) {
				case 1 -> toolReply(call("save", "save_review", "{\"title\":\"Review\"}"),
						call("early", RESULT_TOOL_NAME, ANSWER));
				case 2 -> {
					assertThat(prompt.getInstructions()).anySatisfy(message -> assertThat(message)
							.isInstanceOfSatisfying(ToolResponseMessage.class, result -> assertThat(result.getResponses())
									.extracting(ToolResponseMessage.ToolResponse::id).containsExactly("save")));
					yield toolReply(call("invalid", RESULT_TOOL_NAME, "{}"));
				}
				case 3 -> {
					assertThat(prompt.getInstructions()).anySatisfy(message -> assertThat(message)
							.isInstanceOfSatisfying(ToolResponseMessage.class, feedback -> assertThat(feedback.getResponses())
									.anySatisfy(result -> {
										assertThat(result.id()).isEqualTo("invalid");
										assertThat(result.responseData()).contains("required");
									})));
					yield toolReply(call("save-again", "save_review", "{\"title\":\"Duplicate\"}"));
				}
				default -> toolReply(call("final", RESULT_TOOL_NAME, ANSWER));
			};
		});
		var agent = ReactAgent.builder().name("review_output").model(model).tools(saveReview).outputType(Result.class)
				.hooks(HumanInTheLoopHook.builder().approvalOn("save_review", "Review the save").build())
				.toolCallLimits(1, 1).saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("review-output").addMetadata("_stream_", false).build();

		var output = agent.invokeAndGetOutput("Save and score this review", config).orElseThrow();
		assertThat(output).isInstanceOf(InterruptionMetadata.class);
		var paused = (InterruptionMetadata) output;
		assertThat(paused.toolFeedbacks()).hasSize(1);
		assertThat(agent.structuredOutput(paused, Result.class)).isEmpty();
		assertThat(calls.get()).isZero();
		var decision = InterruptionMetadata.builder(paused).addToolFeedback(
				ToolFeedback.builder(paused.toolFeedbacks().get(0)).result(FeedbackResult.APPROVED).build()).build();

		var completed = agent.invokeAndGetOutput("", RunnableConfig.builder(config).addHumanFeedback(decision).build())
				.orElseThrow();
		assertThat(agent.structuredOutput(completed, Result.class)).contains(new Result(4));
		assertThat(calls.get()).isOne();
		assertThat(modelCalls.get()).isEqualTo(4);
		assertThat(completed.state().<List<Message>>value("messages").orElseThrow())
				.filteredOn(AssistantMessage.class::isInstance).map(AssistantMessage.class::cast)
				.flatExtracting(AssistantMessage::getToolCalls).extracting(AssistantMessage.ToolCall::id)
				.containsExactly("save");
	}

	@Test
	void rejectionOnANewRunDoesNotReturnThePreviousCheckpointedResult() throws Exception {
		var calls = new AtomicInteger();
		var model = new StubModel(ChatOptions.builder().build(), prompt -> calls.incrementAndGet() == 1
				? textReply(ANSWER) : new ChatResponse(List.of(GuardrailRefusal.generation("Policy refused"))));
		var agent = ReactAgent.builder().name("checkpoints").model(model).outputType(Result.class)
				.saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("same-thread").build();

		assertThat(agent.callStructured("Score this review", Result.class, config)).isEqualTo(new Result(4));
		var refused = agent.invokeAndGetOutput("Another question", config).orElseThrow();
		assertThat(agent.structuredOutput(refused, Result.class)).isEmpty();
		assertThat(calls.get()).isEqualTo(2);
	}

	@Test
	void exhaustedRetryBudgetReportsValidationFailure() {
		var calls = new AtomicInteger();
		var agent = ReactAgent.builder().name("exhausted").model(prompt -> {
			calls.incrementAndGet();
			return textReply(ANSWER + ANSWER);
		}).outputType(Result.class).structuredOutputMaxRetries(1).build();

		assertThatThrownBy(() -> agent.callStructured("Score this review", Result.class))
				.isInstanceOf(StructuredOutputException.class).hasMessageContaining("single valid JSON value");
		assertThat(calls.get()).isEqualTo(2);
	}

	@Test
	void forcedStrategiesDoNotDowngrade() {
		var model = new StubModel(OpenAiChatOptions.builder().model("rejects-native").build(), prompt -> {
			assertThat(((OpenAiChatOptions) prompt.getOptions()).getOutputSchema()).isNotBlank();
			throw new IllegalArgumentException("This model does not support JSON Schema");
		});
		var agent = ReactAgent.builder().name("forced_native").model(model).outputType(Result.class)
				.structuredOutputStrategy(StructuredOutputStrategy.NATIVE).build();

		assertThatThrownBy(() -> agent.callStructured("Score this review", Result.class))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not support JSON Schema");
	}

	@Test
	void choosesPromptForArraySchemasWhenToolsRequireObjectArguments() throws Exception {
		var model = new StubModel(ToolCallingChatOptions.builder().build(), prompt -> {
			assertThat(toolNames(prompt)).isEmpty();
			assertThat(texts(prompt)).anyMatch(text -> text.contains("\"array\""));
			return textReply("[1,2]");
		});
		var agent = ReactAgent.builder().name("array_output").model(model).outputType(int[].class).build();

		assertThat(agent.callStructured("Return two numbers", int[].class)).containsExactly(1, 2);
	}

	@Test
	void preservesLegacyFormatInstructionsWithAnExampleObject() throws Exception {
		String instructions = "Follow this example format: {\"score\":4}";
		var model = new StubModel(OpenAiChatOptions.builder().build(), prompt -> {
			assertThat(((OpenAiChatOptions) prompt.getOptions()).getOutputSchema()).isNull();
			assertThat(texts(prompt)).anyMatch(text -> text.contains(instructions));
			return textReply("legacy answer");
		});
		var agent = ReactAgent.builder().name("legacy_format").model(model).outputSchema(instructions).build();

		assertThat(agent.call("Score this review").getText()).isEqualTo("legacy answer");
	}

	private static List<String> texts(Prompt prompt) {
		return prompt.getInstructions().stream().map(Message::getText).toList();
	}

	private static List<String> toolNames(Prompt prompt) {
		return ((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks().stream()
				.map(tool -> tool.getToolDefinition().name()).toList();
	}

	private static ChatResponse textReply(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private static ChatResponse toolReply(AssistantMessage.ToolCall... calls) {
		return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(calls)).build())));
	}

	private static AssistantMessage.ToolCall call(String id, String name, String arguments) {
		return new AssistantMessage.ToolCall(id, "function", name, arguments);
	}

	record Result(int score) {

		Result {
			if (score < 1 || score > 5) {
				throw new IllegalArgumentException("Score must be between 1 and 5");
			}
		}

	}

	record SaveReview(String title) {
	}

	private record StubModel(ChatOptions options, Function<Prompt, ChatResponse> responder) implements ChatModel {

		@Override
		public ChatOptions getOptions() {
			return options;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			return responder.apply(prompt);
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.defer(() -> Flux.just(call(prompt)));
		}

	}

}
