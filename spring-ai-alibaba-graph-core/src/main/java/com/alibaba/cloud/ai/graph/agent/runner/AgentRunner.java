/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.runner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ChatModel;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

public class AgentRunner {
	private BaseAgent agent;
	private String inputKey;
	private KeyStrategyFactory keyStrategyFactory;
	private StateGraph graph;
	private ChatModel chatModel;

	public AgentRunner(BaseAgent agent, KeyStrategyFactory keyStrategyFactory, ChatModel chatModel) throws GraphStateException {
		this.keyStrategyFactory = keyStrategyFactory;
		this.agent = agent;
		this.chatModel = chatModel;
		this.graph = initGraph(agent);
	}

	public Optional<OverAllState> run(Map<String, Object> inputs) throws GraphStateException, GraphRunnerException {
		CompiledGraph compiledGraph = this.graph.compile();
		return compiledGraph.invoke(inputs);
	}

	private StateGraph initGraph(BaseAgent agent) throws GraphStateException {
		StateGraph graph = new StateGraph(agent.name(), keyStrategyFactory);

		// add root agent
		graph.addNode(agent.name(), agent.asAsyncNodeAction(inputKey, agent.outputKey()));

		// add starting edge
		graph.addEdge(START, agent.name());
		// Use recursive method to add all sub-agents
		addSubAgentsRecursively(graph,  agent, agent.subAgents());

		return graph;
	}

	/**
	 * Recursively adds sub-agents and their nested sub-agents to the graph
	 * @param graph the StateGraph to add nodes and edges to
	 * @param parentAgent the name of the parent node
	 * @param subAgents the list of sub-agents to process
	 */
	private void addSubAgentsRecursively(StateGraph graph, BaseAgent parentAgent, List<? extends BaseAgent> subAgents) throws GraphStateException {
		if (parentAgent instanceof ReactAgent) {
			for (BaseAgent subAgent : subAgents) {
				// Add the current sub-agent as a node
				graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(parentAgent.outputKey(), subAgent.outputKey()));

				// Recursively process this sub-agent's sub-agents if they exist
				if (subAgent.subAgents() != null && !subAgent.subAgents().isEmpty()) {
					addSubAgentsRecursively(graph, parentAgent, subAgent.subAgents());
				} else {
					graph.addEdge(subAgent.name(), END);
				}
			}

			// Connect parent to this sub-agent
			graph.addConditionalEdges(parentAgent.name(), AsyncEdgeAction.edge_async(new RoutingEdgeAction(chatModel, this.agent, subAgents)), Map.of());
		} else if (parentAgent instanceof SequentialAgent) {
			for (BaseAgent subAgent : subAgents) {
				// Add the current sub-agent as a node
				graph.addNode(subAgent.name(), subAgent.asAsyncNodeAction(parentAgent.outputKey(), subAgent.outputKey()));
				graph.addEdge(parentAgent.name(), subAgent.name());
				parentAgent = subAgent;
			}
			// parent agent is the last one in the sub agent list, so we connect it to END
			graph.addEdge(parentAgent.name(), END);
		}

	}

}
