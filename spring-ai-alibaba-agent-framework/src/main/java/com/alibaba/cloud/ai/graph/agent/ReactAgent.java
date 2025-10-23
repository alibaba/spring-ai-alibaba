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
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.factory.AgentBuilderFactory;
import com.alibaba.cloud.ai.graph.agent.factory.DefaultAgentBuilderFactory;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.ToolInjection;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;

import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static java.lang.String.format;


public class ReactAgent extends BaseAgent {

	private final AgentLlmNode llmNode;

	private final AgentToolNode toolNode;

	private CompiledGraph compiledGraph;

	private List<Hook> hooks;

	private List<ModelInterceptor> modelInterceptors;

	private List<ToolInterceptor> toolInterceptors;

	private int max_iterations = 10;

	private int iterations = 0;

	private String instruction;

	private Function<OverAllState, Boolean> shouldContinueFunc;

	public ReactAgent(AgentLlmNode llmNode, AgentToolNode toolNode, Builder builder) throws GraphStateException {
		super(builder.name, builder.description, builder.includeContents, builder.returnReasoningContents, builder.outputKey, builder.outputKeyStrategy);
		this.instruction = builder.instruction;
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.compileConfig = builder.compileConfig;
		this.shouldContinueFunc = builder.shouldContinueFunc;
		this.hooks = builder.hooks;
		this.modelInterceptors = builder.modelInterceptors;
		this.toolInterceptors = builder.toolInterceptors;
		this.includeContents = builder.includeContents;
		this.inputSchema = builder.inputSchema;
		this.inputType = builder.inputType;
		this.outputSchema = builder.outputSchema;
		this.outputType = builder.outputType;

		// Set interceptors to nodes
		if (this.modelInterceptors != null && !this.modelInterceptors.isEmpty()) {
			this.llmNode.setModelInterceptors(this.modelInterceptors);
		}
		if (this.toolInterceptors != null && !this.toolInterceptors.isEmpty()) {
			this.toolNode.setToolInterceptors(this.toolInterceptors);
		}
	}

	public static com.alibaba.cloud.ai.graph.agent.Builder builder() {
		return new DefaultAgentBuilderFactory().builder();
	}

	public static Builder builder(AgentBuilderFactory agentBuilderFactory) {
		return agentBuilderFactory.builder();
	}

	public AssistantMessage call(String message) throws GraphRunnerException {
		return doMessageInvoke(message, null);
	}

	public AssistantMessage call(String message, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(message, config);
	}

	public AssistantMessage call(UserMessage message) throws GraphRunnerException {
		return doMessageInvoke(message, null);
	}

	public AssistantMessage call(UserMessage message, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(message, config);
	}

	public AssistantMessage call(List<Message> messages) throws GraphRunnerException {
		return doMessageInvoke(messages, null);
	}

	public AssistantMessage call(List<Message> messages, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(messages, config);
	}

	private AssistantMessage doMessageInvoke(Object message, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs= buildMessageInput(message);
		Optional<OverAllState> state = doInvoke(inputs, config);

		if (StringUtils.hasLength(outputKey)) {
			return state.flatMap(s -> s.value(outputKey))
					.map(msg -> (AssistantMessage) msg)
					.orElseThrow(() -> new IllegalStateException("Output key " + outputKey + " not found in agent state") );
		}
		return state.flatMap(s -> s.value("messages"))
				.map(messageList -> (List<Message>) messageList)
				.stream()
				.flatMap(messageList -> messageList.stream())
				.filter(msg -> msg instanceof AssistantMessage)
				.map(msg -> (AssistantMessage) msg)
				.reduce((first, second) -> second)
				.orElseThrow(() -> new IllegalStateException("No AssistantMessage found in 'messages' state") );
	}

	public StateGraph getStateGraph() {
		return graph;
	}

	public CompiledGraph getCompiledGraph() {
		return compiledGraph;
	}

	@Override
	public Node asNode(boolean includeContents, boolean returnReasoningContents, String outputKeyToParent) {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return new AgentSubGraphNode(this.name, includeContents, returnReasoningContents, outputKeyToParent, this.compiledGraph, this.instruction);
	}

