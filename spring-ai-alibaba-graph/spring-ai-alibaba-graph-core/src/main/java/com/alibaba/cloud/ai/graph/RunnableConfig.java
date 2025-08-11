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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * A final class representing configuration for a runnable task. This class holds various
 * parameters such as thread ID, checkpoint ID, next node, and stream mode, providing
 * methods to modify these parameters safely without permanently altering the original
 * configuration.
 */
public final class RunnableConfig implements HasMetadata<RunnableConfig.Builder> {

	private final String threadId;

	private final String checkPointId;

	private final String nextNode;

	private final CompiledGraph.StreamMode streamMode;

	private final Map<String, Object> metadata;

	private final Map<String, Object> interruptedNodes;

	/**
	 * Returns the stream mode of the compiled graph.
	 * @return {@code StreamMode} representing the current stream mode.
	 */
	public CompiledGraph.StreamMode streamMode() {
		return streamMode;
	}

	/**
	 * Returns the thread ID as an {@link Optional}.
	 * @return the thread ID wrapped in an {@code Optional}, or an empty {@code Optional}
	 * if no thread ID is set.
	 */
	public Optional<String> threadId() {
		return ofNullable(threadId);
	}

	/**
	 * Returns the current {@code checkPointId} wrapped in an {@link Optional}.
	 * @return an {@link Optional} containing the {@code checkPointId}, or
	 * {@link Optional#empty()} if it is null.
	 */
	public Optional<String> checkPointId() {
		return ofNullable(checkPointId);
	}

	/**
	 * Returns an {@code Optional} describing the next node in the sequence, or an empty
	 * {@code Optional} if there is no such node.
	 * @return an {@code Optional} describing the next node, or an empty {@code Optional}
	 */
	public Optional<String> nextNode() {
		return ofNullable(nextNode);
	}

	/**
	 * Checks if a node is marked as interrupted in the metadata.
	 * @param nodeId the ID of the node to check for interruption status
	 * @return true if the node is marked as interrupted, false otherwise
	 */
	public boolean isInterrupted(String nodeId) {
		return interruptData(HasMetadata.formatNodeId(nodeId)).map(value -> Boolean.TRUE.equals(value)).orElse(false);
	}

	/**
	 * Marks a node as not interrupted by setting its value to false in the metadata.
	 * @param nodeId the ID of the node to mark as not interrupted
	 * @return a new {@code RunnableConfig} instance with the updated metadata
	 */
	public void withNodeResumed(String nodeId) {
		String formattedNodeId = HasMetadata.formatNodeId(nodeId);
		interruptedNodes.put(formattedNodeId, false);
	}

	/**
	 * Removes the interrupted marker for a specific node by removing its entry from the
	 * metadata.
	 * @param nodeId the ID of the node to remove the interrupted marker for
	 * @return a new {@code RunnableConfig} instance with the updated metadata
	 */
	public void removeInterrupted(String nodeId) {
		String formattedNodeId = HasMetadata.formatNodeId(nodeId);
		if (interruptedNodes == null || !interruptedNodes.containsKey(formattedNodeId)) {
			return; // No change needed if the marker doesn't exist
		}
		interruptedNodes.remove(formattedNodeId);
	}

	/**
	 * Marks a node as interrupted by adding it to the metadata with a formatted key. The
	 * node ID is formatted using {@link #formatNodeId(String)} and associated with a
	 * value of {@code true} in the metadata map.
	 * @param nodeId the ID of the node to mark as interrupted; must not be null
	 * @return this {@code Builder} instance for method chaining
	 * @throws NullPointerException if nodeId is null
	 */
	public void markNodeAsInterrupted(String nodeId) {
		interruptedNodes.put(HasMetadata.formatNodeId(nodeId), true);
	}

	/**
	 * Create a new RunnableConfig with the same attributes as this one but with a
	 * different {@link CompiledGraph.StreamMode}.
	 * @param streamMode the new stream mode
	 * @return a new RunnableConfig with the updated stream mode
	 */
	public RunnableConfig withStreamMode(CompiledGraph.StreamMode streamMode) {
		if (this.streamMode == streamMode) {
			return this;
		}

		return RunnableConfig.builder(this).streamMode(streamMode).build();
	}

	/**
	 * Updates the checkpoint ID of the configuration.
	 * @param checkPointId The new checkpoint ID to set.
	 * @return A new instance of {@code RunnableConfig} with the updated checkpoint ID, or
	 * the current instance if no change is needed.
	 */
	public RunnableConfig withCheckPointId(String checkPointId) {
		if (Objects.equals(this.checkPointId, checkPointId)) {
			return this;
		}
		return RunnableConfig.builder(this).checkPointId(checkPointId).build();

	}

