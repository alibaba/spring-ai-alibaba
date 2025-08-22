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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ChatModel;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

public class LlmRoutingAgent extends FlowAgent {

	private final ChatModel chatModel;

	protected LlmRoutingAgent(LlmRoutingAgentBuilder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey, builder.inputKey, builder.keyStrategyFactory,
				builder.compileConfig, builder.subAgents);
		this.chatModel = builder.chatModel;
		this.graph = initGraph();
	}

	@Override
	public Optional<OverAllState> invoke(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.invoke(input);
	}

	// protected StateGraph initGraph() throws GraphStateException {
	// StateGraph graph = new StateGraph(this.name(), keyStrategyFactory);
	//
	// // add root agent
	// graph.addNode(this.name(), node_async(LlmNode.builder().(this.outputKey,
	// this.inputKey)));
	//
	// // add starting edge
	// graph.addEdge(START, this.name());
	// // Use recursive method to add all sub-agents
	// processSubAgents(graph, this, this.subAgents());
	//
	// return graph;
	// }

	/**
	 * Recursively adds sub-agents and their nested sub-agents to the graph
	 * @param graph the StateGraph to add nodes and edges to
	 * @param parentAgent the name of the parent node
	 * @param subAgents the list of sub-agents to process
	 */
	@Override
	protected void processSubAgents(StateGraph graph, BaseAgent parentAgent, List<BaseAgent> subAgents)
			throws GraphStateException {
		Map<String, String> edgeRoutingMap = new HashMap<>();
		for (BaseAgent subAgent : subAgents) {
			// Add the current sub-agent as a node
			graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(parentAgent.outputKey(), subAgent.outputKey()));
			// graph.addEdge(parentAgent.name(), subAgent.name());
			edgeRoutingMap.put(subAgent.name(), subAgent.name());

			// Recursively process this sub-agent's sub-agents if they exist
			if (subAgent instanceof FlowAgent subFlowAgent) {
				if (subFlowAgent.subAgents() == null || subFlowAgent.subAgents().isEmpty()) {
					graph.addEdge(subAgent.name(), END);
				}
			}
			else {
				graph.addEdge(subAgent.name(), END);
			}
		}

		// Connect parent to this sub-agent
		graph.addConditionalEdges(parentAgent.name(), new RoutingEdgeAction(chatModel, this, subAgents),
				edgeRoutingMap);
	}

	public static LlmRoutingAgentBuilder builder() {
		return new LlmRoutingAgentBuilder();
	}

	/**
	 * Builder for creating LlmRoutingAgent instances. Extends the common FlowAgentBuilder
	 * and adds LLM-specific configuration.
	 */
	public static class LlmRoutingAgentBuilder extends FlowAgentBuilder<LlmRoutingAgent, LlmRoutingAgentBuilder> {

		private ChatModel chatModel;

		/**
		 * Sets the ChatModel for LLM-based routing decisions.
		 * @param chatModel the chat model to use for routing
		 * @return this builder instance for method chaining
		 */
		public LlmRoutingAgentBuilder model(ChatModel chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		@Override
		protected LlmRoutingAgentBuilder self() {
			return this;
		}

		@Override
		protected void validate() {
			super.validate();
			if (chatModel == null) {
				throw new IllegalArgumentException("ChatModel must be provided for LLM routing agent");
			}
		}

		@Override
		public LlmRoutingAgent build() throws GraphStateException {
			validate();
			return new LlmRoutingAgent(this);
		}

	}

}