	@Override
	protected StateGraph initGraph() throws GraphStateException {
		KeyStrategyFactory keyStrategyFactory = buildMessagesKeyStrategyFactory();

		if (hooks == null) {
			hooks = new ArrayList<>();
		}

		// Validate hook uniqueness
		Set<String> hookNames = new HashSet<>();
		for (Hook hook : hooks) {
			if (!hookNames.add(hook.getName())) {
				throw new IllegalArgumentException("Duplicate hook instances found");
			}
		}

		// Create graph
		StateGraph graph = new StateGraph(name, keyStrategyFactory);

		graph.addNode("model", node_async(this.llmNode));
		graph.addNode("tool", node_async(this.toolNode));

		// some hooks may need tools so they can do some initialization/cleanup on start/end of agent loop
		setupToolsForHooks(hooks, toolNode);

		// Add hook nodes
		for (Hook hook : hooks) {
			if (hook instanceof AgentHook agentHook) {
				graph.addNode(hook.getName() + ".before", agentHook::beforeAgent);
				graph.addNode(hook.getName() + ".after", agentHook::afterAgent);
			} else if (hook instanceof ModelHook modelHook) {
				graph.addNode(hook.getName() + ".beforeModel", modelHook::beforeModel);
				if (modelHook instanceof HumanInTheLoopHook humanInTheLoopHook) {
					graph.addNode(hook.getName() + ".afterModel", humanInTheLoopHook);
				} else {
					graph.addNode(hook.getName() + ".afterModel", modelHook::afterModel);
				}
			}
			else {
				throw new UnsupportedOperationException("Unsupported hook type: " + hook.getClass().getName());
			}
		}

		// Categorize hooks by position
		List<Hook> beforeAgentHooks = filterHooksByPosition(hooks, HookPosition.BEFORE_AGENT);
		List<Hook> afterAgentHooks = filterHooksByPosition(hooks, HookPosition.AFTER_AGENT);
		List<Hook> beforeModelHooks = filterHooksByPosition(hooks, HookPosition.BEFORE_MODEL);
		List<Hook> afterModelHooks = filterHooksByPosition(hooks, HookPosition.AFTER_MODEL);

		// Determine node flow
		String entryNode = determineEntryNode(beforeAgentHooks, beforeModelHooks);
		String loopEntryNode = determineLoopEntryNode(beforeModelHooks);
		String loopExitNode = determineLoopExitNode(afterModelHooks);
		String exitNode = determineExitNode(afterAgentHooks);

		// Set up edges
		graph.addEdge(START, entryNode);
		setupHookEdges(graph, beforeAgentHooks, afterAgentHooks, beforeModelHooks, afterModelHooks,
				entryNode, loopEntryNode, loopExitNode, exitNode, true, this);
		return graph;
	}

	/**
	 * Setup and inject tools for hooks that implement ToolInjection interface.
	 * Only the tool matching the hook's required tool name or type will be injected.
	 *
	 * @param hooks the list of hooks
	 * @param toolNode the agent tool node containing available tools
	 */
	private void setupToolsForHooks(List<Hook> hooks, AgentToolNode toolNode) {
		if (hooks == null || hooks.isEmpty() || toolNode == null) {
			return;
		}

		List<ToolCallback> availableTools = toolNode.getToolCallbacks();
		if (availableTools == null || availableTools.isEmpty()) {
			return;
		}

		for (Hook hook : hooks) {
			if (hook instanceof ToolInjection) {
				ToolInjection toolInjection = (ToolInjection) hook;
				ToolCallback toolToInject = findToolForHook(toolInjection, availableTools);
				if (toolToInject != null) {
					toolInjection.injectTool(toolToInject);
				}
			}
		}
	}

