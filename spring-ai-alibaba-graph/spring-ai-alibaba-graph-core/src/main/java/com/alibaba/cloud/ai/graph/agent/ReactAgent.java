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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

	private List<String> tools;

	private int max_iterations = 10;

	private int iterations = 0;

	private CompileConfig compileConfig;

	private OverAllState state;

	private Function<OverAllState, Boolean> shouldContinueFunc;

	public ReactAgent(LlmNode llmNode, ToolNode toolNode, int maxIterations, OverAllState state,
			CompileConfig compileConfig, Function<OverAllState, Boolean> shouldContinueFunc)
			throws GraphStateException {
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.max_iterations = maxIterations;
		this.state = state;
		this.compileConfig = compileConfig;
		this.shouldContinueFunc = shouldContinueFunc;
		this.graph = initGraph();
	}

	public ReactAgent(String prompt, ChatClient chatClient, List<FunctionCallback> tools, int maxIterations)
			throws GraphStateException {
		this.llmNode = LlmNode.builder()
			.chatClient(chatClient)
			.userPromptTemplate(prompt)
			.messagesKey("messages")
			.build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		this.max_iterations = maxIterations;
		this.graph = initGraph();
	}

	public ReactAgent(String name, String prompt, ChatClient chatClient, List<FunctionCallback> tools,
			int maxIterations, OverAllState state, CompileConfig compileConfig,
			Function<OverAllState, Boolean> shouldContinueFunc) throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder()
			.chatClient(chatClient)
			.userPromptTemplate(prompt)
			.messagesKey("messages")
			.build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		this.max_iterations = maxIterations;
		this.state = state;
		this.compileConfig = compileConfig;
		this.graph = initGraph();
	}

	public ReactAgent(String name, String prompt, ChatClient chatClient, ToolCallbackResolver resolver,
			int maxIterations) throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder()
			.chatClient(chatClient)
			.userPromptTemplate(prompt)
			.messagesKey("messages")
			.build();
		this.toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		this.max_iterations = maxIterations;
		this.graph = initGraph();
	}

	public ReactAgent(String name, String prompt, ChatClient chatClient, ToolCallbackResolver resolver,
			int maxIterations, OverAllState state, CompileConfig compileConfig,
			Function<OverAllState, Boolean> shouldContinueFunc) throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder()
			.chatClient(chatClient)
			.userPromptTemplate(prompt)
			.messagesKey("messages")
			.build();
		this.toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		this.max_iterations = maxIterations;
		this.state = state;
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
		if (state == null) {
			OverAllState defaultState = new OverAllState();
			defaultState.registerKeyAndStrategy("messages", new AppendStrategy());
			this.state = defaultState;
		}

		return new StateGraph(name, state).addNode("agent", node_async(this.llmNode))
			.addNode("tool", node_async(this.toolNode))
			.addEdge(START, "agent")
			.addConditionalEdges("agent", edge_async(this::think), Map.of("continue", "tool", "end", END))
			.addEdge("tool", "agent");
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

	OverAllState getState() {
		return state;
	}

	void setState(OverAllState state) {
		this.state = state;
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

		private String prompt;

		private List<FunctionCallback> tools;

		private ToolCallbackResolver resolver;

		private int maxIterations = 10;

		private CompileConfig compileConfig;

		private OverAllState state;

		private Function<OverAllState, Boolean> shouldContinueFunc;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder prompt(String prompt) {
			this.prompt = prompt;
			return this;
		}

		public Builder tools(List<FunctionCallback> tools) {
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

		public Builder state(OverAllState state) {
			this.state = state;
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

		public ReactAgent build() throws GraphStateException {
			if (resolver != null) {
				return new ReactAgent(name, prompt, chatClient, resolver, maxIterations, state, compileConfig,
						shouldContinueFunc);
			}
			else if (tools != null) {
				return new ReactAgent(name, prompt, chatClient, tools, maxIterations, state, compileConfig,
						shouldContinueFunc);
			}
			throw new IllegalArgumentException("Either tools or resolver must be provided");
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
