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
import com.alibaba.cloud.ai.graph.agent.exception.AgentException;
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
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

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
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static java.lang.String.format;


public class ReactAgent extends BaseAgent {
	Logger logger = LoggerFactory.getLogger(ReactAgent.class);

	private final AgentLlmNode llmNode;

	private final AgentToolNode toolNode;

	private List<? extends Hook> hooks;

	private List<ModelInterceptor> modelInterceptors;

	private List<ToolInterceptor> toolInterceptors;

	private String instruction;

	private StateSerializer stateSerializer;

    private final Boolean hasTools;

	public ReactAgent(AgentLlmNode llmNode, AgentToolNode toolNode, CompileConfig compileConfig, Builder builder) {
		super(builder.name, builder.description, builder.includeContents, builder.returnReasoningContents, builder.outputKey, builder.outputKeyStrategy);
		this.instruction = builder.instruction;
		this.llmNode = llmNode;
		this.toolNode = toolNode;
		this.compileConfig = compileConfig;
		this.hooks = builder.hooks;
		this.modelInterceptors = builder.modelInterceptors;
		this.toolInterceptors = builder.toolInterceptors;
		this.includeContents = builder.includeContents;
		this.inputSchema = builder.inputSchema;
		this.inputType = builder.inputType;
		this.outputSchema = builder.outputSchema;
		this.outputType = builder.outputType;

		// Set state serializer from builder, or use default
        // Default to Jackson serializer for better compatibility and features
        this.stateSerializer = Objects.requireNonNullElseGet(builder.stateSerializer, () -> new SpringAIJacksonStateSerializer(OverAllState::new));

		// Set executor configuration from builder
		this.executor = builder.executor;

		// Set interceptors to nodes
		if (this.modelInterceptors != null && !this.modelInterceptors.isEmpty()) {
			this.llmNode.setModelInterceptors(this.modelInterceptors);
		}
		if (this.toolInterceptors != null && !this.toolInterceptors.isEmpty()) {
			this.toolNode.setToolInterceptors(this.toolInterceptors);
		}

        // Set tools flag if tool interceptors are present.
        hasTools = toolNode.getToolCallbacks() != null && !toolNode.getToolCallbacks().isEmpty();
	}

	public static Builder builder() {
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
			// Note: outputKey="messages" is prohibited by validation in DefaultBuilder
			// This branch only handles custom output keys
			return state.flatMap(s -> s.value(outputKey))
					.map(msg -> (AssistantMessage) msg)
					.orElseThrow(() -> new IllegalStateException("Output key " + outputKey + " not found in agent state") );
		}

