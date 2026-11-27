/*
 * Copyright 2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.streaming;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.part.ReasoningPart;
import org.springframework.ai.chat.messages.part.StreamingParts;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.alibaba.cloud.ai.graph.streaming.RunEvent.Channel;
import static com.alibaba.cloud.ai.graph.streaming.RunEvent.Type;

/** Projects one model invocation from the same responses used for state aggregation. */
public final class ModelEvents {

	private static final String FINAL_RESPONSE = "com.alibaba.cloud.ai.graph.model.finalResponse";

	private final RunEventContext context;

	private final String id = UUID.randomUUID().toString();

	private final Map<Integer, String> arguments = new HashMap<>();

	private final Map<String, Integer> indexes = new HashMap<>();

	public ModelEvents(RunEventContext context) {
		this.context = context;
	}

	public Mono<Void> chunk(ChatResponse response) {
		var generation = response.getResult();
		if (response.getMetadata().containsKey(FINAL_RESPONSE) || generation == null || generation.getOutput() == null) {
			return Mono.empty();
		}
		return chunk(generation.getOutput());
	}

	public Mono<Void> chunk(AssistantMessage message) {
		var deltas = new ArrayList<RunEvent.ModelDelta>();
		int callIndex = 0;
		boolean reasoningPart = false;
		for (var part : message.getParts()) {
			Integer index = StreamingParts.partIndex(part);
			if (part instanceof TextPart text && text.text() != null && !text.text().isEmpty()) {
				deltas.add(new RunEvent.ModelDelta(id, index, text.text(), null, null));
			}
			else if (part instanceof ReasoningPart reasoning) {
				reasoningPart = true;
				if (context.options().includeReasoning()) {
					deltas.add(new RunEvent.ModelDelta(id, index, null, reasoning.text(), null));
				}
			}
			else if (part instanceof ToolCallPart tool && context.options().includeToolArguments()) {
				deltas.add(toolArgument(tool, index, callIndex++));
			}
		}
		if (!reasoningPart && context.options().includeReasoning()
				&& message.getMetadata().get("reasoningContent") instanceof String reasoning && !reasoning.isEmpty()) {
			deltas.add(new RunEvent.ModelDelta(id, null, null, reasoning, null));
		}
		if (deltas.isEmpty()) {
			return Mono.empty();
		}
		if (deltas.size() == 1) {
			return context.emitReactive(Type.MODEL_DELTA, deltas.get(0));
		}
		return Flux.fromIterable(deltas).concatMap(delta -> context.emitReactive(Type.MODEL_DELTA, delta), 1).then();
	}

	/** Marks a response that an output filter has already assembled. */
	public static ChatResponse finalResponse(ChatResponse response) {
		var source = response.getMetadata();
		var metadata = ChatResponseMetadata.builder().id(source.getId()).model(source.getModel())
				.usage(source.getUsage()).rateLimit(source.getRateLimit()).promptMetadata(source.getPromptMetadata());
		source.entrySet().forEach(entry -> metadata.keyValue(entry.getKey(), entry.getValue()));
		return new ChatResponse(response.getResults(), metadata.keyValue(FINAL_RESPONSE, true).build());
	}

	private RunEvent.ModelDelta toolArgument(ToolCallPart part, Integer contentIndex, int position) {
		var call = part.toolCall();
		int index = contentIndex == null ? position : contentIndex;
		if (call.id() != null && !call.id().isBlank()) {
			index = indexes.computeIfAbsent(call.id(), ignored -> contentIndex == null ? position : contentIndex);
		}
		String current = call.arguments();
		String delta = current;
		if (current != null && contentIndex == null && !StreamingParts.isPartial(part)) {
			String previous = arguments.getOrDefault(index, "");
			String aggregate = current.startsWith(previous) ? current
					: previous.startsWith(current) ? previous : previous + current;
			delta = aggregate.substring(previous.length());
			arguments.put(index, aggregate);
		}
		return new RunEvent.ModelDelta(id, contentIndex, null, null,
				new RunEvent.ToolArgument(index, call.id(), call.name(), delta));
	}

	public Mono<Void> completedAsync(AssistantMessage message, Usage usage) {
		return context.emitReactive(Type.MODEL_COMPLETED, completion(message, usage));
	}

	private RunEvent.ModelCompleted completion(AssistantMessage message, Usage usage) {
		var visible = message;
		if (!context.options().includeReasoning() || !context.options().includeToolArguments()) {
			var builder = AssistantMessage.builder().parts(message.getParts().stream()
					.filter(part -> context.options().includeReasoning() || !(part instanceof ReasoningPart))
					.filter(part -> context.options().includeToolArguments() || !(part instanceof ToolCallPart)).toList());
			var metadata = new HashMap<>(message.getMetadata());
			if (!context.options().includeReasoning()) {
				metadata.remove("reasoningContent");
			}
			visible = builder.properties(metadata).build();
		}
		return new RunEvent.ModelCompleted(id, visible,
				usage == null ? null : usage.getPromptTokens(), usage == null ? null : usage.getCompletionTokens(),
				usage == null ? null : usage.getTotalTokens());
	}

	public static void completed(RunEventContext context, AssistantMessage message, Usage usage) {
		if (context.enabled(Channel.MESSAGES)) {
			context.emit(Type.MODEL_COMPLETED, new ModelEvents(context).completion(message, usage));
		}
	}
}
