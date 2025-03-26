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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

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
	private StateGraph graph;

	public ReactAgent(LlmNode llmNode, ToolNode toolNode, int maxIterations, OverAllState state, CompileConfig compileConfig) throws GraphStateException {
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.max_iterations = maxIterations;
		this.state = state;
		this.compileConfig = compileConfig;
		this.graph = initGraph();
	}

	public ReactAgent(String prompt, ChatClient chatClient, List<FunctionCallback> tools, int maxIterations) throws GraphStateException {
		this.llmNode = LlmNode.builder().chatClient(chatClient).promptTemplate(prompt).build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		this.max_iterations = maxIterations;
		this.graph = initGraph();
	}

	public ReactAgent(String prompt, ChatClient chatClient, List<FunctionCallback> tools, int maxIterations, OverAllState state, CompileConfig compileConfig) throws GraphStateException {
		this.llmNode = LlmNode.builder().chatClient(chatClient).promptTemplate(prompt).build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		this.max_iterations = maxIterations;
		this.state = state;
		this.compileConfig = compileConfig;
		this.graph = initGraph();
	}

	public ReactAgent(String prompt, ChatClient chatClient, ToolCallbackResolver resolver, int maxIterations) throws GraphStateException {
		this.llmNode = LlmNode.builder().chatClient(chatClient).promptTemplate(prompt).build();
		this.toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		this.max_iterations = maxIterations;
		this.graph = initGraph();
	}

	public ReactAgent(String prompt, ChatClient chatClient, ToolCallbackResolver resolver, int maxIterations, OverAllState state, CompileConfig compileConfig) throws GraphStateException {
		this.llmNode = LlmNode.builder().chatClient(chatClient).promptTemplate(prompt).build();
		this.toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		this.max_iterations = maxIterations;
		this.state = state;
		this.compileConfig = compileConfig;
		this.graph = initGraph();
	}

	public StateGraph getStateGraph()  {
		return graph;
	}

	public CompiledGraph getAndCompileGraph(CompileConfig compileConfig) throws GraphStateException {
		return getStateGraph().compile(compileConfig);
	}

	public CompiledGraph getAndCompileGraph() throws GraphStateException {
		return getStateGraph().compile(this.compileConfig);
	}

	private StateGraph initGraph() throws GraphStateException {
		if (state == null) {
			OverAllState defaultState = new OverAllState();
			defaultState.registerKeyAndStrategy("messages", List::of);
			defaultState.registerKeyAndStrategy("react_output", (o1, o2) -> o1);
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

		List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
		AssistantMessage message = (AssistantMessage)messages.get(messages.size() - 1);
		if (message.hasToolCalls()) {
			return "continue";
		}

		return "end";
	}

}