        // Default behavior: use "messages" key and extract the last AssistantMessage
        return state.flatMap(s -> s.value("messages"))
                .stream()
                .flatMap(messageList -> ((List<?>) messageList).stream()
                        .filter(msg -> msg instanceof AssistantMessage)
                        .map(msg -> (AssistantMessage) msg))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AgentException("No AssistantMessage found in 'messages' state"));
	}

	public StateGraph getStateGraph() {
		return getGraph();
	}

	public CompiledGraph getCompiledGraph() {
		return compiledGraph;
	}

	@Override
	public Node asNode(boolean includeContents, boolean returnReasoningContents) {
		if (this.compiledGraph == null) {
			this.compiledGraph = getAndCompileGraph();
		}
		return new AgentSubGraphNode(this.name, includeContents, returnReasoningContents, this.compiledGraph, this.instruction);
	}

	@Override
	protected StateGraph initGraph() throws GraphStateException {

		if (hooks == null) {
			hooks = new ArrayList<>();
		}

		// Validate hook uniqueness
		Set<String> hookNames = new HashSet<>();
		for (Hook hook : hooks) {
			if (!hookNames.add(hook.getName())) {
				throw new IllegalArgumentException("Duplicate hook instances found");
			}

			// set agent name to every hook node.
			hook.setAgentName(this.name);
		}

		// Create graph with state serializer
		StateGraph graph = new StateGraph(name, buildMessagesKeyStrategyFactory(hooks), stateSerializer);

		graph.addNode("model", node_async(this.llmNode));
		graph.addNode("tool", node_async(this.toolNode));

		// some hooks may need tools so they can do some initialization/cleanup on start/end of agent loop
		setupToolsForHooks(hooks, toolNode);

		// Categorize hooks by position
		List<Hook> beforeAgentHooks = filterHooksByPosition(hooks, HookPosition.BEFORE_AGENT);
		List<Hook> afterAgentHooks = filterHooksByPosition(hooks, HookPosition.AFTER_AGENT);
		List<Hook> beforeModelHooks = filterHooksByPosition(hooks, HookPosition.BEFORE_MODEL);
		List<Hook> afterModelHooks = filterHooksByPosition(hooks, HookPosition.AFTER_MODEL);

		// Add hook nodes for beforeAgent hooks
		for (Hook hook : beforeAgentHooks) {
			if (hook instanceof AgentHook agentHook) {
				graph.addNode(hook.getName() + ".before", agentHook::beforeAgent);
			}
		}

		// Add hook nodes for afterAgent hooks
		for (Hook hook : afterAgentHooks) {
			if (hook instanceof AgentHook agentHook) {
				graph.addNode(hook.getName() + ".after", agentHook::afterAgent);
			}
		}

		// Add hook nodes for beforeModel hooks
		for (Hook hook : beforeModelHooks) {
			if (hook instanceof ModelHook modelHook) {
				graph.addNode(hook.getName() + ".beforeModel", modelHook::beforeModel);
			}
		}

		// Add hook nodes for afterModel hooks
		for (Hook hook : afterModelHooks) {
			if (hook instanceof ModelHook modelHook) {
				if (hook instanceof HumanInTheLoopHook humanInTheLoopHook) {
					graph.addNode(hook.getName() + ".afterModel", humanInTheLoopHook);
				} else {
					graph.addNode(hook.getName() + ".afterModel", modelHook::afterModel);
				}
			}
		}

		// Determine node flow
		String entryNode = determineEntryNode(beforeAgentHooks, beforeModelHooks);
		String loopEntryNode = determineLoopEntryNode(beforeModelHooks);
		String loopExitNode = determineLoopExitNode(afterModelHooks);
		String exitNode = determineExitNode(afterAgentHooks);

		// Set up edges
		graph.addEdge(START, entryNode);
		setupHookEdges(graph, beforeAgentHooks, afterAgentHooks, beforeModelHooks, afterModelHooks,
				entryNode, loopEntryNode, loopExitNode, exitNode, this);
		return graph;
	}

	/**
	 * Setup and inject tools for hooks that implement ToolInjection interface.
	 * Only the tool matching the hook's required tool name or type will be injected.
	 *
	 * @param hooks the list of hooks
	 * @param toolNode the agent tool node containing available tools
	 */
	private void setupToolsForHooks(List<? extends Hook> hooks, AgentToolNode toolNode) {
		if (hooks == null || hooks.isEmpty() || toolNode == null) {
			return;
		}

		List<ToolCallback> availableTools = toolNode.getToolCallbacks();
		if (availableTools == null || availableTools.isEmpty()) {
			return;
		}

		for (Hook hook : hooks) {
			if (hook instanceof ToolInjection toolInjection) {
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
	private static List<Hook> filterHooksByPosition(List<? extends Hook> hooks, HookPosition position) {
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
			return modelHooks.get(0).getName() + ".afterModel";
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
			ReactAgent agentInstance) throws GraphStateException {

		// Chain before_agent hook
		chainHook(graph, beforeAgentHooks, ".before", loopEntryNode, loopEntryNode, exitNode);

		// Chain before_model hook
		chainHook(graph, beforeModelHooks, ".beforeModel", "model", loopEntryNode, exitNode);

		// Chain after_model hook (reverse order)
		if (!afterModelHooks.isEmpty()) {
			chainModelHookReverse(graph, afterModelHooks, ".afterModel", "model", loopEntryNode, exitNode);
		}

		// Chain after_agent hook (reverse order)
		if (!afterAgentHooks.isEmpty()) {
			chainAgentHookReverse(graph, afterAgentHooks, ".after", exitNode, loopEntryNode, exitNode);
		}

		// Add tool routing if tools exist
		if (agentInstance.hasTools) {
			setupToolRouting(graph, loopExitNode, loopEntryNode, exitNode, agentInstance);
		} else if (!loopExitNode.equals("model")) {
			// No tools but have after_model - connect to exit
			addHookEdge(graph, loopExitNode, exitNode, loopEntryNode, exitNode, afterModelHooks.get(afterModelHooks.size() - 1).canJumpTo());
		} else {
			// No tools and no after_model - direct to exit
			graph.addEdge(loopExitNode, exitNode);
		}
	}

	private static void chainModelHookReverse(
			StateGraph graph,
			List<Hook> hooks,
			String nameSuffix,
			String defaultNext,
			String modelDestination,
			String endDestination) throws GraphStateException {

		graph.addEdge(defaultNext, hooks.get(hooks.size() - 1).getName() + nameSuffix);

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

	private static void chainAgentHookReverse(
			StateGraph graph,
			List<Hook> hooks,
			String nameSuffix,
			String defaultNext,
			String modelDestination,
			String endDestination) throws GraphStateException {
		if (!hooks.isEmpty()) {
			Hook first = hooks.get(0);
			addHookEdge(graph,
					first.getName() + nameSuffix,
					StateGraph.END,
					modelDestination, endDestination,
					first.canJumpTo());
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

	private KeyStrategyFactory buildMessagesKeyStrategyFactory(List<? extends Hook> hooks) {
		return () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			if (outputKey != null && !outputKey.isEmpty()) {
				// Note: outputKey="messages" is prohibited by validation in DefaultBuilder
				// This branch only handles custom output keys
				keyStrategyHashMap.put(outputKey, outputKeyStrategy == null ? new ReplaceStrategy() : outputKeyStrategy);
			}
			// Always ensure "messages" uses AppendStrategy for conversation history
			keyStrategyHashMap.put("messages", new AppendStrategy());

			// Iterate through hooks and collect their key strategies
			if (hooks != null) {
				for (Hook hook : hooks) {
					Map<String, KeyStrategy> hookStrategies = hook.getKeyStrategys();
					if (hookStrategies != null && !hookStrategies.isEmpty()) {
						keyStrategyHashMap.putAll(hookStrategies);
					}
				}
			}

			return keyStrategyHashMap;
		};
	}

	private EdgeAction makeModelToTools(String modelDestination, String endDestination) {
		return state -> {
			List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
			if (messages.isEmpty()) {
				logger.warn("No messages found in state when routing from model to tools");
				return endDestination;
			}
			Message lastMessage = messages.get(messages.size() - 1);

			// 1. Check the last message type
			if (lastMessage instanceof AssistantMessage assistantMessage) {
				// 2. If last message is AssistantMessage
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
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());

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
		llmNode.setInstruction(instruction);
	}

	public class SubGraphNodeAdapter implements NodeActionWithConfig {

		private boolean includeContents;

		private boolean returnReasoningContents;

		private String instruction;

		private CompiledGraph childGraph;

		private CompileConfig parentCompileConfig;

		public SubGraphNodeAdapter(boolean includeContents, boolean returnReasoningContents,
				CompiledGraph childGraph, String instruction, CompileConfig parentCompileConfig) {
			this.includeContents = includeContents;
			this.returnReasoningContents = returnReasoningContents;
			this.instruction = instruction;
			this.childGraph = childGraph;
			this.parentCompileConfig = parentCompileConfig;
		}

		public String subGraphId() {
			return format("subgraph_%s", childGraph.stateGraph.getName());
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState, RunnableConfig config) throws Exception {
			RunnableConfig subGraphRunnableConfig = getSubGraphRunnableConfig(config);

			// Extract parent messages from parent state
			List<Message> parentMessages = extractMessagesFromState(parentState);

			// Prepare messages for child graph based on includeContents flag
			List<Object> messagesForChild = prepareChildMessages(parentMessages);

			// Create child state with prepared messages
			// IMPORTANT: Use ReplaceStrategy to avoid appending messagesForChild to existing messages in snapshot
			// snapShot() creates a shallow copy that includes existing messages from parentState
			// Without ReplaceStrategy, updateState() would use AppendStrategy and cause message duplication
			OverAllState childState = parentState.snapShot()
					.orElseThrow(() -> new IllegalStateException("Failed to create state snapshot"));
			childState.updateStateWithKeyStrategies(
					Map.of("messages", messagesForChild),
					Map.of("messages", new ReplaceStrategy()));

			// Execute child graph
			Flux<GraphResponse<NodeOutput>> subGraphResult =
					childGraph.graphResponseStream(childState, subGraphRunnableConfig);

			// Determine output key (default is "messages")
			String outputKeyToParent = StringUtils.hasLength(ReactAgent.this.outputKey)
					? ReactAgent.this.outputKey : "messages";

			// Build result map
			Map<String, Object> result = new HashMap<>();
			result.put(outputKeyToParent,
					processGraphResult(subGraphResult, parentMessages, outputKeyToParent));

			// When includeContents=false and outputKey is custom (not "messages"),
			// we need to preserve parent messages in "messages" key
			if (!includeContents && !"messages".equals(outputKeyToParent) && !parentMessages.isEmpty()) {
				result.put("messages", parentMessages);
			}

			return result;
		}

		/**
		 * Extract messages from state, ensuring type safety.
		 * Returns an empty list if no messages found or if messages are not of correct type.
		 *
		 * @param state the state to extract messages from
		 * @return list of Message objects
		 */
		private List<Message> extractMessagesFromState(OverAllState state) {
			Object messagesObj = state.value("messages").orElse(null);
			if (messagesObj == null) {
				return new ArrayList<>();
			}

			if (messagesObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> messagesList = (List<Object>) messagesObj;
				List<Message> result = new ArrayList<>();
				for (Object obj : messagesList) {
					if (obj instanceof Message) {
						result.add((Message) obj);
					} else {
						logger.warn("Found non-Message object in messages list: {}. " +
								"This object will be excluded from processing. Type: {}",
								obj, obj != null ? obj.getClass().getName() : "null");
					}
				}
				return result;
			} else if (messagesObj instanceof Message) {
				return List.of((Message) messagesObj);
			} else {
				logger.warn("Unexpected type for messages: {}. Expected List<Message> or Message. " +
						"Treating as empty messages list.", messagesObj.getClass().getName());
				return new ArrayList<>();
			}
		}

	/**
	 * Prepare messages for child graph based on includeContents flag.
	 *
	 * @param parentMessages the parent messages
	 * @return list of messages for child graph
	 */
	private List<Object> prepareChildMessages(List<Message> parentMessages) {
		List<Object> messagesForChild = new ArrayList<>();

		if (includeContents) {
			// Include parent messages, but mark them so they can be identified later
			for (Message msg : parentMessages) {
				messagesForChild.add(MessageMarker.mark(msg));
			}
		}
		// If not includeContents, start with empty list (child is isolated)

		// Add instruction if present
		// IMPORTANT: Mark instruction to prevent it from accumulating in parent state
		// Instruction is a temporary directive for child graph, not part of conversation history
		if (StringUtils.hasLength(instruction)) {
			messagesForChild.add(MessageMarker.mark(new AgentInstructionMessage(instruction)));
		}

		return messagesForChild;
	}

		/**
		 * Process the graph result stream, handling message filtering and merging.
		 *
		 * @param subGraphResult the result stream from child graph
		 * @param parentMessages the parent messages (for merging when needed)
		 * @param outputKey the output key for results
		 * @return processed graph response flux
		 */
		private Flux<GraphResponse<NodeOutput>> processGraphResult(
				Flux<GraphResponse<NodeOutput>> subGraphResult,
				List<Message> parentMessages,
				String outputKey) {

		return Flux.create(sink -> {
			AtomicReference<GraphResponse<NodeOutput>> lastRef = new AtomicReference<>();

			// Note: This code assumes subGraphResult emits elements sequentially on a single thread,
			// which is the default behavior of CompiledGraph.graphResponseStream().
			// If the upstream Flux uses multi-threaded scheduling (e.g., publishOn with parallel scheduler),
			// additional synchronization would be needed to ensure thread safety.
			subGraphResult.subscribe(
				// onNext: buffer all intermediate results
				// Note: Intermediate responses are streaming chunks (e.g., LLM tokens)
				// and don't contain complete message state, so we pass them through as-is
				item -> {
					GraphResponse<NodeOutput> previous = lastRef.getAndSet(item);
					if (previous != null) {
						sink.next(previous);
					}
				},
				// onError: propagate error
				sink::error,
				// onComplete: process final result
				// Only the final response contains complete message state and needs processing
				() -> {
					GraphResponse<NodeOutput> lastResponse = lastRef.get();
					if (lastResponse != null) {
						// Process the final response
						lastResponse = processFinalResponse(lastResponse, parentMessages, outputKey);
						sink.next(lastResponse);
					} else {
						// Handle empty Flux case: child graph produced no output
						// Create an empty response to avoid NPE downstream
						logger.warn("SubGraph '{}' produced no output, creating empty response with empty messages",
								ReactAgent.this.name);
						sink.next(GraphResponse.done(Map.of("messages", List.of())));
					}
					sink.complete();
				}
			);
		});
		}

		/**
		 * Process the final graph response, handling message filtering and merging.
		 *
		 * @param response the final graph response
		 * @param parentMessages the parent messages
		 * @param outputKey the output key
		 * @return processed graph response
		 */
		private GraphResponse<NodeOutput> processFinalResponse(
				GraphResponse<NodeOutput> response,
				List<Message> parentMessages,
				String outputKey) {

			// Extract result map from response
			Optional<Object> resultValueOpt = response.resultValue();
			if (resultValueOpt.isEmpty() || !(resultValueOpt.get() instanceof Map)) {
				return response;
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> resultMap = (Map<String, Object>) resultValueOpt.get();

		// Extract messages from result
		List<Message> childMessages = extractMessagesFromMap(resultMap);

		// Process messages based on outputKey
		if (!"messages".equals(outputKey)) {
			// Output goes to custom key, need to handle "messages" properly
			if (includeContents) {
				// Remove marked parent messages from "messages" to avoid duplication
				List<Message> cleanedMessages = removeMarkedParentMessages(childMessages);
				Map<String, Object> newResultMap = new HashMap<>(resultMap);
				newResultMap.put("messages", cleanedMessages);
				return GraphResponse.done(newResultMap);
			} else {
				// For includeContents=false, child is isolated from parent
				// Remove "messages" key to prevent child's internal messages from leaking to parent state
				// Parent messages are preserved via the "messages" key in apply()'s return map
				Map<String, Object> newResultMap = new HashMap<>(resultMap);
				newResultMap.remove("messages");
				return GraphResponse.done(newResultMap);
			}
		}

		// Output goes to "messages" key - apply full processing
		List<Message> processedMessages = processMessagesForOutput(
				childMessages, parentMessages);

		// Apply returnReasoningContents logic
		List<Message> finalMessages;
		if (returnReasoningContents) {
			finalMessages = processedMessages;
		} else {
			// Only return last message when returnReasoningContents=false
			finalMessages = processedMessages.isEmpty()
					? List.of()
					: List.of(processedMessages.get(processedMessages.size() - 1));
		}

		// Update result map
		Map<String, Object> newResultMap = new HashMap<>(resultMap);
		newResultMap.put("messages", finalMessages);
		return GraphResponse.done(newResultMap);
	}

		/**
		 * Extract messages from result map with type safety.
		 *
		 * @param resultMap the result map
		 * @return list of Message objects
		 */
		private List<Message> extractMessagesFromMap(Map<String, Object> resultMap) {
			Object messagesObj = resultMap.get("messages");
			if (messagesObj == null) {
				return new ArrayList<>();
			}

			if (messagesObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> messagesList = (List<Object>) messagesObj;
				List<Message> result = new ArrayList<>();
				for (Object obj : messagesList) {
					if (obj instanceof Message) {
						result.add((Message) obj);
					} else {
						logger.warn("Found non-Message object in result map messages: {}. " +
								"This object will be excluded from processing. Type: {}",
								obj, obj != null ? obj.getClass().getName() : "null");
					}
				}
				return result;
			}

			return new ArrayList<>();
		}

		/**
		 * Remove messages that are marked as parent messages.
		 *
		 * @param messages the messages to filter
		 * @return filtered list with marked parent messages removed
		 */
		private List<Message> removeMarkedParentMessages(List<Message> messages) {
			return messages.stream()
					.filter(msg -> !MessageMarker.isMarked(msg))
					.collect(Collectors.toList());
		}

	/**
	 * Process messages for output by removing all marked messages.
	 * Returns only new messages produced by child graph.
	 *
	 * @param childMessages messages from child graph
	 * @param parentMessages parent messages (not used, kept for compatibility)
	 * @return processed messages with all marked messages removed
	 */
	private List<Message> processMessagesForOutput(
			List<Message> childMessages,
			List<Message> parentMessages) {

		// Always remove marked messages (parent messages and instruction)
		// This applies to both includeContents=true and includeContents=false scenarios
		//
		// When includeContents=true:
		//   - childMessages contains: [marked(parent1), marked(parent2), marked(instruction), new_msg]
		//   - After removal: [new_msg]
		//
		// When includeContents=false:
		//   - childMessages contains: [marked(instruction), new_msg]
		//   - After removal: [new_msg]
		//
		// In both cases, only new messages are returned.
		// The framework will automatically merge with parent messages using AppendStrategy.
		return removeMarkedParentMessages(childMessages);
	}


		private RunnableConfig getSubGraphRunnableConfig(RunnableConfig config) {
			RunnableConfig subGraphRunnableConfig = RunnableConfig.builder(config)
					.checkPointId(null)
					.clearContext()
					.nextNode(null)
					.addMetadata("_AGENT_", subGraphId()) // subGraphId is the same as the name of the agent that created it
					.build();
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
							.clearContext()
							.addMetadata("_AGENT_", subGraphId()) // subGraphId is the same as the name of the agent that created it
							.build();
				}
			}
			return subGraphRunnableConfig;
		}

	}

	/**
	 * Utility class for marking and unmarking parent messages using metadata.
	 * This class provides a centralized and type-safe way to handle message markers,
	 * eliminating code duplication and improving maintainability.
	 */
	private static class MessageMarker {
		private static final Logger logger = LoggerFactory.getLogger(MessageMarker.class);
		private static final String PARENT_MESSAGE_MARKER = "_parent_message_marker_";

		/**
		 * Mark a message as a parent message by adding metadata marker.
		 * Creates a new message instance with the marker to avoid modifying the original.
		 *
		 * @param message the message to mark (must not be null)
		 * @return a new message instance with parent marker in metadata
		 */
		static Message mark(Message message) {
			// Create new metadata map with parent marker
			Map<String, Object> metadata = new HashMap<>(
				message.getMetadata() != null ? message.getMetadata() : Map.of()
			);
			metadata.put(PARENT_MESSAGE_MARKER, true);

			// Rebuild message with updated metadata using type-specific builders
			return rebuildMessageWithMetadata(message, metadata);
		}

		/**
		 * Check if a message is marked as a parent message.
		 *
		 * @param message the message to check
		 * @return true if the message has the parent marker in metadata
		 */
		static boolean isMarked(Message message) {
			Map<String, Object> metadata = message.getMetadata();
			return metadata != null && Boolean.TRUE.equals(metadata.get(PARENT_MESSAGE_MARKER));
		}

		/**
		 * Remove parent message marker from a message.
		 * Creates a new message instance without the marker.
		 *
		 * @param message the message to unmark (must not be null)
		 * @return a new message instance without the parent marker
		 */
		static Message unmark(Message message) {
			// Create new metadata map without the parent marker
			Map<String, Object> metadata = new HashMap<>(
				message.getMetadata() != null ? message.getMetadata() : Map.of()
			);
			metadata.remove(PARENT_MESSAGE_MARKER);

			// Rebuild message with updated metadata
			return rebuildMessageWithMetadata(message, metadata);
		}

		/**
		 * Rebuild a message with new metadata, preserving all other properties.
		 * Handles all known message types with appropriate builders.
		 *
		 * @param message the original message
		 * @param metadata the new metadata to use
		 * @return a new message instance with updated metadata
		 */
		private static Message rebuildMessageWithMetadata(Message message, Map<String, Object> metadata) {
			if (message instanceof AssistantMessage) {
				AssistantMessage am = (AssistantMessage) message;
				return AssistantMessage.builder()
						.content(am.getText())
						.properties(metadata)
						.toolCalls(am.getToolCalls())
						.media(am.getMedia())
						.build();
			} else if (message instanceof UserMessage) {
				UserMessage um = (UserMessage) message;
				return UserMessage.builder()
						.text(um.getText())
						.metadata(metadata)
						.media(um.getMedia())
						.build();
			} else if (message instanceof SystemMessage) {
				SystemMessage sm = (SystemMessage) message;
				return SystemMessage.builder()
						.text(sm.getText())
						.metadata(metadata)
						.build();
			} else if (message instanceof ToolResponseMessage) {
				ToolResponseMessage trm = (ToolResponseMessage) message;
				return ToolResponseMessage.builder()
						.responses(trm.getResponses())
						.metadata(metadata)
						.build();
			} else if (message instanceof AgentInstructionMessage) {
				AgentInstructionMessage aim = (AgentInstructionMessage) message;
				return AgentInstructionMessage.builder()
						.text(aim.getText())
						.metadata(metadata)
						.build();
			} else {
				// For unknown message types, return the original message with a warning
				logger.warn("Unable to rebuild message of type {} with updated metadata. " +
						"Marker mechanism may not work correctly for this message type.",
						message.getClass().getName());
				return message;
			}
		}
	}

	/**
	 * Internal class that adapts a ReactAgent to be used as a SubGraph Node.
	 */
	private class AgentSubGraphNode extends Node implements SubGraphNode {

		private final CompiledGraph subGraph;

		public AgentSubGraphNode(String id, boolean includeContents, boolean returnReasoningContents, CompiledGraph subGraph, String instruction) {
			super(Objects.requireNonNull(id, "id cannot be null"),
					(config) -> node_async(new SubGraphNodeAdapter(includeContents, returnReasoningContents, subGraph, instruction, config)));
			this.subGraph = subGraph;
		}

	@Override
	public StateGraph subGraph() {
		return subGraph.stateGraph;
	}

	@Override
	public Map<String, KeyStrategy> keyStrategies() {
		return subGraph.getKeyStrategyMap();
	}
	}
}
