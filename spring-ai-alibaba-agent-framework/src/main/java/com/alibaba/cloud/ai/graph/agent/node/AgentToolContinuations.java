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

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.checkpoint.InterruptionCheckpointCodec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class AgentToolContinuations {

	private final Map<String, PausedCall> pending;
	private final InterruptionMetadata feedback;
	private final String selected;
	private final Boolean allReturnDirect;
	private final String executionId;
	private final boolean savedState;

	private AgentToolContinuations(Map<String, PausedCall> pending, InterruptionMetadata feedback,
			Boolean allReturnDirect, String executionId, boolean savedState) {
		this.pending = Map.copyOf(pending);
		this.feedback = feedback;
		this.selected = select(pending, feedback);
		this.allReturnDirect = allReturnDirect;
		this.executionId = executionId;
		this.savedState = savedState;
	}

	@SuppressWarnings("unchecked")
	static AgentToolContinuations load(OverAllState state, String stateKey, RunnableConfig config) {
		var saved = state.<Map<String, Object>>value(stateKey).orElse(Map.of());
		String executionId = state.<String>value(GraphLifecycleListener.EXECUTION_ID_KEY).orElse("");
		boolean sameExecution = executionId.equals(saved.get("executionId"));
		var calls = sameExecution ? (Map<String, Map<String, Object>>) saved.getOrDefault("calls", Map.of())
				: Map.<String, Map<String, Object>>of();
		var pending = new LinkedHashMap<String, PausedCall>();
		calls.forEach((key, call) -> pending.put(key, PausedCall.decode(call)));
		var feedback = config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY)
				.filter(InterruptionMetadata.class::isInstance).map(InterruptionMetadata.class::cast).orElse(null);
		return new AgentToolContinuations(pending, feedback, sameExecution ? (Boolean) saved.get("allReturnDirect") : null,
				executionId, !saved.isEmpty());
	}

	boolean hasSavedState() {
		return savedState;
	}

	boolean isWaiting(String key) {
		return pending.containsKey(key) && !key.equals(selected);
	}

	Boolean allReturnDirect() {
		return allReturnDirect;
	}

	Map<String, PausedCall> pending() {
		return pending;
	}

	AgentTool.Continuation continuation(String key, String toolName, String arguments) {
		if (!key.equals(selected)) {
			return null;
		}
		var paused = pending.get(key);
		if (!paused.toolName().equals(toolName) || !Objects.equals(paused.arguments(), arguments)) {
			throw new IllegalStateException("Resumed agent tool call '" + key + "' differs from the paused request");
		}
		return new AgentTool.Continuation(paused.threadId(), feedback);
	}

	Map<String, Object> save(Map<String, PausedCall> pending, Boolean allReturnDirect) {
		var encoded = new LinkedHashMap<String, Object>();
		pending.forEach((key, call) -> encoded.put(key, call.encode()));
		return allReturnDirect == null ? Map.of("calls", encoded, "executionId", executionId)
				: Map.of("calls", encoded, "executionId", executionId, "allReturnDirect", allReturnDirect);
	}

	static InterruptionMetadata interruption(List<InterruptionMetadata> interruptions) {
		var first = interruptions.get(0);
		return interruptions.size() == 1 ? first : InterruptionMetadata.builder(first).toolFeedbacks(first.toolFeedbacks())
				.pendingInterruptions(interruptions).build();
	}

	private static String select(Map<String, PausedCall> pending, InterruptionMetadata feedback) {
		if (pending.isEmpty() || feedback == null) {
			return null;
		}
		var id = feedback.interruptionId();
		if (id.isEmpty()) {
			var first = pending.entrySet().iterator().next();
			if (pending.size() == 1 && first.getValue().interruption().pendingInterruptions().size() <= 1) {
				return first.getKey();
			}
			throw new IllegalArgumentException("Feedback for multiple paused agent tools requires an interruption ID");
		}
		return pending.entrySet().stream()
				.filter(call -> contains(call.getValue().interruption(), id.get()))
				.map(Map.Entry::getKey).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No agent tool is paused at interruption '" + id.get() + "'"));
	}

	private static boolean contains(InterruptionMetadata interruption, String id) {
		return interruption.interruptionId().filter(id::equals).isPresent()
				|| interruption.pendingInterruptions().stream().anyMatch(child -> contains(child, id));
	}

	record PausedCall(String threadId, String toolName, String arguments, InterruptionMetadata interruption) {

		static PausedCall from(AgentTool.Interrupted pause, String toolName, String arguments) {
			var interruption = pause.interruption();
			if (interruption.interruptionId().isEmpty()) {
				interruption = InterruptionMetadata.builder(interruption).toolFeedbacks(interruption.toolFeedbacks())
						.interruptionId(UUID.randomUUID().toString()).build();
			}
			return new PausedCall(pause.threadId(), toolName, arguments, interruption);
		}

		Map<String, Object> encode() {
			var data = new LinkedHashMap<String, Object>();
			data.put("threadId", threadId);
			data.put("toolName", toolName);
			data.put("arguments", arguments);
			data.put("interruption", InterruptionCheckpointCodec.encodePause(interruption));
			return data;
		}

		@SuppressWarnings("unchecked")
		static PausedCall decode(Map<String, Object> data) {
			return new PausedCall((String) data.get("threadId"), (String) data.get("toolName"),
					(String) data.get("arguments"),
					InterruptionCheckpointCodec.decodePause((Map<String, Object>) data.get("interruption")));
		}

	}

}
