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

import java.util.Objects;
import java.util.Optional;

public final class RunnableConfig {

	private String threadId;

	private String checkPointId;

	private String nextNode;

	private CompiledGraph.StreamMode streamMode = CompiledGraph.StreamMode.VALUES;

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
		return Optional.ofNullable(threadId);
	}

	/**
	 * Returns the current {@code checkPointId} wrapped in an {@link Optional}.
	 * @return an {@link Optional} containing the {@code checkPointId}, or
	 * {@link Optional#empty()} if it is null.
	 */
	public Optional<String> checkPointId() {
		return Optional.ofNullable(checkPointId);
	}

	/**
	 * Returns an {@code Optional} describing the next node in the sequence, or an empty
	 * {@code Optional} if there is no such node.
	 * @return an {@code Optional} describing the next node, or an empty {@code Optional}
	 */
	public Optional<String> nextNode() {
		return Optional.ofNullable(nextNode);
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
		RunnableConfig newConfig = new RunnableConfig(this);
		newConfig.streamMode = streamMode;
		return newConfig;
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
		RunnableConfig newConfig = new RunnableConfig(this);
		newConfig.checkPointId = checkPointId;
		return newConfig;
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
	public static class Builder {

		private final RunnableConfig config;

		/**
		 * Constructs a new instance of the {@link Builder} with default configuration
		 * settings. Initializes a new {@link RunnableConfig} object for configuration
		 * purposes.
		 */
		Builder() {
			;
			this.config = new RunnableConfig();
		}

		/**
		 * Initializes a new instance of the {@code Builder} class with the specified
		 * {@link RunnableConfig}.
		 * @param config The configuration to be used for initialization.
		 */
		Builder(RunnableConfig config) {
			this.config = new RunnableConfig(config);
		}

		/**
		 * Sets the ID of the thread.
		 * @param threadId the ID of the thread to set
		 * @return a reference to this {@code Builder} object so that method calls can be
		 * chained together
		 */
		public Builder threadId(String threadId) {
			this.config.threadId = threadId;
			return this;
		}

		/**
		 * Sets the checkpoint ID for the configuration.
		 * @param {@code checkPointId} - the ID of the checkpoint to be set
		 * @return {@literal this} - a reference to the current `Builder` instance
		 */
		public Builder checkPointId(String checkPointId) {
			this.config.checkPointId = checkPointId;
			return this;
		}

		/**
		 * Sets the next node in the configuration and returns this builder for method
		 * chaining.
		 * @param nextNode The next node to be set.
		 * @return This builder instance, allowing for method chaining.
		 */
		public Builder nextNode(String nextNode) {
			this.config.nextNode = nextNode;
			return this;
		}

		/**
		 * Sets the stream mode of the configuration.
		 * @param streamMode The {@link CompiledGraph.StreamMode} to set.
		 * @return A reference to this builder for method chaining.
		 */
		public Builder streamMode(CompiledGraph.StreamMode streamMode) {
			this.config.streamMode = streamMode;
			return this;
		}

		/**
		 * Constructs and returns the configured {@code RunnableConfig} object.
		 * @return the configured {@code RunnableConfig} object
		 */
		public RunnableConfig build() {
			return config;
		}

	}

	/**
	 * Creates a new instance of {@code RunnableConfig} as a copy of the provided
	 * {@code config}.
	 * @param config The configuration to copy.
	 * @throws NullPointerException If {@code config} is null.
	 */
	private RunnableConfig(RunnableConfig config) {
		Objects.requireNonNull(config, "config cannot be null");
		this.threadId = config.threadId;
		this.checkPointId = config.checkPointId;
		this.nextNode = config.nextNode;
		this.streamMode = config.streamMode;
	}

	/**
	 * Default constructor for the {@link RunnableConfig} class. Private to prevent
	 * instantiation from outside the class.
	 */
	private RunnableConfig() {
	}

	@Override
	public String toString() {
		return "RunnableConfig{" + "threadId='" + threadId + '\'' + ", checkPointId='" + checkPointId + '\''
				+ ", nextNode='" + nextNode + '\'' + ", streamMode=" + streamMode + '}';
	}

}
