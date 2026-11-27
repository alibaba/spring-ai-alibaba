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

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** An event from one invocation of a graph, ordered by delivery within that invocation. */
public record RunEvent(int schemaVersion, String runId, long sequence, Type type, Scope scope, Data data) {

	public enum Channel {
		LIFECYCLE, MESSAGES, TOOLS, CUSTOM, UPDATES, VALUES, CHECKPOINTS
	}

	public enum Type {
		RUN_STARTED(Channel.LIFECYCLE), RUN_COMPLETED(Channel.LIFECYCLE), RUN_FAILED(Channel.LIFECYCLE),
		RUN_PAUSED(Channel.LIFECYCLE), RUN_CANCELLED(Channel.LIFECYCLE), NODE_STARTED(Channel.LIFECYCLE),
		NODE_COMPLETED(Channel.LIFECYCLE), NODE_FAILED(Channel.LIFECYCLE), NODE_PAUSED(Channel.LIFECYCLE),
		NODE_CANCELLED(Channel.LIFECYCLE), ATTEMPT(Channel.LIFECYCLE), TRUNCATED(Channel.LIFECYCLE),
		CANDIDATE(Channel.LIFECYCLE),
		MODEL_DELTA(Channel.MESSAGES), MODEL_COMPLETED(Channel.MESSAGES),
		TOOL_STARTED(Channel.TOOLS), TOOL_DELTA(Channel.TOOLS), TOOL_COMPLETED(Channel.TOOLS),
		TOOL_FAILED(Channel.TOOLS), TOOL_PAUSED(Channel.TOOLS), TOOL_CANCELLED(Channel.TOOLS), CUSTOM(Channel.CUSTOM),
		STATE_UPDATE(Channel.UPDATES), STATE_VALUE(Channel.VALUES), CHECKPOINT_SAVED(Channel.CHECKPOINTS);

		private final Channel channel;

		Type(Channel channel) {
			this.channel = channel;
		}

		public Channel channel() {
			return channel;
		}
	}

	public enum ScopeKind {
		GRAPH, AGENT, INLINE_GRAPH, BRANCH, NODE, TOOL
	}

	public record PathSegment(ScopeKind kind, String name, String id) {
	}

	public record Scope(List<PathSegment> namespace, String nodeId, String nodeExecutionId, String taskId,
			Integer attempt) {

		public Scope {
			namespace = List.copyOf(namespace);
		}

		public String scopeId() {
			return namespace.get(namespace.size() - 1).id();
		}

		public boolean root() {
			return namespace.size() == 1;
		}
	}

	public sealed interface Data permits Lifecycle, Failure, State, StateUpdate, Paused, Checkpoint, Custom, ModelDelta,
			ModelCompleted, Tool, Truncated, Attempt, CandidateStatus {
	}

	public record Lifecycle(String threadId, String checkpointId, String resumedInterruptionId) implements Data {
	}

	public record Failure(String errorType, String message) implements Data {
	}

	public record State(Map<String, Object> values) implements Data {

		public State {
			values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
		}
	}

	public record StateUpdate(List<Map<String, Object>> writes) implements Data {

		public StateUpdate {
			writes = writes.stream().map(write -> new State(write).values()).toList();
		}
	}

	public record Paused(InterruptionMetadata interruption, String checkpointId) implements Data {
	}

	public record Checkpoint(String checkpointId, String nodeId, String nextNodeId) implements Data {
	}

	public record Custom(String name, Object value) implements Data {
	}

	public record ToolArgument(int index, String id, String name, String arguments) {
	}

	public record ModelDelta(String modelCallId, Integer partIndex, String text, String reasoning,
			ToolArgument toolArgument) implements Data {
	}

	public record ModelCompleted(String modelCallId, AssistantMessage message, Integer inputTokens,
			Integer outputTokens, Integer totalTokens) implements Data {
	}

	public record Tool(String toolCallId, int toolCallIndex, String name, Object value) implements Data {
	}

	public record Truncated(long omitted) implements Data {
	}

	public record Attempt(String phase, Failure failure) implements Data {
	}

	public record CandidateStatus(Phase phase) implements Data {

		public enum Phase {
			STARTED, PAUSED, FAILED, CANCELLED, SELECTED
		}
	}
}