	/**
	 * Find the matching tool based on hook's requirements.
	 * Matching priority: 1) by name, 2) by type, 3) first available tool
	 *
	 * @param toolInjection the hook that needs a tool
	 * @param availableTools all available tool callbacks
	 * @return the matching tool, or null if no match found
	 */
	private ToolCallback findToolForHook(ToolInjection toolInjection, List<ToolCallback> availableTools) {
		String requiredToolName = toolInjection.getRequiredToolName();
		Class<? extends ToolCallback> requiredToolType = toolInjection.getRequiredToolType();

		// Priority 1: Match by tool name
		if (requiredToolName != null) {
			for (ToolCallback tool : availableTools) {
				String toolName = tool.getToolDefinition().name();
				if (requiredToolName.equals(toolName)) {
					return tool;
				}
			}
		}

		// Priority 2: Match by tool type
		if (requiredToolType != null) {
			for (ToolCallback tool : availableTools) {
				if (requiredToolType.isInstance(tool)) {
					return tool;
				}
			}
		}

		// Priority 3: If no specific requirement, return the first available tool
		if (requiredToolName == null && requiredToolType == null && !availableTools.isEmpty()) {
			return availableTools.get(0);
		}

		return null;
	}

	/**
	 * Filter hooks by their position based on @HookPositions annotation.
	 * A hook will be included if its getHookPositions() contains the specified position.
	 *
	 * @param hooks the list of hooks to filter
	 * @param position the position to filter by
	 * @return list of hooks that should execute at the specified position
	 */
	private static List<Hook> filterHooksByPosition(List<Hook> hooks, HookPosition position) {
		return hooks.stream()
				.filter(hook -> {
					HookPosition[] positions = hook.getHookPositions();
					return Arrays.asList(positions).contains(position);
				})
				.collect(Collectors.toList());
	}

	private static String determineEntryNode(
			List<Hook> agentHooks,
			List<Hook> modelHooks) {

		if (!agentHooks.isEmpty()) {
			return agentHooks.get(0).getName() + ".before";
		} else if (!modelHooks.isEmpty()) {
			return modelHooks.get(0).getName() + ".beforeModel";
		} else {
			return "model";
		}
	}

	private static String determineLoopEntryNode(
			List<Hook> modelHooks) {

		if (!modelHooks.isEmpty()) {
			return modelHooks.get(0).getName() + ".beforeModel";
		} else {
			return "model";
		}
	}

	private static String determineLoopExitNode(
			List<Hook> modelHooks) {

		if (!modelHooks.isEmpty()) {
			return modelHooks.get(modelHooks.size() - 1).getName() + ".afterModel";
		} else {
			return "model";
		}
	}

	private static String determineExitNode(
			List<Hook> agentHooks) {

		if (!agentHooks.isEmpty()) {
			return agentHooks.get(agentHooks.size() - 1).getName() + ".after";
		} else {
			return StateGraph.END;
		}
	}

	private static void setupHookEdges(
			StateGraph graph,
			List<Hook> beforeAgentHooks,
			List<Hook> afterAgentHooks,
			List<Hook> beforeModelHooks,
			List<Hook> afterModelHooks,
			String entryNode,
			String loopEntryNode,
			String loopExitNode,
			String exitNode,
			boolean hasTools,
			ReactAgent agentInstance) throws GraphStateException {

		// Chain before_agent hook
		chainHook(graph, beforeAgentHooks, ".before", loopEntryNode, loopEntryNode, exitNode);

		// Chain before_model hook
		chainHook(graph, beforeModelHooks, ".beforeModel", "model", loopEntryNode, exitNode);

		// Chain after_model hook (reverse order)
		chainHookReverse(graph, afterModelHooks, ".afterModel", "model", loopEntryNode, exitNode);

		// Chain after_agent hook (reverse order)
		chainHookReverse(graph, afterAgentHooks, ".after", StateGraph.END, loopEntryNode, exitNode);

		// Add tool routing if tools exist
		if (hasTools) {
			setupToolRouting(graph, loopExitNode, loopEntryNode, exitNode, agentInstance);
		} else if (!loopExitNode.equals("model")) {
			// No tools but have after_model - connect to exit
			addHookEdge(graph, loopExitNode, exitNode, loopEntryNode, exitNode, afterModelHooks.get(afterModelHooks.size() - 1).canJumpTo());
		} else {
			// No tools and no after_model - direct to exit
			graph.addEdge(loopExitNode, exitNode);
		}
	}

