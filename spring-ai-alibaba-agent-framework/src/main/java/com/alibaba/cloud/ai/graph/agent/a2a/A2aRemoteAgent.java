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
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import io.a2a.spec.AgentCard;

import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;


public class A2aRemoteAgent extends BaseAgent {
	Logger logger = Logger.getLogger(A2aRemoteAgent.class.getName());

	private final AgentCardWrapper agentCard;

	private KeyStrategyFactory keyStrategyFactory;

	private String instruction;

	private boolean streaming;

	private boolean shareState;

	// Private constructor for Builder pattern
	private A2aRemoteAgent(Builder builder) {
		super(builder.name, builder.description, builder.includeContents, builder.returnReasoningContents, builder.outputKey, builder.outputKeyStrategy);
		this.agentCard = builder.agentCard;
		this.keyStrategyFactory = builder.keyStrategyFactory;
		this.compileConfig = builder.compileConfig;
		this.includeContents = builder.includeContents;
		this.streaming = builder.streaming;
		this.instruction = builder.instruction;
		this.shareState = builder.shareState;
	}

	@Override
	protected StateGraph initGraph() throws GraphStateException {
		if (keyStrategyFactory == null) {
			this.keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			};
		}

		StateGraph graph = new StateGraph(name, this.keyStrategyFactory);
		graph.addNode("A2aNode", AsyncNodeActionWithConfig.node_async(new A2aNodeActionWithConfig(agentCard, name, includeContents, outputKey, instruction, streaming)));
		graph.addEdge(StateGraph.START, "A2aNode");
		graph.addEdge("A2aNode", StateGraph.END);
		return graph;
	}

	@Override
	public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) {
		throw new UnsupportedOperationException("A2aRemoteAgent has not support schedule.");
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public Node asNode(boolean includeContents, boolean returnReasoningContents, String outputKeyToParent) {
		return new A2aRemoteAgentNode(this.name, includeContents, returnReasoningContents, outputKeyToParent, this.instruction, this.agentCard, this.streaming, this.shareState, this.getAndCompileGraph());
	}

	/**
	 * Internal class that adapts an A2aRemoteAgent to be used as a Node.
	 * Similar to AgentSubGraphNode but uses A2aNodeActionWithConfig internally.
	 * Implements SubGraphNode interface to provide subgraph functionality.
	 */
	private static class A2aRemoteAgentNode extends Node implements SubGraphNode {

		private final CompiledGraph subGraph;

		public A2aRemoteAgentNode(String id, boolean includeContents, boolean returnReasoningContents, String outputKeyToParent, String instruction, AgentCardWrapper agentCard, boolean streaming, boolean shareState, CompiledGraph subGraph) {
			super(Objects.requireNonNull(id, "id cannot be null"),
					(config) -> AsyncNodeActionWithConfig.node_async(new A2aNodeActionWithConfig(agentCard, subGraph.stateGraph.getName(), includeContents, outputKeyToParent, instruction, streaming, shareState, config)));
			this.subGraph = subGraph;
		}

		@Override
		public StateGraph subGraph() {
			return subGraph.stateGraph;
		}
	}

	public static class Builder {

		// BaseAgent properties
		private String name;

		private String description;

		private String instruction;

		private String outputKey = "output";

		private KeyStrategy outputKeyStrategy;

		private boolean returnReasoningContents = false;

		// A2aRemoteAgent specific properties
		private AgentCardWrapper agentCard;

		private AgentCardProvider agentCardProvider;

		private boolean includeContents = true;

		private KeyStrategyFactory keyStrategyFactory;

		private CompileConfig compileConfig;

		private boolean streaming = false;

		private boolean shareState = true;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder outputKeyStrategy(KeyStrategy outputKeyStrategy) {
			this.outputKeyStrategy = outputKeyStrategy;
			return this;
		}

		public Builder returnReasoningContents(boolean returnReasoningContents) {
			this.returnReasoningContents = returnReasoningContents;
			return this;
		}

		public Builder agentCard(AgentCard agentCard) {
			this.agentCard = new AgentCardWrapper(agentCard);
			return this;
		}

		public Builder agentCardProvider(AgentCardProvider agentCardProvider) {
			this.agentCardProvider = agentCardProvider;
			return this;
		}

		public Builder includeContents(boolean includeContents) {
			this.includeContents = includeContents;
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

		public Builder shareState(boolean shareState) {
			this.shareState = shareState;
			return this;
		}

		public A2aRemoteAgent build() {
			// Validation
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("Name must be provided");
			}
			if (description == null || description.trim().isEmpty()) {
				throw new IllegalArgumentException("Description must be provided");
			}
			if (agentCard == null) {
				if (null == agentCardProvider) {
					throw new IllegalArgumentException("AgentCard or AgentCardProvider must be provided");
				}
				if (agentCardProvider.supportGetAgentCardByName()) {
					agentCard = agentCardProvider.getAgentCard(name);
				}
				else {
					agentCard = agentCardProvider.getAgentCard();
				}
			}

			this.streaming = agentCard.capabilities().streaming();

			return new A2aRemoteAgent(this);
		}

	}
}
