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

package com.alibaba.cloud.ai.graph.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.part.MediaPart;
import org.springframework.ai.chat.messages.part.MessagePart;
import org.springframework.ai.chat.messages.part.StreamingParts;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Assembles a model turn using Spring AI's Message Parts and handles legacy tool
 * argument fragments and media chunks that have no part index.
 */
public final class ChatResponseStreamAggregator {

	private ChatResponseStreamAggregator() {
	}

	public static Mono<ChatResponse> aggregate(Flux<ChatResponse> responses, boolean mergeReasoningContent) {
		return Mono.defer(() -> aggregateSubscription(responses, mergeReasoningContent));
	}

	private static Mono<ChatResponse> aggregateSubscription(Flux<ChatResponse> responses, boolean mergeReasoningContent) {
		List<ToolCallPart> toolCalls = new ArrayList<>();
		List<MediaPart> media = new ArrayList<>();
		Map<String, Object> reasoningMetadata = new LinkedHashMap<>();
		AtomicReference<ChatResponseMetadata> lastMetadata = new AtomicReference<>();
		AtomicReference<ChatResponse> result = new AtomicReference<>();
		AtomicReference<AssistantMessage> firstOutput = new AtomicReference<>();
		AtomicInteger outputs = new AtomicInteger();
		Flux<ChatResponse> normalized = responses.map(response -> {
			lastMetadata.set(response.getMetadata());
			Generation generation = response.getResult();
			if (generation == null || generation.getOutput() == null) {
				return new ChatResponse(List.of(), response.getMetadata());
			}
			AssistantMessage output = generation.getOutput();
			if (outputs.getAndIncrement() == 0 && !response.getMetadata().containsKey("toolCalls")) {
				firstOutput.set(output);
			}
			List<MessagePart> parts = new ArrayList<>();
			List<ToolCallPart> legacyCalls = new ArrayList<>();
			for (MessagePart part : output.getParts()) {
				if (StreamingParts.partIndex(part) == null && part instanceof ToolCallPart call
						&& !StreamingParts.isPartial(part)) {
					legacyCalls.add(call);
				}
				else if (StreamingParts.partIndex(part) == null && part instanceof MediaPart attachment) {
					media.add(attachment);
				}
				else {
					parts.add(part);
				}
			}
			mergeToolCalls(toolCalls, legacyCalls);
			if (mergeReasoningContent) {
				mergeReasoningMetadata(reasoningMetadata, output.getMetadata());
			}
			AssistantMessage normalizedMessage = AssistantMessage.builder().parts(parts)
					.properties(output.getMetadata()).build();
			return new ChatResponse(List.of(new Generation(normalizedMessage, generation.getMetadata())),
					response.getMetadata());
		});
		return new MessageAggregator().aggregate(normalized, aggregated -> {
			if (outputs.get() == 0) {
				return;
			}
			Generation generation = aggregated.getResult();
			AssistantMessage output = generation.getOutput();
			List<MessagePart> parts = new ArrayList<>(output.getParts());
			parts.addAll(toolCalls);
			parts.addAll(media);
			Map<String, Object> metadata = new LinkedHashMap<>(output.getMetadata());
			metadata.putAll(reasoningMetadata);
			AssistantMessage original = firstOutput.get();
			// A provider or an output filter can supply one already assembled message.
			// Keep its explicit part order when there are no streaming markers to merge.
			AssistantMessage message = outputs.get() == 1 && original != null
					&& original.getParts().stream().noneMatch(part ->
							StreamingParts.partIndex(part) != null || StreamingParts.isPartial(part))
					? original : AssistantMessage.builder().parts(parts).properties(metadata).build();
			var aggregateMetadata = aggregated.getMetadata();
			var rawMetadata = lastMetadata.get();
			var responseMetadata = ChatResponseMetadata.builder()
					.id(aggregateMetadata.getId())
					.model(aggregateMetadata.getModel())
					.usage(aggregateMetadata.getUsage())
					.rateLimit(aggregateMetadata.getRateLimit())
					.promptMetadata(aggregateMetadata.getPromptMetadata());
			rawMetadata.entrySet().forEach(entry -> responseMetadata.keyValue(entry.getKey(), entry.getValue()));
			result.set(new ChatResponse(List.of(new Generation(message, generation.getMetadata())),
					responseMetadata.build()));
		}).then(Mono.defer(() -> Mono.justOrEmpty(result.get())));
	}

