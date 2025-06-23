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

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import java.util.Collection;
import java.util.Deque;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant.MEMORY;
import static java.util.Optional.ofNullable;

/**
 * class is a configuration container for defining compile settings and behaviors. It
 * includes various fields and methods to manage checkpoint savers and interrupts,
 * providing both deprecated and current accessors.
 */
public class CompileConfig {

	private SaverConfig saverConfig = new SaverConfig().register(MEMORY, new MemorySaver());

	private Deque<GraphLifecycleListener> lifecycleListeners = new LinkedBlockingDeque<>(25);

	// private BaseCheckpointSaver checkpointSaver; // replaced with SaverConfig
	private Set<String> interruptsBefore = Set.of();

	private Set<String> interruptsAfter = Set.of();

	private boolean releaseThread = false;

	/**
	 * Returns the current state of the thread release flag.
	 *
	 * @see BaseCheckpointSaver#release(RunnableConfig)
	 * @return true if the thread has been released, false otherwise
	 */
	public boolean releaseThread() {
		return releaseThread;
	}

	/**
	 * Gets an unmodifiable list of node lifecycle listeners.
	 * @return The list of lifecycle listeners.
	 */
	public Queue<GraphLifecycleListener> lifecycleListeners() {
		return lifecycleListeners;
	}

	/**
	 * Returns the array of interrupts that will occur before the specified node
	 * (deprecated).
	 * @return An array of interruptible nodes.
	 * @deprecated Use {@link #interruptsBefore()} instead for better immutability and
	 * type safety.
	 */
	@Deprecated
	public String[] getInterruptBefore() {
		return interruptsBefore.toArray(new String[0]);
	}

	/**
	 * Returns the array of interrupts that will occur after the specified node
	 * (deprecated).
	 * @return An array of interruptible nodes.
	 * @deprecated Use {@link #interruptsAfter()} instead for better immutability and type
	 * safety.
	 */
	@Deprecated
	public String[] getInterruptAfter() {
		return interruptsAfter.toArray(new String[0]);
	}

	/**
	 * Returns the set of interrupts that will occur before the specified node.
	 * @return An unmodifiable set of interruptible nodes.
	 */
	public Set<String> interruptsBefore() {
		return interruptsBefore;
	}

	/**
	 * Returns the set of interrupts that will occur after the specified node.
	 * @return An unmodifiable set of interruptible nodes.
	 */
	public Set<String> interruptsAfter() {
		return interruptsAfter;
	}

	/**
	 * Retrieves a checkpoint saver based on the specified type from the saver
	 * configuration.
	 * @param type The type of the checkpoint saver to retrieve.
	 * @return An Optional containing the checkpoint saver if available; otherwise, empty.
	 */
	public Optional<BaseCheckpointSaver> checkpointSaver(String type) {
		return ofNullable(saverConfig.get(type));
	}

	/**
	 * Retrieves the default checkpoint saver from the saver configuration.
	 * @return An Optional containing the default checkpoint saver if available;
	 * otherwise, empty.
	 */
	public Optional<BaseCheckpointSaver> checkpointSaver() {
		return ofNullable(saverConfig.get());
	}

	/**
	 * Returns a new instance of the builder with default configuration settings.
	 * @return A new Builder instance.
	 */
	public static Builder builder() {
		return new Builder(new CompileConfig());
	}

	/**
	 * Returns a new instance of the builder initialized with the provided configuration.
	 * @param config The compile configuration to use as a base.
	 * @return A new Builder instance initialized with the given configuration.
	 */
	public static Builder builder(CompileConfig config) {
		return new Builder(config);
	}

	/**
	 * Builder class for creating instances of CompileConfig. It allows setting various
	 * options such as savers, interrupts, and lifecycle listeners in a fluent manner.
	 */
	public static class Builder {

		private final CompileConfig config;

		/**
		 * Initializes the builder with the provided compile configuration.
		 * @param config The base configuration to start from.
		 */
		protected Builder(CompileConfig config) {
			this.config = new CompileConfig(config);
		}

		/**
		 * Sets whether the thread should be released during execution.
		 * @param releaseThread Flag indicating whether to release the thread.
		 * @see BaseCheckpointSaver#release(RunnableConfig)
		 * @return This builder instance for method chaining.
		 */
		public Builder releaseThread(boolean releaseThread) {
			this.config.releaseThread = releaseThread;
			return this;
		}

		/**
		 * Sets the saver configuration for checkpoints.
		 * @param saverConfig The SaverConfig to use.
		 * @return This builder instance for method chaining.
		 */
		public Builder saverConfig(SaverConfig saverConfig) {
			this.config.saverConfig = saverConfig;
			return this;
		}

		/**
		 * Sets individual interrupt points that trigger before node execution using
		 * varargs.
		 * @param interruptBefore One or more strings representing interrupt points.
		 * @return This builder instance for method chaining.
		 */
		public Builder interruptBefore(String... interruptBefore) {
			this.config.interruptsBefore = Set.of(interruptBefore);
			return this;
		}

		/**
		 * Sets individual interrupt points that trigger after node execution using
		 * varargs.
		 * @param interruptAfter One or more strings representing interrupt points.
		 * @return This builder instance for method chaining.
		 */
		public Builder interruptAfter(String... interruptAfter) {
			this.config.interruptsAfter = Set.of(interruptAfter);
			return this;
		}

		/**
		 * Sets multiple interrupt points that trigger before node execution from a
		 * collection.
		 * @param interruptsBefore Collection of strings representing interrupt points.
		 * @return This builder instance for method chaining.
		 */
		public Builder interruptsBefore(Collection<String> interruptsBefore) {
			this.config.interruptsBefore = interruptsBefore.stream().collect(Collectors.toUnmodifiableSet());
			return this;
		}

		/**
		 * Sets multiple interrupt points that trigger after node execution from a
		 * collection.
		 * @param interruptsAfter Collection of strings representing interrupt points.
		 * @return This builder instance for method chaining.
		 */
		public Builder interruptsAfter(Collection<String> interruptsAfter) {
			this.config.interruptsAfter = interruptsAfter.stream().collect(Collectors.toUnmodifiableSet());
			return this;
		}

		/**
		 * Adds a lifecycle listener to monitor node execution events.
		 * @param listener The NodeLifecycleListener to add.
		 * @return This builder instance for method chaining.
		 */
		public Builder withLifecycleListener(GraphLifecycleListener listener) {
			this.config.lifecycleListeners.offer(listener);
			return this;
		}

		/**
		 * Finalizes the configuration and returns the compiled instance.
		 * @return The configured CompileConfig object.
		 */
		public CompileConfig build() {
			return config;
		}

	}

	/**
	 * Default constructor used internally to create a new configuration with default
	 * settings. Made private to ensure all instances are created through the builder
	 * pattern.
	 */
	private CompileConfig() {
	}

	/**
	 * Copy constructor to create a new instance based on an existing configuration.
	 * @param config The configuration to copy.
	 */
	private CompileConfig(CompileConfig config) {
		this.saverConfig = config.saverConfig;
		this.interruptsBefore = config.interruptsBefore;
		this.interruptsAfter = config.interruptsAfter;
		this.releaseThread = config.releaseThread;
		this.lifecycleListeners = config.lifecycleListeners;
	}

}