	private static void chainHookReverse(
			StateGraph graph,
			List<Hook> hooks,
			String nameSuffix,
			String defaultNext,
			String modelDestination,
			String endDestination) throws GraphStateException {
		if (!hooks.isEmpty()) {
			Hook last = hooks.get(hooks.size() - 1);
			addHookEdge(graph,
					defaultNext,
					last.getName() + nameSuffix,
					modelDestination, endDestination,
					last.canJumpTo());
		}

		for (int i = hooks.size() - 1; i > 0; i--) {
			Hook m1 = hooks.get(i);
			Hook m2 = hooks.get(i - 1);
			addHookEdge(graph,
					m1.getName() + nameSuffix,
					m2.getName() + nameSuffix,
					modelDestination, endDestination,
					m1.canJumpTo());
		}
	}

	private static void chainHook(
			StateGraph graph,
			List<Hook> hooks,
			String nameSuffix,
			String defaultNext,
			String modelDestination,
			String endDestination) throws GraphStateException {

		for (int i = 0; i < hooks.size() - 1; i++) {
			Hook m1 = hooks.get(i);
			Hook m2 = hooks.get(i + 1);
			addHookEdge(graph,
					m1.getName() + nameSuffix,
					m2.getName() + nameSuffix,
					modelDestination, endDestination,
					m1.canJumpTo());
		}

		if (!hooks.isEmpty()) {
			Hook last = hooks.get(hooks.size() - 1);
			addHookEdge(graph,
					last.getName() + nameSuffix,
					defaultNext,
					modelDestination, endDestination,
					last.canJumpTo());
		}
	}

	private static void addHookEdge(
			StateGraph graph,
			String name,
			String defaultDestination,
			String modelDestination,
			String endDestination,
			List<JumpTo> canJumpTo) throws GraphStateException {

		if (canJumpTo != null && !canJumpTo.isEmpty()) {
			EdgeAction router = state -> {
				JumpTo jumpTo = (JumpTo)state.value("jump_to").orElse(null);
				return resolveJump(jumpTo, modelDestination, endDestination, defaultDestination);
			};

			Map<String, String> destinations = new HashMap<>();
			destinations.put(defaultDestination, defaultDestination);

			if (canJumpTo.contains(JumpTo.end)) {
				destinations.put(endDestination, endDestination);
			}
			if (canJumpTo.contains(JumpTo.tool)) {
				destinations.put("tool", "tool");
			}
			if (canJumpTo.contains(JumpTo.model) && !name.equals(modelDestination)) {
				destinations.put(modelDestination, modelDestination);
			}

			graph.addConditionalEdges(name, edge_async(router), destinations);
		} else {
			graph.addEdge(name, defaultDestination);
		}
	}

	private static void setupToolRouting(
			StateGraph graph,
			String loopExitNode,
			String loopEntryNode,
			String exitNode,
			ReactAgent agentInstance) throws GraphStateException {

		// Model to tools routing
		graph.addConditionalEdges(loopExitNode, edge_async(agentInstance.makeModelToTools(loopEntryNode, exitNode)), Map.of("tool", "tool", exitNode, exitNode, loopEntryNode, loopEntryNode));

		// Tools to model routing
		graph.addConditionalEdges("tool", edge_async(agentInstance.makeToolsToModelEdge(loopEntryNode, exitNode)), Map.of(loopEntryNode, loopEntryNode, exitNode, exitNode));
	}

	private static String resolveJump(JumpTo jumpTo, String modelDestination, String endDestination, String defaultDestination) {
		if (jumpTo == null) {
			return defaultDestination;
		}

		return switch (jumpTo) {
			case model -> modelDestination;
			case end -> endDestination;
			case tool -> "tool";
		};
	}

