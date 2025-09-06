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
import java.util.Optional;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.async.AsyncGenerator;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.apache.commons.collections4.CollectionUtils;

import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ReactAgent extends BaseAgent {

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

	private String instruction;

	private Function<OverAllState, Boolean> shouldContinueFunc;

	private String inputKey;

	protected ReactAgent(LlmNode llmNode, ToolNode toolNode, Builder builder) throws GraphStateException {
		this.name = builder.name;
		this.description = builder.description;
		this.instruction = builder.instruction;
		this.outputKey = builder.outputKey;
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.keyStrategyFactory = builder.keyStrategyFactory;
		this.compileConfig = builder.compileConfig;
		this.shouldContinueFunc = builder.shouldContinueFunc;
		this.preLlmHook = builder.preLlmHook;
		this.postLlmHook = builder.postLlmHook;
		this.preToolHook = builder.preToolHook;
		this.postToolHook = builder.postToolHook;
		this.inputKey = builder.inputKey;

		// 初始化graph
		this.graph = initGraph();
	}

	public Optional<OverAllState> invoke(Map<String, Object> input) throws GraphStateException, GraphRunnerException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return this.compiledGraph.invoke(input);
	}

	public Optional<OverAllState> invoke(Map<String, Object> input, RunnableConfig runnableConfig)
			throws GraphStateException, GraphRunnerException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return this.compiledGraph.invoke(input);
	}

	@Override
	public AsyncGenerator<NodeOutput> stream(Map<String, Object> input)
			throws GraphStateException, GraphRunnerException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return this.compiledGraph.stream(input);
	}

	@Override
	public AsyncGenerator<NodeOutput> stream(Map<String, Object> input, RunnableConfig runnableConfig)
			throws GraphStateException, GraphRunnerException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return this.compiledGraph.stream(input);
	}

	@Override
	public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) throws GraphStateException {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.schedule(scheduleConfig);
	}

	public StateGraph getStateGraph() {
		return graph;
	}

	public CompiledGraph getCompiledGraph() throws GraphStateException {
		return compiledGraph;
	}

	public CompiledGraph getAndCompileGraph(CompileConfig compileConfig) throws GraphStateException {
		if (this.compileConfig == null) {
			this.compiledGraph = getStateGraph().compile();
		}
		else {
			this.compiledGraph = getStateGraph().compile(compileConfig);
		}
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

	public AsyncNodeAction asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent)
			throws GraphStateException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return node_async(new SubGraphStreamingNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph));
	}

	private StateGraph initGraph() throws GraphStateException {
		if (keyStrategyFactory == null) {
			this.keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				if (inputKey != null) {
					keyStrategyHashMap.put(inputKey, new ReplaceStrategy());
				}
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			};
		}
		else {
			KeyStrategyFactory originalFactory = this.keyStrategyFactory;
			this.keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>(originalFactory.apply());
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			};
		}

		NodeAction effectivePreLlmHook = this.preLlmHook;
		if (effectivePreLlmHook == null) {
			effectivePreLlmHook = state -> {
				if (state.value("messages").isPresent()) {
					List<Message> messages = (List<Message>) state.value("messages").orElseThrow();
					state.updateState(Map.of(this.inputKey, messages));
				}
				return Map.of();
			};
		}

		StateGraph graph = new StateGraph(name, this.keyStrategyFactory);

		graph.addNode("preLlm", node_async(effectivePreLlmHook));
		graph.addNode("llm", node_async(this.llmNode));
		if (postLlmHook != null) {
			graph.addNode("postLlm", node_async(this.postLlmHook));
		}

		if (preToolHook != null) {
			graph.addNode("preTool", node_async(this.preToolHook));
		}

		graph.addNode("tool", node_async(this.toolNode));

		if (postToolHook != null) {
			graph.addNode("postTool", node_async(this.postToolHook));
		}

		graph.addEdge(START, "preLlm").addEdge("preLlm", "llm");

		if (postLlmHook != null) {
			graph.addEdge("llm", "postLlm")
				.addConditionalEdges("postLlm", edge_async(this::think),
						Map.of("continue", preToolHook != null ? "preTool" : "tool", "end", END));
		}
		else {
			graph.addConditionalEdges("llm", edge_async(this::think),
					Map.of("continue", preToolHook != null ? "preTool" : "tool", "end", END));
		}

		// Add tool-related edges
		if (preToolHook != null) {
			graph.addEdge("preTool", "tool");
		}
		if (postToolHook != null) {
			graph.addEdge("tool", "postTool").addEdge("postTool", "preLlm");
		}
		else {
			graph.addEdge("tool", "preLlm");
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

	public String instruction() {
		return instruction;
	}

	/**
	 * Gets the agent's unique name.
	 * @return the unique name of the agent.
	 */
	public String name() {
		return name;
	}

	/**
	 * Gets the one-line description of the agent's capability.
	 * @return the description of the agent.
	 */
	public String description() {
		return description;
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

		private String description;

		private String instruction;

		private String outputKey;

		private ChatModel model;

		private ChatOptions chatOptions;

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

		private String inputKey = "messages";

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder model(ChatModel model) {
			this.model = model;
			return this;
		}

		public Builder chatOptions(ChatOptions chatOptions) {
			this.chatOptions = chatOptions;
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

		public Builder inputKey(String inputKey) {
			this.inputKey = inputKey;
			return this;
		}

		public ReactAgent build() throws GraphStateException {
			if (chatClient == null) {
				if (model == null) {
					throw new IllegalArgumentException("Either chatClient or model must be provided");
				}
				ChatClient.Builder clientBuilder = ChatClient.builder(model);
				if (chatOptions != null) {
					clientBuilder.defaultOptions(chatOptions);
				}
				if (instruction != null) {
					clientBuilder.defaultSystem(instruction);
				}
				chatClient = clientBuilder.build();
			}

			LlmNode.Builder llmNodeBuilder = LlmNode.builder()
				.stream(true)
				.chatClient(chatClient)
				.messagesKey(this.inputKey);
			if (outputKey != null && !outputKey.isEmpty()) {
				llmNodeBuilder.outputKey(outputKey);
			}
			if (CollectionUtils.isNotEmpty(tools)) {
				llmNodeBuilder.toolCallbacks(tools);
			}
			LlmNode llmNode = llmNodeBuilder.build();

			ToolNode toolNode = null;
			if (resolver != null) {
				toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
			}
			else if (tools != null) {
				toolNode = ToolNode.builder().toolCallbacks(tools).build();
			}
			else {
				toolNode = ToolNode.builder().build();
			}

			return new ReactAgent(llmNode, toolNode, this);
		}

	}

	public static class SubGraphNodeAdapter implements NodeAction {

		private String inputKeyFromParent;

		private String outputKeyToParent;

		private CompiledGraph childGraph;

		public SubGraphNodeAdapter(String inputKeyFromParent, String outputKeyToParent, CompiledGraph childGraph) {
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

	public static class SubGraphStreamingNodeAdapter implements NodeAction {

		private String inputKeyFromParent;

		private String outputKeyToParent;

		private CompiledGraph childGraph;

		public SubGraphStreamingNodeAdapter(String inputKeyFromParent, String outputKeyToParent,
				CompiledGraph childGraph) {
			this.inputKeyFromParent = inputKeyFromParent;
			this.outputKeyToParent = outputKeyToParent;
			this.childGraph = childGraph;
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState) throws Exception {
			String input = (String) parentState.value(inputKeyFromParent).orElseThrow();
			Message message = new UserMessage(input);
			List<Message> messages = List.of(message);

			AsyncGenerator<NodeOutput> child = childGraph.stream(Map.of("messages", messages));

			AsyncGenerator<NodeOutput> wrapped = new AsyncGenerator<NodeOutput>() {
				private volatile Map<String, Object> lastStateData;

				@Override
				public Data<NodeOutput> next() {
					Data<NodeOutput> data = child.next();
					if (data.isDone()) {
						String result = extractAssistantText(lastStateData);
						return Data.done(Map.of(outputKeyToParent, result));
					}
					if (data.isError()) {
						return data;
					}
					return Data.of(data.getData().thenApply(n -> {
						try {
							lastStateData = n.state().data();
						}
						catch (Exception ignored) {
						}
						return n;
					}));
				}
			};

			return Map.of(outputKeyToParent, wrapped);
		}

		private String extractAssistantText(Map<String, Object> stateData) {
			if (stateData == null) {
				return "";
			}
			Object msgs = stateData.get("messages");
			if (!(msgs instanceof List)) {
				return "";
			}
			List<?> list = (List<?>) msgs;
			if (list.isEmpty()) {
				return "";
			}
			Object last = list.get(list.size() - 1);
			if (last instanceof AssistantMessage assistant) {
				return assistant.getText();
			}
			return "";
		}

	}

}
