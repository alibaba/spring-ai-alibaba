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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.tools.Tool;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
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
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ReactAgent2 extends BaseAgent {
	private int iterations = 0;
	private int max_iterations = 10;
	private Function<OverAllState, Boolean> shouldContinueFunc;
	private String description;
	private String outputKey;
	private KeyStrategyFactory keyStrategyFactory;

	private StateGraph graph;
	private CompiledGraph compiledGraph;

	protected ReactAgent2(LlmNode llmNode, ToolNode toolNode, Builder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.subAgents);
		this.max_iterations = builder.maxIterations;
		this.shouldContinueFunc = builder.shouldContinueFunc;
		this.description = builder.description;
		this.outputKey = builder.outputKey;
		this.keyStrategyFactory = builder.keyStrategyFactory;
		this.graph = initGraph(llmNode, toolNode);
	}


	public CompiledGraph getCompiledGraph() throws GraphStateException {
		return compiledGraph;
	}

	public CompiledGraph getAndCompileGraph(CompileConfig compileConfig) throws GraphStateException {
		this.compiledGraph = graph.compile(compileConfig);
		return this.compiledGraph;
	}

	public CompiledGraph getAndCompileGraph() throws GraphStateException {
		this.compiledGraph = graph.compile();
		return this.compiledGraph;
	}

	private StateGraph initGraph(LlmNode llmNode, ToolNode toolNode) throws GraphStateException {
		if (keyStrategyFactory == null) {
			this.keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			};
		}

		StateGraph graph = new StateGraph(name, keyStrategyFactory)
				.addNode("agent", node_async(llmNode))
				.addNode("tool", node_async(toolNode))

				.addEdge(START, "agent")
				.addConditionalEdges("agent", edge_async(this::think),
						Map.of("continue", "tool", "end", END));



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

	public static class Builder {
		private String name;
		private String description;
		private String instruction;

		private String outputKey;

		private List<? extends BaseAgent> subAgents;

		private ChatModel model;

		private ChatOptions chatOptions;

		private List<ToolCallback> tools;

		private int maxIterations = 10;

		private CompileConfig compileConfig;

		private KeyStrategyFactory keyStrategyFactory;

		private Function<OverAllState, Boolean> shouldContinueFunc;

		public ReactAgent2.Builder name(String name) {
			this.name = name;
			return this;
		}

		public ReactAgent2.Builder description(String description) {
			this.description = description;
			return this;
		}

		public ReactAgent2.Builder model(ChatModel model) {
			this.model = model;
			return this;
		}

		public ReactAgent2.Builder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		public ReactAgent2.Builder tools(List<ToolCallback> tools) {
			this.tools = tools;
			return this;
		}

		public ReactAgent2.Builder maxIterations(int maxIterations) {
			this.maxIterations = maxIterations;
			return this;
		}

		public ReactAgent2.Builder state(KeyStrategyFactory keyStrategyFactory) {
			this.keyStrategyFactory = keyStrategyFactory;
			return this;
		}

		public ReactAgent2.Builder compileConfig(CompileConfig compileConfig) {
			this.compileConfig = compileConfig;
			return this;
		}

		public ReactAgent2.Builder shouldContinueFunction(Function<OverAllState, Boolean> shouldContinueFunc) {
			this.shouldContinueFunc = shouldContinueFunc;
			return this;
		}

		public ReactAgent2 build() throws GraphStateException {
			subAgents.forEach(agent -> {
				if (agent instanceof ReactAgent2) {
					ToolCallback toolCallback = AgentTool.getFunctionToolCallback((ReactAgent2) agent);
					this.tools.add(toolCallback);
				}
			});

			ChatClient chatClient = ChatClient.builder(model).defaultOptions(chatOptions).defaultSystem(instruction).defaultToolCallbacks(tools).build();

			LlmNode llmNode = LlmNode.builder().chatClient(chatClient).messagesKey("messages").build();
			ToolNode toolNode  = ToolNode.builder().toolCallbacks(tools).build();

			return new ReactAgent2(llmNode, toolNode, this);
		}

	}
}