	/**
	 * Retrieves interrupt data associated with the specified key.
	 * @param key the key for which to retrieve interrupt data; may be null
	 * @return an Optional containing the interrupt data if present, or an empty Optional
	 * if the key is null or no data is found
	 */
	public Optional<Object> interruptData(String key) {
		if (key == null) {
			return Optional.empty();
		}
		return ofNullable(interruptedNodes).map(m -> m.get(key));
	}

	/**
	 * return metadata value for key
	 * @param key given metadata key
	 * @return metadata value for key if any
	 */
	@Override
	public Optional<Object> metadata(String key) {
		if (key == null) {
			return Optional.empty();
		}
		return ofNullable(metadata).map(m -> m.get(key));
	}

	/**
	 * Creates a new instance of the {@link Builder} class.
	 * @return A new {@code Builder} object.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Creates a new {@code Builder} instance with the specified {@link RunnableConfig}.
	 * @param config The configuration for the {@code Builder}.
	 * @return A new {@code Builder} instance.
	 */
	public static Builder builder(RunnableConfig config) {
		return new Builder(config);
	}

	/**
	 * A builder pattern class for constructing {@link RunnableConfig} objects. This class
	 * provides a fluent interface to set various properties of a {@link RunnableConfig}
	 * object and then build the final configuration.
	 */
	public static class Builder extends HasMetadata.Builder<Builder> {

		private String threadId;

		private String checkPointId;

		private String nextNode;

		private CompiledGraph.StreamMode streamMode = CompiledGraph.StreamMode.VALUES;

		/**
		 * Constructs a new instance of the {@link Builder} with default configuration
		 * settings. Initializes a new {@link RunnableConfig} object for configuration
		 * purposes.
		 */
		Builder() {
		}

		/**
		 * Initializes a new instance of the {@code Builder} class with the specified
		 * {@link RunnableConfig}.
		 * @param config The configuration to be used for initialization.
		 */
		Builder(RunnableConfig config) {
			super(requireNonNull(config, "config cannot be null!").metadata);
			this.threadId = config.threadId;
			this.checkPointId = config.checkPointId;
			this.nextNode = config.nextNode;
			this.streamMode = config.streamMode;
		}

		/**
		 * Sets the ID of the thread.
		 * @param threadId the ID of the thread to set
		 * @return a reference to this {@code Builder} object so that method calls can be
		 * chained together
		 */
		public Builder threadId(String threadId) {
			this.threadId = threadId;
			return this;
		}

		/**
		 * Sets the checkpoint ID for the configuration.
		 * @param {@code checkPointId} - the ID of the checkpoint to be set
		 * @return {@literal this} - a reference to the current `Builder` instance
		 */
		public Builder checkPointId(String checkPointId) {
			this.checkPointId = checkPointId;
			return this;
		}

		/**
		 * Sets the next node in the configuration and returns this builder for method
		 * chaining.
		 * @param nextNode The next node to be set.
		 * @return This builder instance, allowing for method chaining.
		 */
		public Builder nextNode(String nextNode) {
			this.nextNode = nextNode;
			return this;
		}

		/**
		 * Sets the stream mode of the configuration.
		 * @param streamMode The {@link CompiledGraph.StreamMode} to set.
		 * @return A reference to this builder for method chaining.
		 */
		public Builder streamMode(CompiledGraph.StreamMode streamMode) {
			this.streamMode = streamMode;
			return this;
		}

		/**
		 * Adds a custom {@link Executor} for a specific parallel node.
		 * <p>
		 * This allows you to control the execution of branches within a parallel node.
		 * When a parallel node is executed, it will look for an executor in the
		 * {@link RunnableConfig} metadata. If found, it will be used to run the parallel
		 * branches concurrently.
		 * @param nodeId the ID of the parallel node.
		 * @param executor the {@link Executor} to use for the parallel node.
		 * @return this {@code Builder} instance for method chaining.
		 */
		public Builder addParallelNodeExecutor(String nodeId, Executor executor) {
			return addMetadata(ParallelNode.formatNodeId(nodeId), requireNonNull(executor, "executor cannot be null!"));
		}

		/**
		 * Constructs and returns the configured {@code RunnableConfig} object.
		 * @return the configured {@code RunnableConfig} object
		 */
		public RunnableConfig build() {
			return new RunnableConfig(this);
		}

	}

	/**
	 * Creates a new instance of {@code RunnableConfig} as a copy of the provided
	 * {@code config}.
	 * @param builder The configuration builder.
	 */
	private RunnableConfig(Builder builder) {
		this.threadId = builder.threadId;
		this.checkPointId = builder.checkPointId;
		this.nextNode = builder.nextNode;
		this.streamMode = builder.streamMode;
		this.metadata = ofNullable(builder.metadata()).map(Map::copyOf).orElse(null);
		this.interruptedNodes = new ConcurrentHashMap<>();
	}

	@Override
	public String toString() {
		return format("RunnableConfig{ threadId=%s, checkPointId=%s, nextNode=%s, streamMode=%s }", threadId,
				checkPointId, nextNode, streamMode);
	}

}
