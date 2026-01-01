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
package com.alibaba.cloud.ai.graph.action;

import com.alibaba.cloud.ai.graph.HasMetadata;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.utils.CollectionsUtils;

import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Represents the metadata associated with a graph execution interruption. This class is
 * immutable and captures the state of the graph at the point of interruption, the node
 * where the interruption occurred, and any additional custom metadata.
 *
 */
public final class InterruptionMetadata extends NodeOutput implements HasMetadata<InterruptionMetadata.Builder> {

	private final Map<String, Object> metadata;

	private List<AssistantMessage.ToolCall> toolsAutomaticallyApproved;

	private List<ToolFeedback> toolFeedbacks;

	private InterruptionMetadata(Builder builder) {
		super(builder.nodeId, builder.state);
		this.metadata = builder.metadata();
		this.toolFeedbacks = new ArrayList<>(builder.toolFeedbacks);
		if (builder.toolsAutomaticallyApproved != null) {
			this.toolsAutomaticallyApproved = builder.toolsAutomaticallyApproved;
		} else {
			this.toolsAutomaticallyApproved = new ArrayList<>();
		}
	}

	/**
	 * Retrieves a metadata value associated with the specified key.
	 * @param key the key whose associated value is to be returned
	 * @return an {@link Optional} containing the value to which the specified key is
	 * mapped, or an empty {@link Optional} if this metadata contains no mapping for the
	 * key.
	 */
	@Override
	public Optional<Object> metadata(String key) {
		return ofNullable(metadata).map(m -> m.get(key));
	}

	@Override
	public Optional<Map<String, Object>> metadata() {
		return Optional.of(metadata);
	}

	public List<ToolFeedback> toolFeedbacks() {
		if (toolFeedbacks == null) {
			return new ArrayList<>();
		}
		return toolFeedbacks;
	}

	public List<AssistantMessage.ToolCall> getToolsAutomaticallyApproved() {
		return toolsAutomaticallyApproved;
	}

	@Override
	public String toString() {
		return String.format("""
				InterruptionMetadata{
				\tnodeId='%s',
				\tstate=%s,
				\tmetadata=%s
				}""", node(), state(), CollectionsUtils.toString(metadata));
	}

	/**
	 * Creates a new builder for {@link InterruptionMetadata}.
	 * @return a new {@link Builder} instance
	 */
	public static Builder builder(String nodeId, OverAllState state) {
		return new Builder(nodeId, state);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder(InterruptionMetadata interruptionMetadata) {
		Builder builder = new Builder(interruptionMetadata.metadata().orElse(Map.of()))
			.nodeId(interruptionMetadata.node())
			.state(interruptionMetadata.state());
		if (interruptionMetadata.getToolsAutomaticallyApproved() != null) {
			builder.toolsAutomaticallyApproved(interruptionMetadata.getToolsAutomaticallyApproved());
		}
		// if (interruptionMetadata.toolFeedbacks() != null && !interruptionMetadata.toolFeedbacks().isEmpty()) {
		// 	builder.toolFeedbacks(interruptionMetadata.toolFeedbacks());
		// }
		return builder;
	}

	/**
	 * A builder for creating instances of {@link InterruptionMetadata}.
	 *
	 */
	public static class Builder extends HasMetadata.Builder<Builder> {
		List<ToolFeedback> toolFeedbacks;

		List<AssistantMessage.ToolCall> toolsAutomaticallyApproved;

		String nodeId;

		OverAllState state;

		public Builder() {
			this.toolFeedbacks = new ArrayList<>();
		}

		/**
		 * Constructs a new builder.
		 *
		 */
		public Builder(String nodeId, OverAllState state) {
			this.nodeId = nodeId;
			this.state = state;
			this.toolFeedbacks = new ArrayList<>();
		}

		public Builder(Map<String, Object> metadata) {
			super(metadata);
			this.toolFeedbacks = new ArrayList<>();
		}

		public Builder nodeId(String nodeId) {
			this.nodeId = nodeId;
			return this;
		}

		public Builder state(OverAllState state) {
			this.state = state;
			return this;
		}

		public Builder addToolFeedback(ToolFeedback toolFeedback) {
			this.toolFeedbacks.add(toolFeedback);
			return this;
		}

		public Builder toolFeedbacks(List<ToolFeedback> toolFeedbacks) {
			this.toolFeedbacks = new ArrayList<>(toolFeedbacks);
			return this;
		}

		public Builder addToolsAutomaticallyApproved(AssistantMessage.ToolCall toolCall) {
			if (this.toolsAutomaticallyApproved == null) {
				this.toolsAutomaticallyApproved = new ArrayList<>();
			}
			this.toolsAutomaticallyApproved.add(toolCall);
			return this;
		}

		public Builder toolsAutomaticallyApproved(List<AssistantMessage.ToolCall> toolsAutomaticallyApproved) {
			this.toolsAutomaticallyApproved = new ArrayList<>(toolsAutomaticallyApproved);
			return this;
		}

		/**
		 * Builds the {@link InterruptionMetadata} instance.
		 * @return a new, immutable {@link InterruptionMetadata} instance
		 */
		public InterruptionMetadata build() {
			return new InterruptionMetadata(this);
		}

	}

	public static class ToolFeedback {
		String id;
		String name;
		String arguments;
		FeedbackResult result;
		String description;

		public ToolFeedback(String id, String name, String arguments, FeedbackResult result, String description) {
			this.id = id;
			this.name = name;
			this.arguments = arguments;
			this.result = result;
			this.description = description;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getArguments() {
			return arguments;
		}

		public FeedbackResult getResult() {
			return result;
		}

		public String getDescription() {
			return description;
		}

		public static Builder builder() {
			return new Builder();
		}

		public static Builder builder(ToolFeedback toolFeedback) {
			return new Builder()
				.id(toolFeedback.getId())
				.name(toolFeedback.getName())
				.arguments(toolFeedback.getArguments())
				.result(toolFeedback.getResult())
				.description(toolFeedback.getDescription());
		}

		public static class Builder {
			String id;
			String name;
			String arguments;
			FeedbackResult result;
			String description;

			public Builder id(String id) {
				this.id = id;
				return this;
			}

			public Builder name(String name) {
				this.name = name;
				return this;
			}

			public Builder arguments(String arguments) {
				this.arguments = arguments;
				return this;
			}

			public Builder result(FeedbackResult result) {
				this.result = result;
				return this;
			}

			public Builder type(String type) {
				this.result = FeedbackResult.valueOf(type.toUpperCase());
				return this;
			}

			public Builder description(String description) {
				this.description = description;
				return this;
			}

			public ToolFeedback build() {
				return new ToolFeedback(id, name, arguments, result, description);
			}
		}

		public enum FeedbackResult {
			APPROVED,
			REJECTED,
			EDITED;
		}
	}

}
