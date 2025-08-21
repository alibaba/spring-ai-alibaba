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
package com.alibaba.cloud.ai.graph.agent.flow;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ChatModel;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

public class SequentialAgent extends FlowAgent {

	protected SequentialAgent(Builder builder) throws GraphStateException {
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
	protected void processSubAgents(StateGraph graph, BaseAgent parentAgent, List<? extends BaseAgent> subAgents)
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

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		// Base agent properties
		private String name;

		private String description;

		private String outputKey;

		private List<? extends BaseAgent> subAgents;

		// LlmRoutingAgent specific properties
		private String inputKey;

		private KeyStrategyFactory keyStrategyFactory;

		private ChatModel chatModel;

		private CompileConfig compileConfig;

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

		public Builder subAgents(List<? extends BaseAgent> subAgents) {
			this.subAgents = subAgents;
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

		public Builder model(ChatModel chatModel) {
			this.chatModel = chatModel;
			return this;
		}

		public Builder compileConfig(CompileConfig compileConfig) {
			this.compileConfig = compileConfig;
			return this;
		}

		public SequentialAgent build() throws GraphStateException {
			// Validation
			if (name == null || name.trim().isEmpty()) {
				throw new IllegalArgumentException("Name must be provided");
			}
			if (subAgents == null || subAgents.isEmpty()) {
				throw new IllegalArgumentException("At least one agent must be provided for sequential flow");
			}

			return new SequentialAgent(this);
		}

	}

}
