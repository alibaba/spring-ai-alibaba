/*
 * Copyright 2024-2025 the original author or authors.
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

import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * Represents the metadata associated with a graph execution interruption. This class is
 * immutable and captures the state of the graph at the point of interruption, the node
 * where the interruption occurred, and any additional custom metadata.
 *
 */
public final class InterruptionMetadata extends NodeOutput implements HasMetadata<InterruptionMetadata.Builder> {

	private final Map<String, Object> metadata;

	private InterruptionMetadata(Builder builder) {
		super(requireNonNull(builder.nodeId, "nodeId cannot be null!"),
				requireNonNull(builder.state, "state cannot be null!"));
		this.metadata = builder.metadata();
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

	/**
	 * A builder for creating instances of {@link InterruptionMetadata}.
	 *
	 */
	public static class Builder extends HasMetadata.Builder<Builder> {

		final String nodeId;

		final OverAllState state;

		/**
		 * Constructs a new builder.
		 *
		 */
		public Builder(String nodeId, OverAllState state) {
			this.nodeId = nodeId;
			this.state = state;
		}

		/**
		 * Builds the {@link InterruptionMetadata} instance.
		 * @return a new, immutable {@link InterruptionMetadata} instance
		 */
		public InterruptionMetadata build() {
			return new InterruptionMetadata(this);
		}

	}

}
