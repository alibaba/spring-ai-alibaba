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
package com.alibaba.cloud.ai.graph.agent.flow.builder;

import java.util.List;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

/**
 * Abstract base builder for FlowAgent and its subclasses. Provides common builder
 * functionality and enforces consistent builder patterns.
 *
 * @param <T> the concrete FlowAgent type this builder creates
 * @param <B> the concrete builder type (for fluent interface support)
 */
public abstract class FlowAgentBuilder<T extends FlowAgent, B extends FlowAgentBuilder<T, B>> {

	// Common FlowAgent properties
	public String name;

	public String description;

	public String outputKey;

	public String inputKey;

	public KeyStrategyFactory keyStrategyFactory;

	public CompileConfig compileConfig;

	public List<BaseAgent> subAgents;

	/**
	 * Sets the agent name.
	 * @param name the unique name of the agent
	 * @return this builder instance for method chaining
	 */
	public B name(String name) {
		this.name = name;
		return self();
	}

	/**
	 * Sets the agent description.
	 * @param description the description of the agent's capability
	 * @return this builder instance for method chaining
	 */
	public B description(String description) {
		this.description = description;
		return self();
	}

	/**
	 * Sets the output key for the agent's result.
	 * @param outputKey the output key
	 * @return this builder instance for method chaining
	 */
	public B outputKey(String outputKey) {
		this.outputKey = outputKey;
		return self();
	}

	/**
	 * Sets the input key for the agent.
	 * @param inputKey the input key
	 * @return this builder instance for method chaining
	 */
	public B inputKey(String inputKey) {
		this.inputKey = inputKey;
		return self();
	}

	/**
	 * Sets the key strategy factory for state management.
	 * @param keyStrategyFactory the key strategy factory
	 * @return this builder instance for method chaining
	 */
	public B state(KeyStrategyFactory keyStrategyFactory) {
		this.keyStrategyFactory = keyStrategyFactory;
		return self();
	}

	/**
	 * Sets the compile configuration.
	 * @param compileConfig the compile configuration
	 * @return this builder instance for method chaining
	 */
	public B compileConfig(CompileConfig compileConfig) {
		this.compileConfig = compileConfig;
		return self();
	}

	/**
	 * Sets the list of sub-agents.
	 * @param subAgents the list of sub-agents
	 * @return this builder instance for method chaining
	 */
	public B subAgents(List<BaseAgent> subAgents) {
		this.subAgents = subAgents;
		return self();
	}

	/**
	 * Returns the concrete builder instance. This method enables fluent interface support
	 * in subclasses.
	 * @return this builder instance
	 */
	protected abstract B self();

	/**
	 * Validates the builder state before creating the agent. Subclasses can override this
	 * method to add specific validation logic.
	 * @throws IllegalArgumentException if validation fails
	 */
	protected void validate() {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Name must be provided");
		}
		if (subAgents == null || subAgents.isEmpty()) {
			throw new IllegalArgumentException("At least one sub-agent must be provided for flow");
		}
	}

	/**
	 * Builds the concrete FlowAgent instance. Subclasses must implement this method to
	 * create the specific agent type.
	 * @return the built FlowAgent instance
	 * @throws GraphStateException if agent creation fails
	 */
	public abstract T build() throws GraphStateException;

}
