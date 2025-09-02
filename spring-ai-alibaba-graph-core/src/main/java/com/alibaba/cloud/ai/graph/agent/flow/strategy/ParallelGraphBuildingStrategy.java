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
package com.alibaba.cloud.ai.graph.agent.flow.strategy;

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.builder.FlowGraphBuilder;
import com.alibaba.cloud.ai.graph.agent.flow.enums.FlowAgentEnum;
import com.alibaba.cloud.ai.graph.agent.flow.node.EnhancedParallelResultAggregator;
import com.alibaba.cloud.ai.graph.agent.flow.node.ParallelResultAggregator;
import com.alibaba.cloud.ai.graph.agent.flow.node.TransparentNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Strategy for building parallel execution graphs. In a parallel graph, all sub-agents
 * execute concurrently and their results are aggregated using a specified merge strategy.
 */
public class ParallelGraphBuildingStrategy implements FlowGraphBuildingStrategy {

	@Override
	public StateGraph buildGraph(FlowGraphBuilder.FlowGraphConfig config) throws GraphStateException {
		validateConfig(config);
		validateParallelConfig(config);

		StateGraph graph = new StateGraph(config.getName(), config.getKeyStrategyFactory());
		BaseAgent rootAgent = config.getRootAgent();

		// Add root transparent node
		graph.addNode(rootAgent.name(),
				node_async(new TransparentNode(rootAgent.outputKey(), ((FlowAgent) rootAgent).inputKey())));

		// Add starting edge
		graph.addEdge(START, rootAgent.name());

		// Determine which aggregator to use based on available configuration
		String aggregatorNodeName = rootAgent.name() + "_aggregator";

		// Check if we have parallel-specific configuration (merge strategy,
		// concurrency)
		Object mergeStrategy = config.getCustomProperty("mergeStrategy");
		Integer maxConcurrency = (Integer) config.getCustomProperty("maxConcurrency");

		if (mergeStrategy != null || maxConcurrency != null) {
			// Use enhanced aggregator with custom merge strategy and concurrency control
			graph.addNode(aggregatorNodeName, node_async(new EnhancedParallelResultAggregator(rootAgent.outputKey(),
					config.getSubAgents(), mergeStrategy, maxConcurrency)));
		}
		else {
			// Use basic aggregator
			graph.addNode(aggregatorNodeName, node_async(new ParallelResultAggregator(rootAgent.outputKey())));
		}

		// Process sub-agents for parallel execution
		for (BaseAgent subAgent : config.getSubAgents()) {
			// Add the current sub-agent as a node
			graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(rootAgent.outputKey(), subAgent.outputKey()));
			// Connect root to each sub-agent (fan-out)
			graph.addEdge(rootAgent.name(), subAgent.name());
			// Connect each sub-agent to aggregator (gather)
			graph.addEdge(subAgent.name(), aggregatorNodeName);
		}

		// Connect aggregator to END
		graph.addEdge(aggregatorNodeName, END);

		return graph;
	}

	@Override
	public String getStrategyType() {
		return FlowAgentEnum.PARALLEL.getType();
	}

	@Override
	public void validateConfig(FlowGraphBuilder.FlowGraphConfig config) {
		FlowGraphBuildingStrategy.super.validateConfig(config);
		validateParallelConfig(config);
	}

	/**
	 * Validates parallel-specific configuration requirements.
	 * @param config the configuration to validate
	 * @throws IllegalArgumentException if validation fails
	 */
	private void validateParallelConfig(FlowGraphBuilder.FlowGraphConfig config) {
		if (config.getSubAgents() == null || config.getSubAgents().isEmpty()) {
			throw new IllegalArgumentException("Parallel flow requires at least one sub-agent");
		}

		if (config.getSubAgents().size() < 2) {
			throw new IllegalArgumentException(
					"Parallel flow requires at least 2 sub-agents for meaningful parallel execution");
		}

		// Ensure root agent is a FlowAgent for input key access
		if (!(config.getRootAgent() instanceof FlowAgent)) {
			throw new IllegalArgumentException("Parallel flow requires root agent to be a FlowAgent");
		}

		// Validate maxConcurrency if provided
		Integer maxConcurrency = (Integer) config.getCustomProperty("maxConcurrency");
		if (maxConcurrency != null && maxConcurrency < 1) {
			throw new IllegalArgumentException("maxConcurrency must be at least 1, but got: " + maxConcurrency);
		}
	}

}
