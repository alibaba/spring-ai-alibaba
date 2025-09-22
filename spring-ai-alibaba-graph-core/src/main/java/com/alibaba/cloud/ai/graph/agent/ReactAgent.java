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
import com.alibaba.cloud.ai.graph.SubGraphNode;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.factory.AgentBuilderFactory;
import com.alibaba.cloud.ai.graph.agent.factory.DefaultAgentBuilderFactory;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.utils.Messageutils.convertToMessages;
import static java.lang.String.format;

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

	private Function<OverAllState, Boolean> shouldContinueFunc;

	public ReactAgent(LlmNode llmNode, ToolNode toolNode, Builder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.includeContents, builder.outputKey, builder.outputKeyStrategy);
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
		this.includeContents = builder.includeContents;
	}

	public static Builder builder() {
		return new DefaultAgentBuilderFactory().builder();
	}

	public static Builder builder(AgentBuilderFactory agentBuilderFactory) {
		return agentBuilderFactory.builder();
	}

	@Override
	public ScheduledAgentTask schedule(ScheduleConfig scheduleConfig) throws GraphStateException {
		CompiledGraph compiledGraph = getAndCompileGraph();
		return compiledGraph.schedule(scheduleConfig);
	}

	public AssistantMessage invoke(String message) throws GraphRunnerException {
		return invokeMessage(message);
	}

	public AssistantMessage invoke(String message, RunnableConfig config) throws GraphRunnerException {
		return invokeMessage(message, config);
	}

	public AssistantMessage invoke(UserMessage userMessage) throws GraphRunnerException {
		return invokeMessage(userMessage);
	}

	public AssistantMessage invoke(UserMessage userMessage, RunnableConfig config) throws GraphRunnerException {
		return invokeMessage(userMessage, config);
	}

	public AssistantMessage invoke(List<Message> messages) throws GraphRunnerException {
		return invokeMessage(messages);
	}

	public AssistantMessage invoke(List<Message> messages, RunnableConfig config) throws GraphRunnerException {
		return invokeMessage(messages, config);
	}

	public Flux<NodeOutput> stream(String message) throws GraphRunnerException {
		return stream(Map.of("messages", convertToMessages(message)));
	}

	public Flux<NodeOutput> stream(String message, RunnableConfig config) throws GraphRunnerException {
		return stream(Map.of("messages", convertToMessages(message)), config);
	}

	public Flux<NodeOutput> stream(UserMessage userMessage) throws GraphRunnerException {
		return stream(Map.of("messages", convertToMessages(userMessage)));
	}

	public Flux<NodeOutput> stream(UserMessage userMessage, RunnableConfig config) throws GraphRunnerException {
		return stream(Map.of("messages", convertToMessages(userMessage)), config);
	}

	public Flux<NodeOutput> stream(List<Message> messages) throws GraphRunnerException {
		return stream(Map.of("messages", messages));
	}

	public Flux<NodeOutput> stream(List<Message> messages, RunnableConfig config) throws GraphRunnerException {
		return stream(Map.of("messages", messages), config);
	}

	private AssistantMessage invokeMessage(Object message) throws GraphRunnerException {
		return invokeMessage(message, RunnableConfig.builder().build());
	}

	private AssistantMessage invokeMessage(Object message, RunnableConfig config) throws GraphRunnerException {
		List<Message> messages;
		if (message instanceof List) {
			messages = (List<Message>) message;
		} else {
			messages = convertToMessages(message);
		}

		Optional<OverAllState> state = invoke(Map.of("messages", messages), config);

		return state.flatMap(s -> s.value("messages"))
				.map(messageList -> (List<Message>) messageList)
				.stream()
				.flatMap(messageList -> messageList.stream())
				.filter(msg -> msg instanceof AssistantMessage)
				.map(msg -> (AssistantMessage) msg)
				.reduce((first, second) -> second)
				.orElse(new AssistantMessage("No response generated"));
	}

	public StateGraph getStateGraph() {
		return graph;
	}

	public CompiledGraph getCompiledGraph() {
		return compiledGraph;
	}

	@Override
	public Node asNode(boolean includeContents, String outputKeyToParent) {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return new AgentSubGraphNode(this.name, includeContents, outputKeyToParent, this.compiledGraph);
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

	public void setInstruction(String instruction) {
		this.instruction = instruction;
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

	public static class SubGraphNodeAdapter implements NodeActionWithConfig {

		private boolean includeContents;

		private String outputKeyToParent;

		private CompiledGraph childGraph;

		private CompileConfig parentCompileConfig;

		public SubGraphNodeAdapter(boolean includeContents, String outputKeyToParent,
				CompiledGraph childGraph, CompileConfig parentCompileConfig) {
			this.includeContents = includeContents;
			this.outputKeyToParent = outputKeyToParent;
			this.childGraph = childGraph;
			this.parentCompileConfig = parentCompileConfig;
		}

		public String subGraphId() {
			return format("subgraph_%s", childGraph.stateGraph.getName());
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState, RunnableConfig config) throws Exception {
			RunnableConfig subGraphRunnableConfig = getSubGraphRunnableConfig(config);
			Flux<GraphResponse<NodeOutput>> subGraphFlux;
			Object parentMessages = null;
			if (includeContents) {
				// by default, includeContents is true, we pass down the messages from the parent state
				subGraphFlux = childGraph.fluxDataStream(parentState, subGraphRunnableConfig);
			} else {
				Map<String, Object> stateForChild = new HashMap<>(parentState.data());
				parentMessages = stateForChild.remove("messages");
				// use the instruction directly, without any user message or parent messages.
				subGraphFlux = childGraph.fluxDataStream(stateForChild, subGraphRunnableConfig);
			}

			Map<String, Object> result = new HashMap<>();
			result.put(outputKeyToParent, subGraphFlux);
			if (parentMessages != null) {
				result.put("messages", parentMessages);
			}
			return result;
		}

		private RunnableConfig getSubGraphRunnableConfig(RunnableConfig config) {
			RunnableConfig subGraphRunnableConfig = config;
			var parentSaver = parentCompileConfig.checkpointSaver();
			var subGraphSaver = childGraph.compileConfig.checkpointSaver();

			if (subGraphSaver.isPresent()) {
				if (parentSaver.isEmpty()) {
					throw new IllegalStateException("Missing CheckpointSaver in parent graph!");
				}

				// Check saver are the same instance
				if (parentSaver.get() == subGraphSaver.get()) {
					subGraphRunnableConfig = RunnableConfig.builder(config)
							.threadId(config.threadId()
									.map(threadId -> format("%s_%s", threadId, subGraphId()))
									.orElseGet(this::subGraphId))
							.nextNode(null)
							.checkPointId(null)
							.build();
				}
			}
			return subGraphRunnableConfig;
		}

	}

	/**
	 * Internal class that adapts a ReactAgent to be used as a SubGraph Node.
	 * Similar to SubCompiledGraphNode but uses SubGraphNodeAdapter internally.
	 */
	private static class AgentSubGraphNode extends Node implements SubGraphNode {

		private final CompiledGraph subGraph;

		public AgentSubGraphNode(String id, boolean includeContents, String outputKeyToParent, CompiledGraph subGraph) {
			super(Objects.requireNonNull(id, "id cannot be null"),
					(config) -> AsyncNodeActionWithConfig.node_async(new SubGraphNodeAdapter(includeContents, outputKeyToParent, subGraph, config)));
			this.subGraph = subGraph;
		}

		@Override
		public StateGraph subGraph() {
			return subGraph.stateGraph;
		}
	}
}
