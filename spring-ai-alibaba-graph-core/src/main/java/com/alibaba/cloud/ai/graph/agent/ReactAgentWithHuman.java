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
import java.util.UUID;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;
import com.alibaba.cloud.ai.graph.node.HumanNode;
import com.alibaba.cloud.ai.graph.node.LlmNode;
import com.alibaba.cloud.ai.graph.node.ToolNode;

import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ReactAgentWithHuman {

	private String name;

	private LlmNode llmNode;

	private ToolNode toolNode;

	private HumanNode humanNode;

	private StateGraph graph;

	private CompiledGraph compiledGraph;

	private String prompt;

	private List<String> tools;

	private int max_iterations = 10;

	private int iterations = 0;

	private CompileConfig compileConfig;

	private KeyStrategyFactory keyStrategyFactory;

	private Function<OverAllState, Boolean> shouldContinueFunc;

	public ReactAgentWithHuman(String name, String prompt, ChatClient chatClient, List<ToolCallback> tools,
			int maxIterations) throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder()
			.chatClient(chatClient)
			.userPromptTemplate(prompt)
			.messagesKey("messages")
			.build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		this.max_iterations = maxIterations;
		this.graph = initGraph();
	}

	public ReactAgentWithHuman(String name, String prompt, ChatClient chatClient, List<ToolCallback> tools,
			int maxIterations, KeyStrategyFactory keyStrategyFactory, CompileConfig compileConfig,
			Function<OverAllState, Boolean> shouldContinueFunc, Function<OverAllState, Boolean> shouldInterruptFunc)
			throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder()
			.chatClient(chatClient)
			.userPromptTemplate(prompt)
			.messagesKey("messages")
			.build();
		this.toolNode = ToolNode.builder().toolCallbacks(tools).build();
		if (shouldInterruptFunc != null) {
			this.humanNode = new HumanNode("conditioned", shouldInterruptFunc);
		}
		else {
			this.humanNode = new HumanNode();
		}
		this.max_iterations = maxIterations;
		this.keyStrategyFactory = keyStrategyFactory;
		this.compileConfig = compileConfig;
		this.shouldContinueFunc = shouldContinueFunc;
		this.graph = initGraph();
	}

	public ReactAgentWithHuman(String name, String prompt, ChatClient chatClient, ToolCallbackResolver resolver,
			int maxIterations) throws GraphStateException {
		this(name, prompt, chatClient, resolver, maxIterations, null, null, null, null);
	}

	public ReactAgentWithHuman(String name, String prompt, ChatClient chatClient, ToolCallbackResolver resolver,
			int maxIterations, KeyStrategyFactory keyStrategyFactory, CompileConfig compileConfig,
			Function<OverAllState, Boolean> shouldContinueFunc, Function<OverAllState, Boolean> shouldInterruptFunc)
			throws GraphStateException {
		this.name = name;
		this.llmNode = LlmNode.builder()
			.chatClient(chatClient)
			.userPromptTemplate(prompt)
			.messagesKey("messages")
			.build();
		this.toolNode = ToolNode.builder().toolCallbackResolver(resolver).build();
		if (shouldInterruptFunc != null) {
			this.humanNode = new HumanNode("conditioned", shouldInterruptFunc);
		}
		else {
			this.humanNode = new HumanNode();
		}
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

	public NodeActionWithConfig asNodeAction(String inputKeyFromParent, String outputKeyToParent) {
		return new SubGraphNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph);
	}

	public AsyncNodeActionWithConfig asAsyncNodeAction(String inputKeyFromParent, String outputKeyToParent) {
		return AsyncNodeActionWithConfig
			.node_async(new SubGraphNodeAdapter(inputKeyFromParent, outputKeyToParent, this.compiledGraph));
	}

	private StateGraph initGraph() throws GraphStateException {
		if (keyStrategyFactory == null) {
			this.keyStrategyFactory = () -> {
				HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
				keyStrategyHashMap.put("messages", new AppendStrategy());
				return keyStrategyHashMap;
			};
		}

		StateGraph graph = new StateGraph(name, keyStrategyFactory).addNode("agent", node_async(this.llmNode))
			.addNode("human", node_async(this.humanNode))
			.addNode("tool", node_async(this.toolNode))
			.addEdge(START, "agent")
			.addEdge("agent", "human")
			.addConditionalEdges("human", edge_async(humanNode::think),
					Map.of("agent", "agent", "tool", "tool", "end", END))
			.addEdge("tool", "agent");

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	void setKeyStrategyFactory(KeyStrategyFactory keyStrategyFactory) {
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

		private String prompt;

		private List<ToolCallback> tools;

		private ToolCallbackResolver resolver;

		private int maxIterations = 10;

		private CompileConfig compileConfig;

		private KeyStrategyFactory keyStrategyFactory;

		private Function<OverAllState, Boolean> shouldContinueFunc;

		private Function<OverAllState, Boolean> shouldInterruptFunc;

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

		public Builder shouldInterruptFunction(Function<OverAllState, Boolean> shouldInterruptFunc) {
			this.shouldInterruptFunc = shouldInterruptFunc;
			return this;
		}

		public ReactAgentWithHuman build() throws GraphStateException {
			if (resolver != null) {
				return new ReactAgentWithHuman(name, prompt, chatClient, resolver, maxIterations, keyStrategyFactory,
						compileConfig, shouldContinueFunc, shouldInterruptFunc);
			}
			else if (tools != null) {
				return new ReactAgentWithHuman(name, prompt, chatClient, tools, maxIterations, keyStrategyFactory,
						compileConfig, shouldContinueFunc, shouldInterruptFunc);
			}
			throw new IllegalArgumentException("Either tools or resolver must be provided");
		}

	}

	public static class SubGraphNodeAdapter implements NodeActionWithConfig {

		public String uuid = UUID.randomUUID().toString();

		private String inputKeyFromParent;

		private String outputKeyToParent;

		private CompiledGraph childGraph;

		SubGraphNodeAdapter(String inputKeyFromParent, String outputKeyToParent, CompiledGraph childGraph) {
			this.inputKeyFromParent = inputKeyFromParent;
			this.outputKeyToParent = outputKeyToParent;
			this.childGraph = childGraph;
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState, RunnableConfig config) throws Exception {
			RunnableConfig subConfig = RunnableConfig.builder()
				.threadId(uuid + "-" + config.threadId())
				.nextNode(config.nextNode().orElse(null))
				.checkPointId(config.checkPointId().orElse(null))
				.streamMode(config.streamMode())
				.build();
			OverAllState childState = null;
			if (parentState.humanFeedback() != null && parentState.isResume()) {
				// invoke child graph
				childState = childGraph.resume(parentState.humanFeedback(), subConfig).get();
			}
			else {
				// prepare input for child graph
				String input = (String) parentState.value(inputKeyFromParent).orElseThrow();
				Message message = new UserMessage(input);
				List<Message> messages = List.of(message);

				// invoke child graph
				childState = childGraph.invoke(Map.of("messages", messages), subConfig).get();
			}

			// extract output from child graph
			List<Message> reactMessages = (List<Message>) childState.value("messages").orElseThrow();
			AssistantMessage assistantMessage = (AssistantMessage) reactMessages.get(reactMessages.size() - 1);
			String reactResult = assistantMessage.getText();

			if (StringUtils.hasLength(childState.interruptMessage())) {
				throw RunnableErrors.subGraphInterrupt.exception(childState.interruptMessage());
			}

			// update parent state
			return Map.of(outputKeyToParent, reactResult);
		}

	}

}
