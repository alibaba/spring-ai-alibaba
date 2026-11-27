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
package com.alibaba.cloud.ai.graph.agent.node;

import com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputException;
import com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputFormat;
import com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputStrategy;
import com.alibaba.cloud.ai.graph.utils.ChatResponseStreamAggregator;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.UsageAccumulator;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.chat.metadata.FinishReasonCategory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.GuardrailRefusal;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputFormat.RESULT_METADATA;
import static com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputFormat.RESULT_TOOL_NAME;
import static com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputStrategy.AUTO;
import static com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputStrategy.NATIVE;
import static com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputStrategy.PROMPT;
import static com.alibaba.cloud.ai.graph.agent.structured.StructuredOutputStrategy.TOOL;

/** Negotiates and validates structured output inside a model turn, before graph dispatch. */
final class StructuredOutputAdvisor implements CallAdvisor, StreamAdvisor {

	private final StructuredOutputFormat format;

	private final String agentName;

	private final String executionId;

	private final BiConsumer<List<Message>, String> appendInstructions;

	private final boolean mergeReasoning;

	private final ToolCallback resultTool;

	StructuredOutputAdvisor(StructuredOutputFormat format, String agentName, String executionId,
			BiConsumer<List<Message>, String> appendInstructions, boolean mergeReasoning) {
		this.format = format;
		this.agentName = agentName;
		this.executionId = executionId;
		this.appendInstructions = appendInstructions;
		this.mergeReasoning = mergeReasoning;
		ToolDefinition definition = ToolDefinition.builder()
				.name(RESULT_TOOL_NAME)
				.description("Submit the final structured answer once all business tool calls have been completed.")
				.inputSchema(format.jsonSchema()).build();
		this.resultTool = new ToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return definition;
			}

