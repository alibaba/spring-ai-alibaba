/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;

import static java.util.Optional.ofNullable;

/**
 * class is a configuration container for defining compile settings and behaviors. It
 * includes various fields and methods to manage checkpoint savers and interrupts,
 * providing both deprecated and current accessors.
 */
public class CompileConfig {

	private SaverConfig saverConfig;

	private PlainTextStateSerializer plainTextStateSerializer;

	// private BaseCheckpointSaver checkpointSaver; // replaced with SaverConfig
	private Set<String> interruptsBefore = Set.of();

	private Set<String> interruptsAfter = Set.of();

	/**
	 * Returns the array of interrupts that will occur before the specified node.
	 * @return an array of interruptible nodes.
	 */
	@Deprecated
	public String[] getInterruptBefore() {
		return interruptsBefore.toArray(new String[0]);
	}

	/**
	 * Returns the array of interrupts that will occur after the specified node.
	 * @return an array of interruptible nodes.
	 */
	@Deprecated
	public String[] getInterruptAfter() {
		return interruptsAfter.toArray(new String[0]);
	}

	/**
	 * Returns the array of interrupts that will occur before the specified node.
	 * @return an unmodifiable {@link Set} of interruptible nodes.
	 */
	public Set<String> interruptsBefore() {
		return interruptsBefore;
	}

	/**
	 * Returns the array of interrupts that will occur after the specified node.
	 * @return an unmodifiable {@link Set} of interruptible nodes.
	 */
	public Set<String> interruptsAfter() {
		return interruptsAfter;
	}

	/**
	 * Returns the current {@code BaseCheckpointSaver} instance if it is not {@code null},
	 * otherwise returns an empty {@link Optional}.
	 * @return an {@link Optional} containing the current {@code BaseCheckpointSaver}
	 * instance, or an empty {@link Optional} if it is {@code null}
	 */
	public Optional<BaseCheckpointSaver> checkpointSaver(String type) {
		return ofNullable(saverConfig.get(type));
	}

	public Optional<BaseCheckpointSaver> checkpointSaver() {
		return ofNullable(saverConfig.get());
	}

	/**
	 * Returns a new {@link Builder} instance with the default {@link CompileConfig}.
	 * @return A {@link Builder} instance.
	 */
	public static Builder builder() {
		return new Builder(new CompileConfig());
	}

	/**
	 * Creates a new {@link Builder} instance with the specified Compile configuration.
	 * @param config The {@link CompileConfig} to be used for compilation settings.
	 * @return A new {@link Builder} instance initialized with the given compilation
	 * configuration.
	 */
	public static Builder builder(CompileConfig config) {
		return new Builder(config);
	}

	/**
	 * This class is a builder for {@link CompileConfig}. It allows for the configuration
	 * of various options to customize the compilation process.
	 *
	 */
	public static class Builder {

		private final CompileConfig config;

		/**
		 * Constructs a new instance of {@code Builder} with the specified compile
		 * configuration.
		 * @param config The compile configuration to be used. This value must not be
		 * {@literal null}.
		 */
		protected Builder(CompileConfig config) {
			this.config = new CompileConfig(config);
		}

		/**
		 * Sets the checkpoint saver for the configuration.
		 * @param saverConfig The {@code BaseCheckpointSaver} to set.
		 * @return The current {@code Builder} instance for method chaining.
		 */
		public Builder saverConfig(SaverConfig saverConfig) {
			this.config.saverConfig = saverConfig;
			return this;
		}

		/**
		 * Plain text state serializer builder.
		 * @param plainTextStateSerializer the plain text state serializer
		 * @return The current {@code Builder} instance for method chaining.
		 */
		public Builder plainTextStateSerializer(PlainTextStateSerializer plainTextStateSerializer) {
			this.config.plainTextStateSerializer = plainTextStateSerializer;
			return this;
		}

		/**
		 * Sets the actions to be performed before an interruption.
		 * @param interruptBefore the actions to be performed before an interruption
		 * @return a reference to the current instance of Builder
		 */
		public Builder interruptBefore(String... interruptBefore) {
			this.config.interruptsBefore = Set.of(interruptBefore);
			return this;
		}

		/**
		 * Sets the strings that cause an interrupt in the configuration.
		 * @param interruptAfter An array of string values representing the interruptions.
		 * @return The current Builder instance, allowing method chaining.
		 */
		public Builder interruptAfter(String... interruptAfter) {
			this.config.interruptsAfter = Set.of(interruptAfter);
			return this;
		}

		/**
		 * Sets the collection of interrupts to be executed before the configuration.
		 * @param interruptsBefore The collection of interrupt strings.
		 * @return This builder instance for method chaining.
		 */
		public Builder interruptsBefore(Collection<String> interruptsBefore) {
			this.config.interruptsBefore = interruptsBefore.stream().collect(Collectors.toUnmodifiableSet());
			return this;
		}

		/**
		 * Sets the collection of strings that specify which interrupts should occur
		 * after.
		 * @param interruptsAfter Collection of interrupt identifiers
		 * @return The current Builder instance for method chaining
		 */
		public Builder interruptsAfter(Collection<String> interruptsAfter) {
			this.config.interruptsAfter = interruptsAfter.stream().collect(Collectors.toUnmodifiableSet());
			;
			return this;
		}

		/**
		 * Initializes the compilation configuration and returns it.
		 * @return the compiled {@link CompileConfig} object
		 */
		public CompileConfig build() {
			return config;
		}

	}

	/**
	 * Default constructor for the {@class CompileConfig} class. This constructor is
	 * private to enforce that instances of this class are not created outside its
	 * package.
	 */
	private CompileConfig() {
	}

	/**
	 * Creates a new {@code CompileConfig} object as a copy of the provided configuration.
	 * @param config The configuration to copy.
	 */
	private CompileConfig(CompileConfig config) {
		this.saverConfig = config.saverConfig;
		this.interruptsBefore = config.interruptsBefore;
		this.interruptsAfter = config.interruptsAfter;
	}

}