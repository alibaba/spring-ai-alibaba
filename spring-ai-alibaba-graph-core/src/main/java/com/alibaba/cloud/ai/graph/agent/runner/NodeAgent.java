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
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class NodeAgent extends BaseNodeAgent {
	private String instruction;
	private String outputKey;

	private KeyStrategyFactory keyStrategyFactory;

	private StateGraph graph;
	private CompiledGraph compiledGraph;

	protected NodeAgent(Builder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.subAgents);
		this.instruction = builder.instruction;
		this.outputKey = builder.outputKey;
		this.graph = initGraph(llmNode, toolNode);
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
		graph.addConditionalEdges(parentNodeName, new RoutingEdgeAction(chatModel, this, subAgents), Map.of());

	}


	String instruction() {
		return instruction;
	}

	String outputKey() {
		return outputKey;
	}

	StateGraph stateGraph() {
		return graph;
	}

	private String think(OverAllState state) {
		if (iterations > max_iterations) {
			return "end";
		}

		if (shouldContinueFunc != null && !shouldContinueFunc.apply(state)) {
			return "end";
		}

		List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
		AssistantMessage message = (AssistantMessage) messages.get(messages.size() - 1);
		if (message.hasToolCalls()) {
			return "continue";
		}

		return "end";
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		return Map.of();
	}

	public static class Builder {
		private String name;
		private String description;
		private String instruction;

		private String outputKey;

		private List<? extends BaseNodeAgent> subAgents;

		private ChatModel model;

		private ChatOptions chatOptions;

		private List<ToolCallback> tools;

		private int maxIterations = 10;

		private CompileConfig compileConfig;

		private KeyStrategyFactory keyStrategyFactory;

		private Function<OverAllState, Boolean> shouldContinueFunc;

		public NodeAgent.Builder name(String name) {
			this.name = name;
			return this;
		}

		public NodeAgent.Builder description(String description) {
			this.description = description;
			return this;
		}

		public NodeAgent.Builder model(ChatModel model) {
			this.model = model;
			return this;
		}

		public NodeAgent.Builder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		public NodeAgent.Builder tools(List<ToolCallback> tools) {
			this.tools = tools;
			return this;
		}

		public NodeAgent.Builder maxIterations(int maxIterations) {
			this.maxIterations = maxIterations;
			return this;
		}

		public NodeAgent.Builder state(KeyStrategyFactory keyStrategyFactory) {
			this.keyStrategyFactory = keyStrategyFactory;
			return this;
		}

		public NodeAgent.Builder compileConfig(CompileConfig compileConfig) {
			this.compileConfig = compileConfig;
			return this;
		}

		public NodeAgent.Builder shouldContinueFunction(Function<OverAllState, Boolean> shouldContinueFunc) {
			this.shouldContinueFunc = shouldContinueFunc;
			return this;
		}

		public NodeAgent build() throws GraphStateException {

			ChatClient chatClient = ChatClient.builder(model).defaultOptions(chatOptions).defaultSystem(instruction).defaultToolCallbacks(tools).build();

			LlmNode llmNode = LlmNode.builder().chatClient(chatClient).messagesKey("messages").build();
			ToolNode toolNode  = ToolNode.builder().toolCallbacks(tools).build();

			return new NodeAgent(llmNode, toolNode, this);
		}

	}
}