	private static void mergeReasoningMetadata(Map<String, Object> aggregate, Map<String, Object> delta) {
		Object reasoning = delta.get("reasoningContent");
		if (reasoning != null) {
			aggregate.merge("reasoningContent", reasoning.toString(), (before, after) -> before.toString() + after);
		}
	}

    /**
     * Merges tool calls from two streamed assistant messages.
     * <p>
     * Streaming chunks from some providers may split tool-call fields across multiple chunks
     * (for example: first chunk has name, later chunk has only arguments and even no id).
     * We merge by id when possible, and fall back to positional merge to keep a single
     * tool call record complete.
     *
     */
    private static void mergeToolCalls(List<ToolCallPart> merged, List<ToolCallPart> currentToolCalls) {
        for (int i = 0; i < currentToolCalls.size(); i++) {
            ToolCallPart current = currentToolCalls.get(i);
            int mergeIndex = findToolCallMergeIndex(merged, current.toolCall(), i);
            if (mergeIndex >= 0) {
                ToolCallPart previous = merged.get(mergeIndex);
                Map<String, String> attributes = new LinkedHashMap<>(previous.attributes());
                attributes.putAll(current.attributes());
                merged.set(mergeIndex, new ToolCallPart(mergeToolCallFields(previous.toolCall(), current.toolCall()),
                        current.payload() != null ? current.payload() : previous.payload(), attributes));
            }
            else {
                merged.add(current);
            }
        }
    }

    private static int findToolCallMergeIndex(List<ToolCallPart> mergedCalls, ToolCall currentToolCall, int currentIndex) {
        String currentId = normalized(currentToolCall.id());
        if (StringUtils.hasText(currentId)) {
            for (int i = 0; i < mergedCalls.size(); i++) {
                if (currentId.equals(normalized(mergedCalls.get(i).toolCall().id()))) {
                    return i;
                }
            }
        }

        if (currentIndex < mergedCalls.size()
                && (!StringUtils.hasText(currentId)
                        || !StringUtils.hasText(mergedCalls.get(currentIndex).toolCall().id()))) {
            return currentIndex;
        }
        if (StringUtils.hasText(currentId)) {
            return -1;
        }

        String currentName = normalized(currentToolCall.name());
        if (StringUtils.hasText(currentName)) {
            int matchedIndex = -1;
            for (int i = 0; i < mergedCalls.size(); i++) {
                if (currentName.equals(normalized(mergedCalls.get(i).toolCall().name()))) {
                    if (matchedIndex >= 0) {
                        return -1;
                    }
                    matchedIndex = i;
                }
            }
            if (matchedIndex >= 0) {
                return matchedIndex;
            }
        }

        return -1;
    }

    private static ToolCall mergeToolCallFields(ToolCall previous, ToolCall current) {
        return new AssistantMessage.ToolCall(
                firstNonBlank(current.id(), previous.id()),
                firstNonBlank(current.type(), previous.type()),
                firstNonBlank(current.name(), previous.name()),
                mergeToolArguments(previous.arguments(), current.arguments()));
    }

    private static String mergeToolArguments(String previousArguments, String currentArguments) {
        if (!StringUtils.hasText(previousArguments)) {
            return currentArguments;
        }
        if (!StringUtils.hasText(currentArguments)) {
            return previousArguments;
        }

        if (currentArguments.equals(previousArguments)) {
            return currentArguments;
        }
        if (currentArguments.startsWith(previousArguments) || currentArguments.contains(previousArguments)) {
            return currentArguments;
        }
        if (previousArguments.startsWith(currentArguments) || previousArguments.contains(currentArguments)) {
            return previousArguments;
        }

        // Typical streaming delta case: append incremental argument fragment.
        return previousArguments + currentArguments;
    }

    private static String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private static String normalized(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

}
