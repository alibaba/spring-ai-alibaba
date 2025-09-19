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
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.utils.Messageutils.convertToMessages;

public class ReactAgent extends BaseAgent {

	private final LlmNode llmNode;

	private final ToolNode toolNode;

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

	protected boolean includeContents;

	/** The output key for the agent's result */
	protected String outputKey;

	protected KeyStrategy outputKeyStrategy;

	private Function<OverAllState, Boolean> shouldContinueFunc;

	protected ReactAgent(LlmNode llmNode, ToolNode toolNode, Builder builder) throws GraphStateException {
		super(builder.name, builder.description);
		this.instruction = builder.instruction;
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.keyStrategyFactory = builder.keyStrategyFactory;
		this.compileConfig = builder.compileConfig;
		this.shouldContinueFunc = builder.shouldContinueFunc;
		this.preLlmHook = builder.preLlmHook;
		this.postLlmHook = builder.postLlmHook;
		this.preToolHook = builder.preToolHook;
		this.postToolHook = builder.postToolHook;
		this.outputKey = builder.outputKey;
		this.outputKeyStrategy = builder.outputKeyStrategy;
		this.includeContents = builder.includeContents;
	}

	public static Builder builder() {
		return new Builder();
	}

	public AssistantMessage invoke(String message) throws GraphStateException, GraphRunnerException {
		return invokeMessage(message);
	}

	public AssistantMessage invoke(UserMessage userMessage) throws GraphStateException, GraphRunnerException {
		return invokeMessage(userMessage);
	}

	public AssistantMessage invoke(List<Message> messages) throws GraphStateException, GraphRunnerException {
		return invokeMessage(messages);
	}

	public Flux<NodeOutput> stream(String message) throws GraphStateException, GraphRunnerException {
		return stream(Map.of("messages", convertToMessages(message)));
	}

	public Flux<NodeOutput> stream(UserMessage userMessage) throws GraphStateException, GraphRunnerException {
		return stream(Map.of("messages", convertToMessages(userMessage)));
	}

	public Flux<NodeOutput> stream(List<Message> messages) throws GraphStateException, GraphRunnerException {
		return stream(Map.of("messages", messages));
	}

	private AssistantMessage invokeMessage(Object message) throws GraphStateException, GraphRunnerException {
		List<Message> messages;
		if (message instanceof List) {
			messages = (List<Message>) message;
		} else {
			messages = convertToMessages(message);
		}

		Optional<OverAllState> state = invoke(Map.of("messages", messages));

		return state.flatMap(s -> s.value("messages"))
				.map(messageList -> (List<Message>) messageList)
				.filter(messageList -> !messageList.isEmpty())
				.map(messageList -> (AssistantMessage) messageList.get(0))
				.orElse(new AssistantMessage("No response generated"));
	}

	public StateGraph getStateGraph() {
		return graph;
	}

	public CompiledGraph getCompiledGraph() throws GraphStateException {
		return compiledGraph;
	}

	public AsyncNodeAction asAsyncNodeAction(boolean includeContents, String outputKeyToParent)
			throws GraphStateException {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return node_async(new SubGraphNodeAdapter(includeContents, outputKeyToParent, this.compiledGraph));
	}

	@Override
	protected StateGraph initGraph() throws GraphStateException {
		insureMessagesKeyStrategyFactory();

		NodeAction effectivePreLlmHook = this.preLlmHook;
		if (effectivePreLlmHook == null) {
			effectivePreLlmHook = state -> {
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

	private void insureMessagesKeyStrategyFactory() {
		if (keyStrategyFactory == null) {
			this.keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
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

	public boolean isIncludeContents() {
		return includeContents;
	}

	public void setIncludeContents(boolean includeContents) {
		this.includeContents = includeContents;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

	public KeyStrategy getOutputKeyStrategy() {
		return outputKeyStrategy;
	}

	public void setOutputKeyStrategy(KeyStrategy outputKeyStrategy) {
		this.outputKeyStrategy = outputKeyStrategy;
	}

	public static class Builder {

		private String name;

		private String description;

		private String instruction;

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

		private boolean includeContents = true;

		protected String outputKey;

		protected KeyStrategy outputKeyStrategy;

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

		public Builder outputKeyStrategy(KeyStrategy outputKeyStrategy) {
			this.outputKeyStrategy = outputKeyStrategy;
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

		public Builder includeContents(boolean includeContents) {
			this.includeContents = includeContents;
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
					.systemPromptTemplate(instruction)
				.chatClient(chatClient)
				.messagesKey(this.inputKey);

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

		private boolean includeContents;

		private String outputKeyToParent;

		private CompiledGraph childGraph;

		public SubGraphNodeAdapter(boolean includeContents, String outputKeyToParent,
				CompiledGraph childGraph) {
			this.includeContents = includeContents;
			this.outputKeyToParent = outputKeyToParent;
			this.childGraph = childGraph;
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState) throws Exception {
			Flux<GraphResponse<NodeOutput>> subGraphFlux;
			Object parentMessages = null;
			if (includeContents) {
				// by default, includeContents is true, we pass down the messages from the parent state
				subGraphFlux = childGraph.fluxDataStream(parentState, RunnableConfig.builder().build());
			} else {
				Map<String, Object> stateForChild = new HashMap<>(parentState.data());
				parentMessages = stateForChild.remove("messages");
				// use the instruction directly, without any user message or parent messages.
				subGraphFlux = childGraph.fluxDataStream(stateForChild, RunnableConfig.builder().build());
			}

			Map<String, Object> result = new HashMap<>();
			result.put(outputKeyToParent, subGraphFlux);
			if (parentMessages != null) {
				result.put("messages", parentMessages);
			}
			return result;
		}

	}
}
