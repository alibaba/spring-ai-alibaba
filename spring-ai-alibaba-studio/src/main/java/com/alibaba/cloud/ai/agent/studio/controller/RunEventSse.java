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
package com.alibaba.cloud.ai.agent.studio.controller;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.streaming.RunEvent;
import com.alibaba.cloud.ai.graph.streaming.StreamOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.part.ReasoningPart;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

final class RunEventSse {

	private RunEventSse() {
	}

	record Options(boolean reasoning, boolean toolPayload, Set<String> stateKeys) {

		Options {
			stateKeys = stateKeys == null ? Set.of() : Set.copyOf(stateKeys);
		}

		StreamOptions streamOptions() {
			var defaults = StreamOptions.all();
			return new StreamOptions(defaults.channels(), true, reasoning, toolPayload, defaults.bufferCapacity(),
					defaults.candidateByteBudget());
		}
	}

	static Flux<ServerSentEvent<String>> encode(Flux<RunEvent> events, ObjectMapper mapper, Options options) {
		return events.map(event -> {
			String type = event.type().name().toLowerCase(Locale.ROOT);
			var data = new Body(event.schemaVersion(), event.runId(), event.sequence(), type, event.scope().scopeId(),
					event.scope(), payload(event.data(), options));
			try {
				return ServerSentEvent.<String>builder(mapper.writeValueAsString(data))
						.id(event.runId() + ":" + event.sequence()).event(type).build();
			}
			catch (JsonProcessingException error) {
				throw new IllegalStateException("Cannot encode the run event", error);
			}
		});
	}

	private static Object payload(RunEvent.Data data, Options options) {
		if (data instanceof RunEvent.State state) {
			return stateValues(state.values(), options);
		}
		if (data instanceof RunEvent.StateUpdate update) {
			return Map.of("writes", update.writes().stream().map(write -> stateValues(write, options)).toList());
		}
		if (data instanceof RunEvent.ModelDelta model) {
			return new RunEvent.ModelDelta(model.modelCallId(), model.partIndex(), model.text(),
					options.reasoning() ? model.reasoning() : null, options.toolPayload() ? model.toolArgument() : null);
		}
		if (data instanceof RunEvent.ModelCompleted model) {
			return modelResult(model, options);
		}
		if (data instanceof RunEvent.Paused paused) {
			return pause(paused.interruption(), paused.checkpointId(), options);
		}
		if (data instanceof RunEvent.Tool tool) {
			return new RunEvent.Tool(tool.toolCallId(), tool.toolCallIndex(), tool.name(),
					options.toolPayload() ? visibleValue(tool.value(), 0) : null);
		}
		if (data instanceof RunEvent.Custom custom) {
			return new RunEvent.Custom(custom.name(), visibleValue(custom.value(), 0));
		}
		if (data instanceof RunEvent.Failure failure) {
			return new RunEvent.Failure(failure.errorType(), null);
		}
		if (data instanceof RunEvent.Attempt attempt && attempt.failure() != null) {
			return new RunEvent.Attempt(attempt.phase(), new RunEvent.Failure(attempt.failure().errorType(), null));
		}
		return data;
	}

	private static ModelResult modelResult(RunEvent.ModelCompleted model, Options options) {
		var message = model.message();
		String reasoning = null;
		if (options.reasoning()) {
			reasoning = message.getParts().stream().filter(ReasoningPart.class::isInstance).map(ReasoningPart.class::cast)
					.map(ReasoningPart::text).filter(Objects::nonNull).collect(Collectors.joining());
			if (reasoning.isEmpty() && message.getMetadata().get("reasoningContent") instanceof String legacy) {
				reasoning = legacy;
			}
		}
		var calls = new ArrayList<RunEvent.ToolArgument>();
		if (options.toolPayload()) {
			for (var call : message.getToolCalls()) {
				calls.add(new RunEvent.ToolArgument(calls.size(), call.id(), call.name(), call.arguments()));
			}
		}
		return new ModelResult(model.modelCallId(), message.getText(), reasoning, calls,
				model.inputTokens(), model.outputTokens(), model.totalTokens());
	}

	private static Map<String, Object> stateValues(Map<String, Object> state, Options options) {
		var selected = new LinkedHashMap<String, Object>();
		state.forEach((key, value) -> {
			if (!key.startsWith("_") && options.stateKeys().contains(key)) {
				selected.put(key, visibleValue(value, 0));
			}
		});
		return selected;
	}

	private static Map<String, Object> pause(InterruptionMetadata pause, String checkpointId, Options options) {
		var payload = new LinkedHashMap<String, Object>();
		payload.put("interruptionId", pause.interruptionId().orElse(null));
		payload.put("nodeId", pause.node());
		payload.put("checkpointId", checkpointId);
		payload.put("pendingInterruptions", pause.pendingInterruptions().stream()
				.map(child -> pause(child, null, options)).toList());
		payload.put("toolFeedbacks", pause.toolFeedbacks().stream().map(tool -> {
			var feedback = new LinkedHashMap<String, Object>();
			feedback.put("id", tool.getId());
			feedback.put("name", tool.getName());
			feedback.put("allowedDecisions", tool.getAllowedDecisions());
			if (options.toolPayload()) {
				feedback.put("arguments", tool.getArguments());
				feedback.put("description", tool.getDescription());
			}
			return feedback;
		}).toList());
		return payload;
	}

	private static Object visibleValue(Object value, int depth) {
		if (depth > 8) {
			return null;
		}
		if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
			return value;
		}
		if (value instanceof Message message) {
			return message.getText();
		}
		if (value instanceof Map<?, ?> map) {
			var visible = new LinkedHashMap<String, Object>();
			map.forEach((key, entry) -> {
				if (key instanceof String text && !text.startsWith("_")) {
					visible.put(text, visibleValue(entry, depth + 1));
				}
			});
			return visible;
		}
		if (value instanceof Iterable<?> entries) {
			var visible = new ArrayList<>();
			for (Object entry : entries) {
				visible.add(visibleValue(entry, depth + 1));
			}
			return visible;
		}
		return null;
	}

	private record Body(int schemaVersion, String runId, long sequence, String type, String scopeId,
			RunEvent.Scope scope, Object data) {
	}

	private record ModelResult(String modelCallId, String text, String reasoning, List<RunEvent.ToolArgument> toolCalls,
			Integer inputTokens, Integer outputTokens,
			Integer totalTokens) {
	}
}