			@Override
			public String call(String input) {
				throw new IllegalStateException("The structured output tool is handled by the model node");
			}
		};
	}

	ToolCallback resultTool() {
		return resultTool;
	}

	@Override
	public String getName() {
		return "ReactAgentStructuredOutput";
	}

	@Override
	public int getOrder() {
		// Run after request tool selection and before tool capture and the context budget.
		return Ordered.LOWEST_PRECEDENCE - 2;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		var usage = new UsageAccumulator();
		var attempt = initialAttempt(request);
		while (true) {
			ChatClientResponse response;
			try {
				response = chain.copy(this).nextCall(prepare(request, attempt));
			}
			catch (RuntimeException ex) {
				attempt = fallback(request, attempt, ex);
				continue;
			}
			usage.addRoundResponse(response.chatResponse());
			try {
				return usage.applyAccumulatedUsage(accept(response, attempt));
			}
			catch (StructuredOutputException ex) {
				attempt = retry(attempt, response, ex);
			}
		}
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
		return Flux.defer(() -> streamAttempt(request, chain, initialAttempt(request), new UsageAccumulator()).flux());
	}

	private Mono<ChatClientResponse> streamAttempt(ChatClientRequest request, StreamAdvisorChain chain,
			Attempt attempt, UsageAccumulator usage) {
		return Mono.defer(() -> {
			var chunks = chain.copy(this).nextStream(prepare(request, attempt));
			return ChatResponseStreamAggregator.aggregate(chunks.handle((chunk, sink) -> {
				if (chunk.chatResponse() != null) {
					sink.next(chunk.chatResponse());
				}
			}), mergeReasoning);
		}).map(response -> new ChatClientResponse(response, request.context()))
				.defaultIfEmpty(new ChatClientResponse(null, request.context()))
				.materialize()
				.flatMap(signal -> {
					if (signal.isOnError()) {
						var error = signal.getThrowable();
						return error instanceof RuntimeException failure
								? streamAttempt(request, chain, fallback(request, attempt, failure), usage)
								: Mono.error(error);
					}
					var response = signal.get();
					usage.addRoundResponse(response.chatResponse());
					try {
						return Mono.just(usage.applyAccumulatedUsage(accept(response, attempt)));
					}
					catch (StructuredOutputException ex) {
						return streamAttempt(request, chain, retry(attempt, response, ex), usage);
					}
				});
	}

	private Attempt initialAttempt(ChatClientRequest request) {
		var options = request.prompt().getOptions();
		var strategy = format.strategy();
		if (strategy == AUTO) {
			strategy = options instanceof StructuredOutputChatOptions ? NATIVE
					: supportsTools(options) ? TOOL : PROMPT;
		}
		else if ((strategy == NATIVE && !(options instanceof StructuredOutputChatOptions))
				|| (strategy == TOOL && !supportsTools(options))) {
			throw new StructuredOutputException("The model options do not support the " + strategy + " strategy");
		}
		return new Attempt(strategy, 0, List.of());
	}

	private boolean supportsTools(ChatOptions options) {
		return options instanceof ToolCallingChatOptions && format.supportsToolSchema();
	}

	private ChatClientRequest prepare(ChatClientRequest request, Attempt attempt) {
		var options = request.prompt().getOptions();
		List<Message> messages = new ArrayList<>(request.prompt().getInstructions());

		if (options instanceof ToolCallingChatOptions tools && tools.getToolCallbacks() != null
				&& tools.getToolCallbacks().stream().anyMatch(tool -> RESULT_TOOL_NAME.equals(tool.getToolDefinition().name()))) {
			throw new StructuredOutputException("Tool name is reserved for structured output: " + RESULT_TOOL_NAME);
		}
		if (options instanceof StructuredOutputChatOptions nativeOptions
				&& (attempt.strategy() == NATIVE || nativeOptions.getOutputSchema() != null)) {
			// Rebuild from the original options on every strategy change.
			options = nativeOptions.mutate().outputSchema(attempt.strategy() == NATIVE ? format.jsonSchema() : null).build();
		}
		if (attempt.strategy() == TOOL && options instanceof ToolCallingChatOptions tools) {
			List<ToolCallback> callbacks = new ArrayList<>(tools.getToolCallbacks() == null
					? List.of() : tools.getToolCallbacks());
			callbacks.add(resultTool);
			options = tools.mutate().toolCallbacks(callbacks).build();
			appendInstructions.accept(messages, "When ready to give the final answer, call " + RESULT_TOOL_NAME
					+ " exactly once. Use any needed business tools before submitting the final answer.");
		}
		else if (attempt.strategy() == PROMPT) {
			appendInstructions.accept(messages, format.instructions());
		}

		messages.addAll(attempt.feedback());
		return new ChatClientRequest(new Prompt(messages, options), request.context());
	}

	private ChatClientResponse accept(ChatClientResponse clientResponse, Attempt attempt) {
		var response = clientResponse.chatResponse();
		var generation = response == null ? null : response.getResult();
		if (generation == null || generation.getOutput() == null) {
			throw new StructuredOutputException("The model returned no structured answer");
		}
		var message = generation.getOutput();
		if (GuardrailRefusal.isRefusal(generation)
				|| generation.getMetadata().getFinishReasonCategory() == FinishReasonCategory.FILTERED) {
			return message.hasToolCalls() ? replace(clientResponse, AssistantMessage.builder().properties(message.getMetadata())
					.parts(message.getParts().stream().filter(part -> !(part instanceof ToolCallPart)).toList())
					.build()) : clientResponse;
		}

		var resultCalls = message.getToolCalls().stream()
				.filter(call -> RESULT_TOOL_NAME.equals(call.name())).toList();
		if (message.getToolCalls().size() > resultCalls.size()) {
			if (attempt.retries() > 0) {
				throw new StructuredOutputException("Submit the corrected final answer without calling business tools");
			}
			// A batch with business calls is not terminal. Keep only business calls for
			// the graph, so the result tool cannot be dispatched or sent for approval.
			return resultCalls.isEmpty() ? clientResponse : replace(clientResponse, AssistantMessage.builder()
					.properties(message.getMetadata())
					.parts(message.getParts().stream().filter(part -> !(part instanceof ToolCallPart call)
							|| !RESULT_TOOL_NAME.equals(call.toolCall().name())).toList()).build());
		}
		if (generation.getMetadata().getFinishReasonCategory() == FinishReasonCategory.TRUNCATED) {
			throw new StructuredOutputException("The structured answer was truncated");
		}
		String json;
		if (attempt.strategy() == TOOL) {
			if (resultCalls.size() != 1) {
				throw new StructuredOutputException("Call " + RESULT_TOOL_NAME + " exactly once to submit the final answer");
			}
			json = format.validate(resultCalls.get(0).arguments());
		}
		else {
			if (!resultCalls.isEmpty()) {
				throw new StructuredOutputException("Return the structured answer as JSON");
			}
			json = format.validate(message.getText());
		}

		var properties = new HashMap<>(message.getMetadata());
		properties.put(RESULT_METADATA, Map.of("agent", agentName, "executionId", executionId, "json", json));
		var parts = new ArrayList<>(message.getParts().stream()
				.filter(part -> !(part instanceof ToolCallPart || part instanceof TextPart)).toList());
		parts.add(TextPart.of(json));
		return replace(clientResponse, AssistantMessage.builder().parts(parts).properties(properties).build());
	}

	private static ChatClientResponse replace(ChatClientResponse original, AssistantMessage replacement) {
		var response = original.chatResponse();
		var generations = new ArrayList<>(response.getResults());
		generations.set(0, new Generation(replacement, response.getResult().getMetadata()));
		return new ChatClientResponse(new ChatResponse(generations, response.getMetadata()), original.context());
	}

	private Attempt retry(Attempt attempt, ChatClientResponse response, StructuredOutputException error) {
		if (attempt.retries() >= format.maxRetries()) {
			throw error;
		}
		var generation = response.chatResponse() == null ? null : response.chatResponse().getResult();
		var message = generation == null ? null : generation.getOutput();
		String correction = "Correct the structured answer. " + error.getMessage();
		List<Message> feedback = List.of(new UserMessage(correction));
		if (attempt.strategy() == TOOL && message != null && message.hasToolCalls()) {
			var ids = new HashSet<String>();
			var calls = message.getToolCalls();
			if (calls.stream().allMatch(call -> RESULT_TOOL_NAME.equals(call.name())
					&& StringUtils.hasText(call.id()) && ids.add(call.id()))) {
				var results = calls.stream().map(call -> new ToolResultPart(call.id(), call.name(),
						List.of(TextPart.of(correction)), true, null, Map.of())).toList();
				feedback = List.of(message, ToolResponseMessage.builder().results(results).build());
			}
		}
		return new Attempt(attempt.strategy(), attempt.retries() + 1, feedback);
	}

	private Attempt fallback(ChatClientRequest request, Attempt attempt, RuntimeException error) {
		if (format.strategy() != AUTO || !isUnsupported(attempt.strategy(), error)) {
			throw error;
		}
		var next = attempt.strategy() == NATIVE && supportsTools(request.prompt().getOptions()) ? TOOL : PROMPT;
		return new Attempt(next, attempt.retries(), List.of());
	}

	private static boolean isUnsupported(StructuredOutputStrategy strategy, Throwable error) {
		if (strategy == PROMPT) {
			return false;
		}
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);
			boolean unsupported = message.contains("not supported") || message.contains("does not support")
					|| message.contains("doesn't support") || message.contains("unsupported")
					|| message.contains("not permitted") || message.contains("not allowed")
					|| message.contains("not available") || message.contains("not enabled")
					|| message.contains("only supported") || message.contains("unknown format")
					|| message.contains("unknown parameter");
			if (unsupported && (strategy == NATIVE
					? message.contains("json_schema") || message.contains("json schema")
								|| message.contains("response_format") || message.contains("response format")
								|| message.contains("json mode") || message.contains("structured output")
					: message.contains("tool calling") || message.contains("tools") || message.contains("function calling"))) {
				return true;
			}
		}
		return false;
	}

	private record Attempt(StructuredOutputStrategy strategy, int retries, List<Message> feedback) {
	}

}
