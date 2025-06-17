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
package com.alibaba.cloud.ai.graph.agent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ReactAgent {

	private String name;

	private final LlmNode llmNode;

	private final ToolNode toolNode;

	private final StateGraph graph;

	private CompiledGraph compiledGraph;

	private NodeAction preLlmHook;

	private NodeAction postLlmHook;

	private NodeAction preToolHook;

	private NodeAction postToolHook;

	private List<String> tools;

	private int max_iterations = 10;

	private int iterations = 0;

	private CompileConfig compileConfig;

	private KeyStrategyFactory keyStrategyFactory;

	private Function<OverAllState, Boolean> shouldContinueFunc;

	private ReactAgent(String name, LlmNode llmNode, ToolNode toolNode, int maxIterations,
			KeyStrategyFactory keyStrategyFactory, CompileConfig compileConfig,
			Function<OverAllState, Boolean> shouldContinueFunc, NodeAction preLlmHook, NodeAction postLlmHook,
			NodeAction preToolHook, NodeAction postToolHook) throws GraphStateException {
		this.name = name;
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.max_iterations = maxIterations;
		this.keyStrategyFactory = keyStrategyFactory;
		this.compileConfig = compileConfig;
		this.shouldContinueFunc = shouldContinueFunc;
		this.preLlmHook = preLlmHook;
		this.postLlmHook = postLlmHook;
		this.preToolHook = preToolHook;
		this.postToolHook = postToolHook;
		this.graph = initGraph();
	}

	public ReactAgent(LlmNode llmNode, ToolNode toolNode, int maxIterations, KeyStrategyFactory keyStrategyFactory,
			CompileConfig compileConfig, Function<OverAllState, Boolean> shouldContinueFunc)
			throws GraphStateException {
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.max_iterations = maxIterations;
		this.keyStrategyFactory = keyStrategyFactory;
		this.compileConfig = compileConfig;
		this.shouldContinueFunc = shouldContinueFunc;
		this.graph = initGraph();
	}

	public ReactAgent(String name, ChatClient chatClient, List<ToolCallback> tools, int maxIterations)
			throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder().chatClient(chatClient).messagesKey("messages").build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		this.max_iterations = maxIterations;
		this.graph = initGraph();
	}

	public ReactAgent(String name, ChatClient chatClient, List<ToolCallback> tools, int maxIterations,
			KeyStrategyFactory keyStrategyFactory, CompileConfig compileConfig,
			Function<OverAllState, Boolean> shouldContinueFunc) throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder().chatClient(chatClient).messagesKey("messages").build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		this.max_iterations = maxIterations;
		this.keyStrategyFactory = keyStrategyFactory;
		this.compileConfig = compileConfig;
		this.graph = initGraph();
	}

	public ReactAgent(String name, ChatClient chatClient, ToolCallbackResolver resolver, int maxIterations)
			throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder()
			.chatClient(chatClient)
			// .userPromptTemplate(prompt)
			.messagesKey("messages")
			.build();
		this.toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		this.max_iterations = maxIterations;
		this.graph = initGraph();
	}

	public ReactAgent(String name, ChatClient chatClient, ToolCallbackResolver resolver, int maxIterations,
			KeyStrategyFactory keyStrategyFactory, CompileConfig compileConfig,
			Function<OverAllState, Boolean> shouldContinueFunc) throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder().chatClient(chatClient).messagesKey("messages").build();
		this.toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		this.max_iterations = maxIterations;
		this.keyStrategyFactory = keyStrategyFactory;
		this.compileConfig = compileConfig;
		this.shouldContinueFunc = shouldContinueFunc;
		this.graph = initGraph();
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

	public NodeAction asNodeAction(String inputKeyFromParent, String outputKeyToParent) {
		return new SubGraphNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph);
	}

	public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent) {
		if (this.compiledGraph == null) {
			throw new IllegalStateException("ReactAgent not compiled yet");
		}
		return node_async(new SubGraphNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph));
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

		if (preLlmHook != null) {
			graph.addNode("preLlm", node_async(preLlmHook));
		}
		graph.addNode("llm", node_async(this.llmNode));
		if (postLlmHook != null) {
			graph.addNode("postLlm", node_async(postLlmHook));
		}

		if (preToolHook != null) {
			graph.addNode("preTool", node_async(preToolHook));
		}
		graph.addNode("tool", node_async(this.toolNode));
		if (postToolHook != null) {
			graph.addNode("postTool", node_async(postToolHook));
		}

		if (preLlmHook != null) {
			graph.addEdge(START, "preLlm").addEdge("preLlm", "llm");
		}
		else {
			graph.addEdge(START, "llm");
		}

		if (postLlmHook != null) {
			graph.addEdge("llm", "postLlm")
				.addConditionalEdges("postLlm", edge_async(this::think),
						Map.of("continue", preToolHook != null ? "preTool" : "tool", "end", END));
		}
		else {
			graph.addConditionalEdges("llm", edge_async(this::think),
					Map.of("continue", preToolHook != null ? "preTool" : "tool", "end", END));
		}

		// 添加工具相关边
		if (preToolHook != null) {
			graph.addEdge("preTool", "tool");
		}
		if (postToolHook != null) {
			graph.addEdge("tool", "postTool").addEdge("postTool", preLlmHook != null ? "preLlm" : "llm");
		}
		else {
			graph.addEdge("tool", preLlmHook != null ? "preLlm" : "llm");
		}

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

	List<String> getTools() {
		return tools;
	}

	void setTools(List<String> tools) {
		this.tools = tools;
	}

	int getMax_iterations() {
		return max_iterations;
	}

	void setMax_iterations(int max_iterations) {
		this.max_iterations = max_iterations;
	}

	int getIterations() {
		return iterations;
	}

	void setIterations(int iterations) {
		this.iterations = iterations;
	}

	CompileConfig getCompileConfig() {
		return compileConfig;
	}

	void setCompileConfig(CompileConfig compileConfig) {
		this.compileConfig = compileConfig;
	}

	KeyStrategyFactory getKeyStrategyFactory() {
		return keyStrategyFactory;
	}

	void setOverAllStateFactory(KeyStrategyFactory keyStrategyFactory) {
		this.keyStrategyFactory = keyStrategyFactory;
	}

	Function<OverAllState, Boolean> getShouldContinueFunc() {
		return shouldContinueFunc;
	}

	void setShouldContinueFunc(Function<OverAllState, Boolean> shouldContinueFunc) {
		this.shouldContinueFunc = shouldContinueFunc;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String name;

		private ChatClient chatClient;

		private List<ToolCallback> tools;

		private ToolCallbackResolver resolver;

		private int maxIterations = 10;

		private CompileConfig compileConfig;

		private KeyStrategyFactory keyStrategyFactory;

		private Function<OverAllState, Boolean> shouldContinueFunc;

		private NodeAction preLlmHook;

		private NodeAction postLlmHook;

		private NodeAction preToolHook;

		private NodeAction postToolHook;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder tools(List<ToolCallback> tools) {
			this.tools = tools;
			return this;
		}

		public Builder resolver(ToolCallbackResolver resolver) {
			this.resolver = resolver;
			return this;
		}

		public Builder maxIterations(int maxIterations) {
			this.maxIterations = maxIterations;
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

		public Builder shouldContinueFunction(Function<OverAllState, Boolean> shouldContinueFunc) {
			this.shouldContinueFunc = shouldContinueFunc;
			return this;
		}

		public Builder preLlmHook(NodeAction preLlmHook) {
			this.preLlmHook = preLlmHook;
			return this;
		}

		public Builder postLlmHook(NodeAction postLlmHook) {
			this.postLlmHook = postLlmHook;
			return this;
		}

		public Builder preToolHook(NodeAction preToolHook) {
			this.preToolHook = preToolHook;
			return this;
		}

		public Builder postToolHook(NodeAction postToolHook) {
			this.postToolHook = postToolHook;
			return this;
		}

		public ReactAgent build() throws GraphStateException {
			LlmNode llmNode = LlmNode.builder().chatClient(chatClient).messagesKey("messages").build();
			ToolNode toolNode = null;
			if (resolver != null) {
				toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
			}
			else if (tools != null) {
				toolNode = ToolNode.builder().toolCallbacks(tools).build();
			}
			else {
				throw new IllegalArgumentException("Either tools or resolver must be provided");
			}

			return new ReactAgent(name, llmNode, toolNode, maxIterations, keyStrategyFactory, compileConfig,
					shouldContinueFunc, preLlmHook, postLlmHook, preToolHook, postToolHook);
		}

	}

	public static class SubGraphNodeAdapter implements NodeAction {

		private String inputKeyFromParent;

		private String outputKeyToParent;

		private CompiledGraph childGraph;

		SubGraphNodeAdapter(String inputKeyFromParent, String outputKeyToParent, CompiledGraph childGraph) {
			this.inputKeyFromParent = inputKeyFromParent;
			this.outputKeyToParent = outputKeyToParent;
			this.childGraph = childGraph;
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState) throws Exception {

			// prepare input for child graph
			String input = (String) parentState.value(inputKeyFromParent).orElseThrow();
			Message message = new UserMessage(input);
			List<Message> messages = List.of(message);

			// invoke child graph
			OverAllState childState = childGraph.invoke(Map.of("messages", messages)).get();

			// extract output from child graph
			List<Message> reactMessages = (List<Message>) childState.value("messages").orElseThrow();
			AssistantMessage assistantMessage = (AssistantMessage) reactMessages.get(reactMessages.size() - 1);
			String reactResult = assistantMessage.getText();

			// update parent state
			return Map.of(outputKeyToParent, reactResult);
		}

	}

}
