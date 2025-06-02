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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
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

	private List<String> tools;

	private int max_iterations = 10;

	private int iterations = 0;

	private CompileConfig compileConfig;

	private OverAllStateFactory overAllStateFactory;

	private Function<OverAllState, Boolean> shouldContinueFunc;

	private NodeAction preLlmHook;

	private NodeAction postLlmHook;


    public ReactAgent(String name, LlmNode llmNode, ToolNode toolNode,
                      int maxIterations,
                      CompileConfig compileConfig, OverAllStateFactory overAllStateFactory,
                      Function<OverAllState, Boolean> shouldContinueFunc,
                      NodeAction preLlmHook,
                      NodeAction postLlmHook) throws GraphStateException {
        this.name = name;
        this.llmNode = llmNode;
        this.toolNode = toolNode;
        this.max_iterations = maxIterations;
        this.compileConfig = compileConfig;
        this.overAllStateFactory = overAllStateFactory;
        this.shouldContinueFunc = shouldContinueFunc;
        this.preLlmHook = preLlmHook;
        this.postLlmHook = postLlmHook;
        this.graph = initGraph();
    }


    public ReactAgent(LlmNode llmNode, ToolNode toolNode, int maxIterations, OverAllStateFactory overAllStateFactory,
			CompileConfig compileConfig, Function<OverAllState, Boolean> shouldContinueFunc)
			throws GraphStateException {
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.max_iterations = maxIterations;
		this.overAllStateFactory = overAllStateFactory;
		this.compileConfig = compileConfig;
		this.shouldContinueFunc = shouldContinueFunc;
		this.graph = initGraph();
	}

	public ReactAgent(String name, ChatClient chatClient, List<ToolCallback> tools, int maxIterations)
			throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder().chatClient(chatClient).toolCallbacks(tools).messagesKey("messages").build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		this.max_iterations = maxIterations;
		this.graph = initGraph();
	}

	public ReactAgent(String name, ChatClient chatClient, List<ToolCallback> tools, int maxIterations,
			OverAllStateFactory overAllStateFactory, CompileConfig compileConfig,
			Function<OverAllState, Boolean> shouldContinueFunc) throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder().chatClient(chatClient).toolCallbacks(tools).chatOptions(ToolCallingChatOptions.builder()
				.internalToolExecutionEnabled(false).build()).messagesKey("messages").build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		this.max_iterations = maxIterations;
		this.overAllStateFactory = overAllStateFactory;
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
			OverAllStateFactory overAllStateFactory, CompileConfig compileConfig,
			Function<OverAllState, Boolean> shouldContinueFunc) throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder().chatClient(chatClient).chatOptions(ToolCallingChatOptions.builder()
				.internalToolExecutionEnabled(false).build()).messagesKey("messages").build();
		this.toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		this.max_iterations = maxIterations;
		this.overAllStateFactory = overAllStateFactory;
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
		if (overAllStateFactory == null) {
			this.overAllStateFactory = () -> {
				OverAllState defaultState = new OverAllState();
				defaultState.registerKeyAndStrategy("messages", new AppendStrategy());
				return defaultState;
			};
		}

		StateGraph graph = new StateGraph(this.overAllStateFactory);
		
		// Add main execution nodes
		graph.addNode("agent", node_async(this.llmNode))
			.addNode("tool", node_async(this.toolNode));

		// Add hook nodes if provided
		if (preLlmHook != null) {
			graph.addNode("pre_llm_hook", node_async(preLlmHook));
		}
		
		if (postLlmHook != null) {
			graph.addNode("post_llm_hook", node_async(postLlmHook));
		}

		// Connect nodes
		if (preLlmHook != null) {
			graph.addEdge(START, "pre_llm_hook")
				 .addEdge("pre_llm_hook", "agent");
		} else {
			graph.addEdge(START, "agent");
		}

		if (postLlmHook != null) {
			graph.addConditionalEdges("agent", edge_async(this::think), 
				Map.of("continue", "tool", "end", "post_llm_hook"))
				.addEdge("post_llm_hook", END);
		} else {
			graph.addConditionalEdges("agent", edge_async(this::think), 
				Map.of("continue", "tool", "end", END));
		}

		graph.addEdge("tool", "agent");

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

	OverAllStateFactory getOverAllStateFactory() {
		return overAllStateFactory;
	}

	void setOverAllStateFactory(OverAllStateFactory overAllStateFactory) {
		this.overAllStateFactory = overAllStateFactory;
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

		private LlmNode llmNode;

		private ToolCallbackResolver resolver;

		private int maxIterations = 10;

		private CompileConfig compileConfig;

		private OverAllStateFactory allStateFactory;

		private Function<OverAllState, Boolean> shouldContinueFunc;

		private NodeAction preLlmHook;

		private NodeAction postLlmHook;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder llmNode(LlmNode llmNode){
			this.llmNode = llmNode;
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

		public Builder state(OverAllStateFactory overAllStateFactory) {
			this.allStateFactory = overAllStateFactory;
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

		public ReactAgent build() throws GraphStateException {
			if (llmNode != null) {
				return new ReactAgent(name, llmNode, 
					ToolNode.builder().toolCallbacks(tools).build(),
					maxIterations, compileConfig, allStateFactory,
					shouldContinueFunc, preLlmHook, postLlmHook);
			}
			else if (resolver != null) {
				LlmNode node = LlmNode.builder()
					.chatClient(chatClient)
					.chatOptions(ToolCallingChatOptions.builder()
						.internalToolExecutionEnabled(false)
						.build())
					.messagesKey("messages")
					.build();
				return new ReactAgent(name, node,
					ToolNode.builder().toolCallbackResolver(resolver).build(),
					maxIterations, compileConfig, allStateFactory,
					shouldContinueFunc, preLlmHook, postLlmHook);
			}
			else if (tools != null) {
				LlmNode node = LlmNode.builder()
					.chatClient(chatClient)
					.chatOptions(ToolCallingChatOptions.builder()
						.internalToolExecutionEnabled(false)
						.build())
					.messagesKey("messages")
					.toolCallbacks(tools)
					.build();
				return new ReactAgent(name, node,
					ToolNode.builder().toolCallbacks(tools).build(),
					maxIterations, compileConfig, allStateFactory,
					shouldContinueFunc, preLlmHook, postLlmHook);
			}
			throw new IllegalArgumentException("Either llmNode, tools, or resolver must be provided");
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