	private KeyStrategyFactory buildMessagesKeyStrategyFactory() {
		return () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			keyStrategyHashMap.put("messages", new AppendStrategy());
			return keyStrategyHashMap;
		};
	}

	private EdgeAction makeModelToTools(String modelDestination, String endDestination) {
		return state -> {
			if (iterations++ > max_iterations) {
				return endDestination;
			}

			if (shouldContinueFunc != null && !shouldContinueFunc.apply(state)) {
				return endDestination;
			}

			List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());
			if (messages.isEmpty()) {
				return endDestination;
			}

			Message lastMessage = messages.get(messages.size() - 1);

			// 1. Check the last message type
			if (lastMessage instanceof AssistantMessage) {
				// 2. If last message is AssistantMessage
				AssistantMessage assistantMessage = (AssistantMessage) lastMessage;
				if (assistantMessage.hasToolCalls()) {
					return "tool";
				} else {
					return endDestination;
				}
			} else if (lastMessage instanceof ToolResponseMessage) {
				// 3. If last message is ToolResponseMessage
				if (messages.size() < 2) {
					// Should not happen in a valid ReAct loop, but as a safeguard.
					throw new RuntimeException("Less than 2 messages in state when last message is ToolResponseMessage");
				}

				Message secondLastMessage = messages.get(messages.size() - 2);
				if (secondLastMessage instanceof AssistantMessage) {
					AssistantMessage assistantMessage = (AssistantMessage) secondLastMessage;
					ToolResponseMessage toolResponseMessage = (ToolResponseMessage) lastMessage;

					if (assistantMessage.hasToolCalls()) {
						Set<String> requestedToolNames = assistantMessage.getToolCalls().stream()
								.map(toolCall -> toolCall.name())
								.collect(java.util.stream.Collectors.toSet());

						Set<String> executedToolNames = toolResponseMessage.getResponses().stream()
								.map(response -> response.name())
								.collect(java.util.stream.Collectors.toSet());

						if (executedToolNames.containsAll(requestedToolNames)) {
							return modelDestination; // All requested tools were executed or responded
						} else {
							return "tool"; // Some tools are still pending
						}
					}
				}
			}

			return endDestination;
		};
	}

	private EdgeAction makeToolsToModelEdge(String modelDestination, String endDestination) {
		return state -> {
			// 1. Extract last AI message and corresponding tool messages
			ToolResponseMessage toolResponseMessage = fetchLastToolResponseMessage(state);
			// 2. Exit condition: All executed tools have return_direct=True
			if (toolResponseMessage != null && !toolResponseMessage.getResponses().isEmpty()) {
				boolean allReturnDirect = toolResponseMessage.getResponses().stream().allMatch(toolResponse -> {
					String toolName = toolResponse.name();
					return false; // FIXME
				});
				if (allReturnDirect) {
					return endDestination;
				}
			}

			// 3. Default: Continue the loop
			//    Tool execution completed successfully, route back to the model
			//    so it can process the tool results and decide the next action.
			return modelDestination;
		};
	}

	private ToolResponseMessage fetchLastToolResponseMessage(OverAllState state) {
		List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<Message>());

		ToolResponseMessage toolResponseMessage = null;

		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i) instanceof ToolResponseMessage) {
				toolResponseMessage = (ToolResponseMessage) messages.get(i);
				break;
			}
		}

		return toolResponseMessage;
	}

	public String instruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}

	public KeyStrategy getOutputKeyStrategy() {
		return outputKeyStrategy;
	}

	public void setOutputKeyStrategy(KeyStrategy outputKeyStrategy) {
		this.outputKeyStrategy = outputKeyStrategy;
	}

	public static class SubGraphNodeAdapter implements NodeActionWithConfig {

		private boolean includeContents;

		private boolean returnReasoningContents;

		private String instruction;

		private String outputKeyToParent;

		private CompiledGraph childGraph;

		private CompileConfig parentCompileConfig;

		public SubGraphNodeAdapter(boolean includeContents, boolean returnReasoningContents, String outputKeyToParent,
				CompiledGraph childGraph, String instruction, CompileConfig parentCompileConfig) {
			this.includeContents = includeContents;
			this.returnReasoningContents = returnReasoningContents;
			this.instruction = instruction;
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
			Flux<GraphResponse<NodeOutput>> subGraphResult;
			Object parentMessages = null;

			if (includeContents) {
				// by default, includeContents is true, we pass down the messages from the parent state
				if (StringUtils.hasLength(instruction)) {
					// instruction will be added as a special UserMessage to the child graph.
					parentState.updateState(Map.of("messages", new AgentInstructionMessage(instruction)));
				}
				subGraphResult = childGraph.graphResponseStream(parentState, subGraphRunnableConfig);
			} else {
				Map<String, Object> stateForChild = new HashMap<>(parentState.data());
				parentMessages = stateForChild.remove("messages");
				if (StringUtils.hasLength(instruction)) {
					// instruction will be added as a special UserMessage to the child graph.
					stateForChild.put("messages", new AgentInstructionMessage(instruction));
				}
				subGraphResult = childGraph.graphResponseStream(stateForChild, subGraphRunnableConfig);
			}

			Map<String, Object> result = new HashMap<>();

			result.put(StringUtils.hasLength(this.outputKeyToParent) ? this.outputKeyToParent : "messages", getGraphResponseFlux(parentState, subGraphResult));
			if (parentMessages != null) {
				result.put("messages", parentMessages);
			}
			return result;
		}

		private @NotNull Flux<GraphResponse<NodeOutput>> getGraphResponseFlux(OverAllState parentState, Flux<GraphResponse<NodeOutput>> subGraphResult) {
			return Flux.create(sink -> {
				AtomicReference<GraphResponse<NodeOutput>> lastRef = new AtomicReference<>();
				subGraphResult.subscribe(item -> {
					GraphResponse<NodeOutput> previous = lastRef.getAndSet(item);
					if (previous != null) {
						sink.next(previous);
					}
				}, sink::error, () -> {
					GraphResponse<NodeOutput> lastResponse = lastRef.get();
					if (lastResponse != null) {
						if (lastResponse.resultValue().isPresent()) {
							Object resultValue = lastResponse.resultValue().get();
							if (resultValue instanceof Map) {
								@SuppressWarnings("unchecked")
								Map<String, Object> resultMap = (Map<String, Object>) resultValue;
								if (resultMap.get("messages") instanceof List) {
									@SuppressWarnings("unchecked")
									List<Object> messages = new ArrayList<>((List<Object>) resultMap.get("messages"));
									if (!messages.isEmpty()) {
										parentState.value("messages").ifPresent(parentMsgs -> {
											if (parentMsgs instanceof List) {
												messages.removeAll((List<?>) parentMsgs);
											}
										});

										List<Object> finalMessages;
										if (returnReasoningContents) {
											finalMessages = messages;
										}
										else {
											if (!messages.isEmpty()) {
												finalMessages = List.of(messages.get(messages.size() - 1));
											} else {
												finalMessages = List.of();
											}
										}

										Map<String, Object> newResultMap = new HashMap<>(resultMap);
										newResultMap.put("messages", finalMessages);
										lastResponse = GraphResponse.done(newResultMap);
									}
								}
							}
						}
					}
					sink.next(lastResponse);
					sink.complete();
				});
			});
		}

		private RunnableConfig getSubGraphRunnableConfig(RunnableConfig config) {
			RunnableConfig subGraphRunnableConfig = RunnableConfig.builder(config).checkPointId(null).nextNode(null).build();
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
	 */
	private static class AgentSubGraphNode extends Node implements SubGraphNode {

		private final CompiledGraph subGraph;

		public AgentSubGraphNode(String id, boolean includeContents, boolean returnReasoningContents, String outputKeyToParent, CompiledGraph subGraph, String instruction) {
			super(Objects.requireNonNull(id, "id cannot be null"),
					(config) -> node_async(new SubGraphNodeAdapter(includeContents, returnReasoningContents, outputKeyToParent, subGraph, instruction, config)));
			this.subGraph = subGraph;
		}

		@Override
		public StateGraph subGraph() {
			return subGraph.stateGraph;
		}
	}
}
