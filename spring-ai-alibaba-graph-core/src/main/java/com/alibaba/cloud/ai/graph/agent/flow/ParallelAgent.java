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
package com.alibaba.cloud.ai.graph.agent.flow;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * ParallelAgent executes multiple sub-agents in parallel and merges their results.
 * 
 * <p>This agent implements the Parallel Fan-Out/Gather pattern where:</p>
 * <ul>
 *   <li><strong>Fan-Out:</strong> The input is distributed to all sub-agents simultaneously</li>
 *   <li><strong>Parallel Execution:</strong> All sub-agents execute concurrently</li>
 *   <li><strong>Gather:</strong> Results from all sub-agents are collected and merged</li>
 * </ul>
 * 
 * <p>The agent leverages the underlying ParallelNode infrastructure to achieve true parallel execution
 * and automatic result merging based on configured KeyStrategies.</p>
 *
 */
public class ParallelAgent extends FlowAgent {

	protected ParallelAgent(ParallelAgentBuilder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey, builder.inputKey, builder.keyStrategyFactory,
				builder.compileConfig, builder.subAgents);
		this.graph = initGraph();
	}

	@Override
	public Optional<OverAllState> invoke(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.invoke(input);
	}

	/**
	 * Configures the graph to execute sub-agents in parallel.
	 * 
	 * <p>The key insight is that by adding edges from the parent agent to ALL sub-agents,
	 * the CompiledGraph automatically detects this as a parallel execution scenario and
	 * creates a ParallelNode to handle the concurrent execution.</p>
	 * 
	 * <p>Each sub-agent receives the same input from the parent agent, but should be designed
	 * to handle different aspects of the task based on their specific instructions and output keys.</p>
	 * 
	 * @param graph the StateGraph to add nodes and edges to
	 * @param parentAgent the parent agent that will distribute work to sub-agents
	 * @param subAgents the list of sub-agents to execute in parallel
	 */
	@Override
	protected void processSubAgents(StateGraph graph, BaseAgent parentAgent, List<BaseAgent> subAgents)
			throws GraphStateException {
		
		// Add all sub-agents as nodes
		for (BaseAgent subAgent : subAgents) {
			// Convert each sub-agent to an AsyncNodeAction
			// Each sub-agent receives the same input from the parent agent
			// but processes it according to their specific instructions and output key
			graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(
				parentAgent.outputKey(),  // Input from parent: same task description
				subAgent.outputKey()      // Output to own key: different result types
			));
		}
		
		// Add edges from parent to ALL sub-agents
		// This triggers the CompiledGraph to automatically create a ParallelNode
		// because there are multiple targets from a single source
		for (BaseAgent subAgent : subAgents) {
			graph.addEdge(parentAgent.name(), subAgent.name());
		}
		
		// Connect all sub-agents to END
		// Each sub-agent completes independently and contributes to the final result
		for (BaseAgent subAgent : subAgents) {
			graph.addEdge(subAgent.name(), END);
		}
	}

	public static ParallelAgentBuilder builder() {
		return new ParallelAgentBuilder();
	}

	/**
	 * Builder for creating ParallelAgent instances. Extends the common FlowAgentBuilder
	 * to provide type-safe building with parallel execution capabilities.
	 * 
	 * <p>Usage example:</p>
	 * <pre>{@code
	 * ParallelAgent parallelAgent = ParallelAgent.builder()
	 *     .name("parallel_workflow")
	 *     .description("Executes multiple tasks in parallel")
	 *     .inputKey("input")
	 *     .outputKey("output")
	 *     .subAgents(List.of(agent1, agent2, agent3))
	 *     .build();
	 * }</pre>
	 */
	public static class ParallelAgentBuilder extends FlowAgentBuilder<ParallelAgent, ParallelAgentBuilder> {

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
			super.validate();
			
			// Validate that sub-agents have unique output keys to avoid conflicts during result merging
			if (subAgents != null && subAgents.size() > 1) {
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
						"ParallelAgent validation failed: Duplicate output keys found among sub-agents: " + 
						duplicateKeys + ". Each sub-agent must have a unique output key to avoid conflicts during result merging."
					);
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
}
