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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

public class SequentialAgent extends FlowAgent {

	protected SequentialAgent(SequentialAgentBuilder builder) throws GraphStateException {
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
	 * Recursively adds sub-agents and their nested sub-agents to the graph
	 * @param graph the StateGraph to add nodes and edges to
	 * @param parentAgent the name of the parent node
	 * @param subAgents the list of sub-agents to process
	 */
	@Override
	protected void processSubAgents(StateGraph graph, BaseAgent parentAgent, List<BaseAgent> subAgents)
			throws GraphStateException {
		for (BaseAgent subAgent : subAgents) {
			// Add the current sub-agent as a node
			graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(parentAgent.outputKey(), subAgent.outputKey()));
			graph.addEdge(parentAgent.name(), subAgent.name());
			parentAgent = subAgent;
		}
		// connect the last agent to END
		graph.addEdge(parentAgent.name(), END);
	}

	public static SequentialAgentBuilder builder() {
		return new SequentialAgentBuilder();
	}

	/**
	 * Builder for creating SequentialAgent instances. Extends the common FlowAgentBuilder
	 * to provide type-safe building.
	 */
	public static class SequentialAgentBuilder extends FlowAgentBuilder<SequentialAgent, SequentialAgentBuilder> {

		@Override
		protected SequentialAgentBuilder self() {
			return this;
		}

		@Override
		protected void validate() {
			super.validate();
			// Add any SequentialAgent-specific validation here if needed
		}

		@Override
		public SequentialAgent build() throws GraphStateException {
			validate();
			return new SequentialAgent(this);
		}

	}

}
