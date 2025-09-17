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
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.factory.AgentBuilderFactory;
import com.alibaba.cloud.ai.graph.agent.factory.DefaultAgentBuilderFactory;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;
import com.alibaba.cloud.ai.graph.scheduling.ScheduleConfig;
import com.alibaba.cloud.ai.graph.scheduling.ScheduledAgentTask;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

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

	private String inputKey;

	public ReactAgent(LlmNode llmNode, ToolNode toolNode, Builder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.outputKey);
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
		this.inputKey = builder.inputKey;
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

	@Override
	protected StateGraph initGraph() throws GraphStateException {
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
		return new DefaultAgentBuilderFactory().builder();
	}

	public static Builder builder(AgentBuilderFactory agentBuilderFactory) {
		return agentBuilderFactory.builder();
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
			OverAllState childState = childGraph.call(Map.of("messages", messages)).get();

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

			Flux<GraphResponse<NodeOutput>> subGraphFlux = childGraph.fluxDataStream(Map.of("messages", messages),
					RunnableConfig.builder().build());

			return Map.of(outputKeyToParent, subGraphFlux);
		}

	}

}
