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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public abstract class FlowAgent extends BaseAgent {

	protected CompileConfig compileConfig;

	protected String inputKey;

	protected KeyStrategyFactory keyStrategyFactory;

	protected List<? extends BaseAgent> subAgents;

	protected StateGraph graph;

	protected CompiledGraph compiledGraph;

	protected FlowAgent(String name, String description, String outputKey, String inputKey,
			KeyStrategyFactory keyStrategyFactory, CompileConfig compileConfig, List<? extends BaseAgent> subAgents)
			throws GraphStateException {
		super(name, description, outputKey);
		this.compileConfig = compileConfig;
		this.inputKey = inputKey;
		this.keyStrategyFactory = keyStrategyFactory;
		this.subAgents = subAgents;
	}

	protected StateGraph initGraph() throws GraphStateException {
		StateGraph graph = new StateGraph(this.name(), keyStrategyFactory);

		// add root agent
		graph.addNode(this.name(), node_async(new TransparentNode(this.outputKey, this.inputKey)));

		// add starting edge
		graph.addEdge(START, this.name());
		// Use recursive method to add all sub-agents
		processSubAgents(graph, this, this.subAgents());

		return graph;
	}

	protected abstract void processSubAgents(StateGraph graph, BaseAgent parentAgent,
			List<? extends BaseAgent> subAgents) throws GraphStateException;

	@Override
	public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent)
			throws GraphStateException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return node_async(
				new ReactAgent.SubGraphNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph));
	}

	public CompiledGraph getAndCompileGraph() throws GraphStateException {
		if (this.compileConfig == null) {
			this.compiledGraph = graph.compile();
		}
		else {
			this.compiledGraph = graph.compile(this.compileConfig);
		}
		return this.compiledGraph;
	}

	public CompileConfig compileConfig() {
		return compileConfig;
	}

	public String inputKey() {
		return inputKey;
	}

	public KeyStrategyFactory keyStrategyFactory() {
		return keyStrategyFactory;
	}

	public List<? extends BaseAgent> subAgents() {
		return this.subAgents;
	}

}
