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
package com.alibaba.cloud.ai.graph.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.tools.Tool;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ReactAgent {
	private LlmNode llmNode;
	private ToolNode toolNode;

	private String prompt;
	private List<String> tools;
	private int max_iterations = 10;
	private int iterations = 0;
	private CompileConfig compileConfig;
	private OverAllState state;

	ReactAgent(LlmNode llmNode, ToolNode toolNode) {
		this.llmNode = llmNode;
		this.toolNode = toolNode;
	}

	ReactAgent(LlmNode llmNode, ToolNode toolNode, int maxIterations, OverAllState state, CompileConfig compileConfig) {
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.max_iterations = maxIterations;
		this.state = state;
		this.compileConfig = compileConfig;
	}

	ReactAgent(String prompt, List<String> tools, int maxIterations, OverAllState state, CompileConfig compileConfig) {
		this.llmNode = LlmNode.builder().withPrompt(prompt).build();
		this.toolNode = ToolNode.builder().withTools(tools).build();
		this.max_iterations = maxIterations;
		this.state = state;
		this.compileConfig = compileConfig;
	}

	public CompiledGraph init() throws GraphStateException {
		StateGraph graph = initGraph();
		return graph.compile(compileConfig);
	}

	private StateGraph initGraph() throws GraphStateException {
		if (state == null) {
			OverAllState defaultState = new OverAllState();
			defaultState.input(Map.of("query", "user input"));
			defaultState.registerKeyAndStrategy(ToolNode.TOOL_RESPONSE_KEY, List::of);
			defaultState.registerKeyAndStrategy(LlmNode.LLM_RESPONSE_KEY, (o1, o2) -> o2);
			this.state = defaultState;
		}

		StateGraph graph = new StateGraph(state)
				.addNode("agent", node_async(this.llmNode))
				.addNode("tool", node_async(this.toolNode))
				.addEdge(START, "agent")
				.addConditionalEdges("agent", edge_async(this::think), Map.of("continue", "tool", "end", END))
				.addEdge("tool", "agent");

		return graph;
	}

	private String think(OverAllState state) {
		if (iterations > max_iterations) {
			return "end";
		}

		AssistantMessage message = (AssistantMessage) state.value(LlmNode.LLM_RESPONSE_KEY).orElseThrow();
		if (message.hasToolCalls()) {
			return "continue";
		}

		return "end";
	}

}
