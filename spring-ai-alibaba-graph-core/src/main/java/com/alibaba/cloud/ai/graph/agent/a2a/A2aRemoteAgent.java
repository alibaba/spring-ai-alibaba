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
package com.alibaba.cloud.ai.graph.agent.a2a;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import io.a2a.spec.AgentCard;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class A2aRemoteAgent extends BaseAgent {

	private final AgentCard agentCard;

	private final StateGraph graph;

	private CompiledGraph compiledGraph;

	private KeyStrategyFactory keyStrategyFactory;

	private A2aNode a2aNode;

	private CompileConfig compileConfig;

	private String inputKey;

	private boolean streaming;

	Logger logger = Logger.getLogger(A2aRemoteAgent.class.getName());

	// Private constructor for Builder pattern
	private A2aRemoteAgent(A2aNode a2aNode, Builder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey);
		this.agentCard = builder.agentCard;
		this.keyStrategyFactory = builder.keyStrategyFactory;
		this.compileConfig = builder.compileConfig;
		this.inputKey = builder.inputKey;
		this.streaming = builder.streaming;
		this.a2aNode = a2aNode;
		this.graph = initGraph();
	}

	private StateGraph initGraph() throws GraphStateException {
		if (keyStrategyFactory == null) {
			this.keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			};
		}

		StateGraph graph = new StateGraph(name, this.keyStrategyFactory);
		graph.addNode("A2aNode", node_async(a2aNode));
		graph.addEdge(StateGraph.START, "A2aNode");
		graph.addEdge("A2aNode", StateGraph.END);
		return graph;
	}

	@Override
	public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent) {
		return node_async(new A2aNode(agentCard, inputKeyFromParent, outputKeyToParent, streaming));
	}

	@Override
	public Optional<OverAllState> invoke(Map<String, Object> input, RunnableConfig runnableConfig)
			throws GraphStateException, GraphRunnerException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return this.compiledGraph.invoke(input, runnableConfig);
	}

	@Override
	public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) throws GraphStateException, GraphRunnerException {
		throw new UnsupportedOperationException("A2aRemoteAgent has not support schedule.");
	}

	public AsyncGenerator<NodeOutput> stream(Map<String, Object> input, RunnableConfig runnableConfig)
			throws GraphStateException, GraphRunnerException {
		if (!streaming) {
			logger.warning("Streaming is not enabled for this agent.");
		}
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return this.compiledGraph.stream(input, runnableConfig);
	}

	public StateGraph getStateGraph() {
		return graph;
	}

	public CompiledGraph getCompiledGraph() throws GraphStateException {
		return compiledGraph;
	}

	public CompiledGraph getAndCompileGraph(CompileConfig compileConfig) throws GraphStateException {
		this.compiledGraph = getStateGraph().compile(compileConfig);
		return this.compiledGraph;
	}

	public CompiledGraph getAndCompileGraph() throws GraphStateException {
		if (this.compileConfig == null) {
			this.compiledGraph = getStateGraph().compile();
		}
		else {
			this.compiledGraph = getStateGraph().compile(this.compileConfig);
		}
		return this.compiledGraph;
	}

	public NodeAction asNodeAction(String inputKeyFromParent, String outputKeyToParent) throws GraphStateException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return new SubGraphNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		// BaseAgent properties
		private String name;

		private String description;

		private String outputKey = "output";

		// A2aRemoteAgent specific properties
		private AgentCard agentCard;

		private String inputKey = "input";

		private KeyStrategyFactory keyStrategyFactory;

		private CompileConfig compileConfig;

		private boolean streaming = false;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder agentCard(AgentCard agentCard) {
			this.agentCard = agentCard;
			return this;
		}

		public Builder inputKey(String inputKey) {
			this.inputKey = inputKey;
			return this;
		}

		public Builder state(KeyStrategyFactory keyStrategyFactory) {
			this.keyStrategyFactory = keyStrategyFactory;
			return this;
		}

		public Builder compileConfig(CompileConfig compileConfig) {
			this.compileConfig = compileConfig;
			return this;
		}

		public Builder streaming(boolean streaming) {
			this.streaming = streaming;
			return this;
		}

		public A2aRemoteAgent build() throws GraphStateException {
			// Validation
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("Name must be provided");
			}
			if (description == null || description.trim().isEmpty()) {
				throw new IllegalArgumentException("Description must be provided");
			}
			if (agentCard == null) {
				throw new IllegalArgumentException("AgentCard must be provided");
			}

			this.streaming = agentCard.capabilities().streaming();
			A2aNode a2aNode = new A2aNode(agentCard, inputKey, outputKey, streaming);

			return new A2aRemoteAgent(a2aNode, this);
		}

	}

	public static class SubGraphNodeAdapter implements NodeAction {

		private final String inputKeyFromParent;

		private final String outputKeyToParent;

		private final CompiledGraph childGraph;

		public SubGraphNodeAdapter(String inputKeyFromParent, String outputKeyToParent, CompiledGraph childGraph) {
			this.inputKeyFromParent = inputKeyFromParent;
			this.outputKeyToParent = outputKeyToParent;
			this.childGraph = childGraph;
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState) throws Exception {
			// prepare input for child graph
			Object inputValue = parentState.value(inputKeyFromParent)
				.orElseThrow(() -> new IllegalArgumentException(
						"Input key '" + inputKeyFromParent + "' not found in state: " + parentState));

			// invoke child graph with input
			OverAllState childState = childGraph.invoke(Map.of("input", inputValue)).get();

			// extract output from child graph
			Object outputValue = childState.value(outputKeyToParent)
				.orElseThrow(() -> new IllegalArgumentException(
						"Output key '" + outputKeyToParent + "' not found in child state"));

			// update parent state
			return Map.of(outputKeyToParent, outputValue);
		}

	}

}
