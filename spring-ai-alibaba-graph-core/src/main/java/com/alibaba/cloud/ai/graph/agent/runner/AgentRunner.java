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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.apache.james.mime4j.field.address.BaseNode;

import org.springframework.ai.chat.model.ChatModel;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class AgentRunner {
	private NodeAgent agent;
	private KeyStrategyFactory keyStrategyFactory;
	private StateGraph graph;
	private ChatModel chatModel;

	public AgentRunner(NodeAgent agent, KeyStrategyFactory keyStrategyFactory, ChatModel chatModel) throws GraphStateException {
		this.keyStrategyFactory = keyStrategyFactory;
		this.agent = agent;
		this.chatModel = chatModel;
		this.graph = initGraph(agent);
	}

	public Optional<OverAllState> run(Map<String, Object> inputs) throws GraphStateException, GraphRunnerException {
		CompiledGraph compiledGraph = this.graph.compile();
		return compiledGraph.invoke(inputs);
	}

	private StateGraph initGraph(NodeAgent agent) throws GraphStateException {
		if (keyStrategyFactory == null) {
			this.keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			};
		}

		StateGraph graph = new StateGraph(agent.name(), keyStrategyFactory)
				.addNode("agent", node_async(agent));

		// Use recursive method to add all sub-agents
		addSubAgentsRecursively(graph, "agent", agent.subAgents);

		return graph;
	}

	/**
	 * Recursively adds sub-agents and their nested sub-agents to the graph
	 * @param graph the StateGraph to add nodes and edges to
	 * @param parentNodeName the name of the parent node
	 * @param subAgents the list of sub-agents to process
	 */
	private void addSubAgentsRecursively(StateGraph graph, String parentNodeName, List<? extends BaseNodeAgent> subAgents) throws GraphStateException {
		for (BaseNodeAgent subAgent : subAgents) {
			// Add the current sub-agent as a node
			graph.addNode(subAgent.name(), node_async(subAgent));
			// Recursively process this sub-agent's sub-agents if they exist
			if (subAgent.subAgents != null && !subAgent.subAgents.isEmpty()) {
				addSubAgentsRecursively(graph, subAgent.name(), subAgent.subAgents);
			}
		}

		// Connect parent to this sub-agent
		graph.addConditionalEdges(parentNodeName, new RoutingEdgeAction(chatModel, subAgents), Map.of());

	}

}
