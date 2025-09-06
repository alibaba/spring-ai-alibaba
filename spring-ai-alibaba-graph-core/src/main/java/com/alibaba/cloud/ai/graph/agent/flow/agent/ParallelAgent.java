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
package com.alibaba.cloud.ai.graph.agent.flow.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowAgentBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ParallelAgent executes multiple sub-agents in parallel and merges their results.
 *
 * <p>
 * This agent implements the Parallel Fan-Out/Gather pattern where:
 * </p>
 * <ul>
 * <li><strong>Fan-Out:</strong> The input is distributed to all sub-agents
 * simultaneously</li>
 * <li><strong>Parallel Execution:</strong> All sub-agents execute concurrently</li>
 * <li><strong>Gather:</strong> Results from all sub-agents are collected and merged</li>
 * </ul>
 *
 * <p>
 * The agent leverages the underlying ParallelNode infrastructure to achieve true parallel
 * execution and provides configurable result merging strategies.
 * </p>
 */
public class ParallelAgent extends FlowAgent {

	private static final Logger logger = LoggerFactory.getLogger(ParallelAgent.class);

	private final MergeStrategy mergeStrategy;

	private final Integer maxConcurrency;

	protected ParallelAgent(ParallelAgentBuilder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey, builder.inputKey, builder.keyStrategyFactory,
				builder.compileConfig, builder.subAgents);
		this.mergeStrategy = builder.mergeStrategy != null ? builder.mergeStrategy : new DefaultMergeStrategy();
		this.maxConcurrency = builder.maxConcurrency;
		this.graph = initGraph();
	}

	@Override
	protected StateGraph buildSpecificGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		// Add parallel-specific properties to config
		config.customProperty("mergeStrategy", this.mergeStrategy);
		config.customProperty("maxConcurrency", this.maxConcurrency);

		return FlowGraphBuilder.buildGraph(FlowAgentEnum.PARALLEL.getType(), config);
	}

	/**
	 * Gets the merge strategy used by this ParallelAgent.
	 * @return the merge strategy
	 */
	public MergeStrategy mergeStrategy() {
		return mergeStrategy;
	}

	/**
	 * Gets the maximum concurrency limit for this ParallelAgent.
	 * @return the max concurrency, or null if unlimited
	 */
	public Integer maxConcurrency() {
		return maxConcurrency;
	}

	public static ParallelAgentBuilder builder() {
		return new ParallelAgentBuilder();
	}

	/**
	 * Builder for creating ParallelAgent instances. Extends the common FlowAgentBuilder
	 * to provide type-safe building with parallel execution capabilities.
	 *
	 * <p>
	 * Usage example:
	 * </p>
	 *
	 * <pre>{@code
	 * ParallelAgent parallelAgent = ParallelAgent.builder()
	 * 		.name("parallel_workflow")
	 * 		.description("Executes multiple tasks in parallel")
	 * 		.inputKey("input")
	 * 		.outputKey("output")
	 * 		.mergeStrategy(new ParallelAgent.ListMergeStrategy())
	 * 		.maxConcurrency(5)
	 * 		.subAgents(List.of(agent1, agent2, agent3))
	 * 		.build();
	 * }</pre>
	 */
	public static class ParallelAgentBuilder extends FlowAgentBuilder<ParallelAgent, ParallelAgentBuilder> {

		private MergeStrategy mergeStrategy;

		private Integer maxConcurrency;

		/**
		 * Sets the merge strategy for combining parallel execution results.
		 * @param mergeStrategy the strategy to use for merging results
		 * @return this builder instance for method chaining
		 */
		public ParallelAgentBuilder mergeStrategy(MergeStrategy mergeStrategy) {
			this.mergeStrategy = mergeStrategy;
			return this;
		}

		/**
		 * Sets the maximum number of sub-agents that can execute concurrently.
		 * @param maxConcurrency the maximum concurrency limit
		 * @return this builder instance for method chaining
		 */
		public ParallelAgentBuilder maxConcurrency(Integer maxConcurrency) {
			this.maxConcurrency = maxConcurrency;
			return this;
		}

		/**
		 * Returns the concrete builder instance for fluent interface support.
		 * @return this builder instance
		 */
		@Override
		protected ParallelAgentBuilder self() {
			return this;
		}

		/**
		 * Validates the builder state before creating the agent.
		 * @throws IllegalArgumentException if validation fails
		 */
		@Override
		protected void validate() {
			// Validate name first (from parent)
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("Name must be provided");
			}

			// Validate minimum sub-agent count for ParallelAgent (skip parent subAgents
			// check)
			if (subAgents == null || subAgents.size() < 2) {
				throw new IllegalArgumentException(
						"ParallelAgent requires at least 2 sub-agents for parallel execution, but got: "
								+ (subAgents != null ? subAgents.size() : 0));
			}

			// Validate maximum sub-agent count for performance reasons
			if (subAgents.size() > 10) {
				throw new IllegalArgumentException(
						"ParallelAgent supports maximum 10 sub-agents for performance reasons, but got: "
								+ subAgents.size());
			}

			// Validate that sub-agents have unique output keys to avoid conflicts during
			// result merging
			validateUniqueOutputKeys();

			// Validate input key compatibility
			validateInputKeyCompatibility();

			// Validate concurrency limit - allow maxConcurrency to be null (unlimited) or
			// within valid range
			if (maxConcurrency != null) {
				if (maxConcurrency < 1) {
					throw new IllegalArgumentException("maxConcurrency must be at least 1, but got: " + maxConcurrency);
				}
			}
		}

		/**
		 * Validates that all sub-agents have unique output keys.
		 */
		private void validateUniqueOutputKeys() {
			Set<String> outputKeys = new HashSet<>();
			Set<String> duplicateKeys = new HashSet<>();

			for (BaseAgent subAgent : subAgents) {
				String outputKey = subAgent.outputKey();
				if (outputKey != null) {
					if (!outputKeys.add(outputKey)) {
						// This key was already seen, it's a duplicate
						duplicateKeys.add(outputKey);
					}
				}
			}

			if (!duplicateKeys.isEmpty()) {
				throw new IllegalArgumentException(
						"ParallelAgent validation failed: Duplicate output keys found among sub-agents: "
								+ duplicateKeys
								+ ". Each sub-agent must have a unique output key to avoid conflicts during result merging.");
			}
		}

		/**
		 * Validates that sub-agents can properly receive data from the parent agent.
		 *
		 * <p>
		 * This validation checks the data flow compatibility by ensuring that:
		 * </p>
		 * <ul>
		 * <li>The parent agent has an outputKey defined</li>
		 * <li>Sub-agents can properly handle the data flow through asAsyncNodeAction</li>
		 * </ul>
		 *
		 * <p>
		 * Note: BaseAgent doesn't have inputKey property, data flow is handled through
		 * asAsyncNodeAction parameters during graph construction.
		 * </p>
		 */
		private void validateInputKeyCompatibility() {
			String parentOutputKey = this.outputKey;
			if (parentOutputKey == null) {
				logger.warn("Parent agent '{}' has no outputKey defined. This may cause data flow issues "
						+ "as sub-agents won't receive input data.", this.name);
			}

			// Check if sub-agents have outputKeys defined (they will be used as input
			// keys for downstream agents)
			for (BaseAgent subAgent : subAgents) {
				String subAgentOutputKey = subAgent.outputKey();
				if (subAgentOutputKey == null) {
					logger.warn("Sub-agent '{}' has no outputKey defined. This may cause data flow issues "
							+ "as downstream agents won't receive data from this agent.", subAgent.name());
				}
			}
		}

		/**
		 * Builds the ParallelAgent instance.
		 * @return the built ParallelAgent instance
		 * @throws GraphStateException if agent creation fails
		 */
		@Override
		public ParallelAgent build() throws GraphStateException {
			validate();
			return new ParallelAgent(this);
		}

	}

	/**
	 * Strategy interface for merging parallel execution results.
	 */
	public interface MergeStrategy {

		/**
		 * Merges results from parallel sub-agents.
		 * @param subAgentResults map of sub-agent output keys to their results
		 * @param overallState the complete state including all context
		 * @return the merged result
		 */
		Object merge(Map<String, Object> subAgentResults, OverAllState overallState);

	}

	/**
	 * Default merge strategy that combines all results into a map.
	 */
	public static class DefaultMergeStrategy implements MergeStrategy {

		@Override
		public Object merge(Map<String, Object> subAgentResults, OverAllState overallState) {
			return new HashMap<>(subAgentResults);
		}

	}

	/**
	 * List merge strategy that combines results into a list.
	 */
	public static class ListMergeStrategy implements MergeStrategy {

		@Override
		public Object merge(Map<String, Object> subAgentResults, OverAllState overallState) {
			return subAgentResults.values().stream().toList();
		}

	}

	/**
	 * Concatenation merge strategy for string results.
	 */
	public static class ConcatenationMergeStrategy implements MergeStrategy {

		private final String separator;

		public ConcatenationMergeStrategy() {
			this("\n");
		}

		public ConcatenationMergeStrategy(String separator) {
			this.separator = separator;
		}

		@Override
		public Object merge(Map<String, Object> subAgentResults, OverAllState overallState) {
			return subAgentResults.values()
				.stream()
				.map(Object::toString)
				.reduce("", (a, b) -> a.isEmpty() ? b : a + separator + b);
		}

	}

}
