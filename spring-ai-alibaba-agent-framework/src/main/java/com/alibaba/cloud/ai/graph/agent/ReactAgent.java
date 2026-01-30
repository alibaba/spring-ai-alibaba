/*
 * Copyright 2024-2026 the original author or authors.
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
import com.alibaba.cloud.ai.graph.agent.hook.InterruptionHook;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
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
import com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_MODEL_NAME;
import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_TOOL_NAME;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static com.alibaba.cloud.ai.graph.agent.hook.InterruptionHook.INTERRUPTION_FEEDBACK_KEY;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.resumeSubGraphId;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.subGraphId;
import static java.lang.String.format;


public class ReactAgent extends BaseAgent {
	Logger logger = LoggerFactory.getLogger(ReactAgent.class);

	private final ConcurrentMap<String, Map<String, Object>> threadIdStateMap;

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
		this.threadIdStateMap = new ConcurrentHashMap<>();

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
		// Collect interceptors from hooks and merge with current interceptors
		List<ModelInterceptor> mergedModelInterceptors = collectAndMergeModelInterceptors();
		List<ToolInterceptor> mergedToolInterceptors = collectAndMergeToolInterceptors();

		if (mergedModelInterceptors != null && !mergedModelInterceptors.isEmpty()) {
			this.llmNode.setModelInterceptors(mergedModelInterceptors);
		}
		if (mergedToolInterceptors != null && !mergedToolInterceptors.isEmpty()) {
			this.toolNode.setToolInterceptors(mergedToolInterceptors);
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

	public void interrupt(RunnableConfig config) {
		updateAgentState(List.of(), config);
	}

	public void interrupt(List<Message> messages, RunnableConfig config) {
		updateAgentState(messages, config);
	}

	public void interrupt(String userMessage, RunnableConfig config) {
		updateAgentState(List.of(UserMessage.builder().text(userMessage).build()), config);
	}

	/**
	 * Updates the agent thread state with interruption feedback.
	 * This method is thread-safe and can be called concurrently with apply() in InterruptionHook.
	 * 
	 * Thread-safety guarantees:
	 * - threadIdStateMap is a ConcurrentHashMap, ensuring thread-safe access
	 * - computeIfAbsent ensures atomic creation of the inner map if it doesn't exist
	 * - The inner map is always a ConcurrentHashMap, ensuring thread-safe put() operations
	 * 
	 * Concurrency behavior:
	 * - If called before apply() processes feedback: the new value will be processed
	 * - If called after apply() removes feedback: the new value will be set for next iteration
	 * - If called concurrently with apply(): the atomic operations ensure no data loss
	 */
	public void updateAgentState(Object state, RunnableConfig config) {
		String threadId = config.threadId().orElseThrow(() -> new IllegalArgumentException("threadId must be provided in RunnableConfig for interruption."));
		// computeIfAbsent is atomic - ensures thread-safe creation of inner map
		// The inner map is always ConcurrentHashMap, ensuring thread-safe put() operations
		Map<String, Object> stateStatus = threadIdStateMap.computeIfAbsent(threadId, k -> new ConcurrentHashMap<>());
		stateStatus.put(INTERRUPTION_FEEDBACK_KEY, state);
	}

	private AssistantMessage doMessageInvoke(Object message, RunnableConfig config) throws GraphRunnerException {
		Map<String, Object> inputs= buildMessageInput(message);
		Optional<OverAllState> state = doInvoke(inputs, config);

		if (StringUtils.hasLength(outputKey)) {
			return state.flatMap(s -> s.value(outputKey))
					.map(msg -> (AssistantMessage) msg)
					.orElseThrow(() -> new IllegalStateException("Output key " + outputKey + " not found in agent state") );
		}

        // Add a validation instance when performing message conversion to
        // avoid potential type conversion exceptions.
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
			if (!hookNames.add(Hook.getFullHookName(hook))) {
				throw new IllegalArgumentException("Duplicate hook instances found");
			}

			// set agent name to every hook node.
			hook.setAgentName(this.name);
			hook.setAgent(this);
		}

		// Create graph with state serializer
		StateGraph graph = new StateGraph(name, buildMessagesKeyStrategyFactory(hooks), stateSerializer);

		graph.addNode(AGENT_MODEL_NAME, node_async(this.llmNode));
		if (hasTools) {
			graph.addNode(AGENT_TOOL_NAME, node_async(this.toolNode));
		}

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
				graph.addNode(Hook.getFullHookName(hook) + ".before", agentHook::beforeAgent);
			} else if (hook instanceof MessagesAgentHook messagesAgentHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".before", MessagesAgentHook.beforeAgentAction(messagesAgentHook));
			}
		}

		// Add hook nodes for afterAgent hooks
		for (Hook hook : afterAgentHooks) {
			if (hook instanceof AgentHook agentHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".after", agentHook::afterAgent);
			} else if (hook instanceof MessagesAgentHook messagesAgentHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".after", MessagesAgentHook.afterAgentAction(messagesAgentHook));
			}
		}

		// Add hook nodes for beforeModel hooks
		for (Hook hook : beforeModelHooks) {
			if (hook instanceof ModelHook modelHook) {
				if (hook instanceof InterruptionHook interruptionHook) {
					graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", interruptionHook);
				} else {
					graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", modelHook::beforeModel);
				}
			} else if (hook instanceof MessagesModelHook messagesModelHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".beforeModel", MessagesModelHook.beforeModelAction(messagesModelHook));
			}
		}

		// Add hook nodes for afterModel hooks
		for (Hook hook : afterModelHooks) {
			if (hook instanceof ModelHook modelHook) {
				if (hook instanceof HumanInTheLoopHook humanInTheLoopHook) {
					graph.addNode(Hook.getFullHookName(hook) + ".afterModel", humanInTheLoopHook);
				} else {
					graph.addNode(Hook.getFullHookName(hook) + ".afterModel", modelHook::afterModel);
				}
			} else if (hook instanceof MessagesModelHook messagesModelHook) {
				graph.addNode(Hook.getFullHookName(hook) + ".afterModel", MessagesModelHook.afterModelAction(messagesModelHook));
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
	 * If a hook implements Prioritized interface, it will be sorted by its order.
	 * Hooks that don't implement Prioritized will maintain their original order.
	 *
	 * @param hooks the list of hooks to filter
	 * @param position the position to filter by
	 * @return list of hooks that should execute at the specified position
	 */
	private static List<Hook> filterHooksByPosition(List<? extends Hook> hooks, HookPosition position) {
		List<Hook> filtered = hooks.stream()
				.filter(hook -> {
					HookPosition[] positions = hook.getHookPositions();
					return Arrays.asList(positions).contains(position);
				})
				.collect(Collectors.toList());
		
		// Separate hooks that implement Prioritized from those that don't
		List<Hook> prioritizedHooks = new ArrayList<>();
		List<Hook> nonPrioritizedHooks = new ArrayList<>();
		
		for (Hook hook : filtered) {
			if (hook instanceof Prioritized) {
				prioritizedHooks.add(hook);
			} else {
				nonPrioritizedHooks.add(hook);
			}
		}
		
		// Sort prioritized hooks by their order
		prioritizedHooks.sort(Comparator.comparingInt(h -> ((Prioritized) h).getOrder()));
		
		// Combine: prioritized hooks first (sorted), then non-prioritized hooks (original order)
		List<Hook> result = new ArrayList<>(prioritizedHooks);
		result.addAll(nonPrioritizedHooks);
		
		return result;
	}

	private static String determineEntryNode(
			List<Hook> agentHooks,
			List<Hook> modelHooks) {

		if (!agentHooks.isEmpty()) {
			return Hook.getFullHookName(agentHooks.get(0)) + ".before";
		} else if (!modelHooks.isEmpty()) {
			return Hook.getFullHookName(modelHooks.get(0)) + ".beforeModel";
		} else {
			return AGENT_MODEL_NAME;
		}
	}

	private static String determineLoopEntryNode(
			List<Hook> modelHooks) {

		if (!modelHooks.isEmpty()) {
			return Hook.getFullHookName(modelHooks.get(0)) + ".beforeModel";
		} else {
			return AGENT_MODEL_NAME;
		}
	}

	private static String determineLoopExitNode(
			List<Hook> modelHooks) {

		if (!modelHooks.isEmpty()) {
			return Hook.getFullHookName(modelHooks.get(0)) + ".afterModel";
		} else {
			return AGENT_MODEL_NAME;
		}
	}

	private static String determineExitNode(
			List<Hook> agentHooks) {

		if (!agentHooks.isEmpty()) {
			return Hook.getFullHookName(agentHooks.get(agentHooks.size() - 1)) + ".after";
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
		chainHook(graph, beforeModelHooks, ".beforeModel", AGENT_MODEL_NAME, loopEntryNode, exitNode);

		// Chain after_model hook (reverse order)
		if (!afterModelHooks.isEmpty()) {
			chainModelHookReverse(graph, afterModelHooks, ".afterModel", AGENT_MODEL_NAME, loopEntryNode, exitNode);
		}

		// Chain after_agent hook (reverse order)
		if (!afterAgentHooks.isEmpty()) {
			chainAgentHookReverse(graph, afterAgentHooks, ".after", exitNode, loopEntryNode, exitNode);
		}

		// Add tool routing if tools exist
		if (agentInstance.hasTools) {
			setupToolRouting(graph, loopExitNode, loopEntryNode, exitNode, agentInstance);
		} else if (!loopExitNode.equals(AGENT_MODEL_NAME)) {
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

		graph.addEdge(defaultNext, Hook.getFullHookName(hooks.get(hooks.size() - 1)) + nameSuffix);

		for (int i = hooks.size() - 1; i > 0; i--) {
			Hook m1 = hooks.get(i);
			Hook m2 = hooks.get(i - 1);
			addHookEdge(graph,
					Hook.getFullHookName(m1) + nameSuffix,
					Hook.getFullHookName(m2) + nameSuffix,
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
					Hook.getFullHookName(first) + nameSuffix,
					StateGraph.END,
					modelDestination, endDestination,
					first.canJumpTo());
		}

		for (int i = hooks.size() - 1; i > 0; i--) {
			Hook m1 = hooks.get(i);
			Hook m2 = hooks.get(i - 1);
			addHookEdge(graph,
					Hook.getFullHookName(m1) + nameSuffix,
					Hook.getFullHookName(m2) + nameSuffix,
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
					Hook.getFullHookName(m1) + nameSuffix,
					Hook.getFullHookName(m2) + nameSuffix,
					modelDestination, endDestination,
					m1.canJumpTo());
		}

		if (!hooks.isEmpty()) {
			Hook last = hooks.get(hooks.size() - 1);
			addHookEdge(graph,
					Hook.getFullHookName(last) + nameSuffix,
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
				Object jumpToValue = state.value("jump_to").orElse(null);
				JumpTo jumpTo = null;
				if (jumpToValue != null) {
					if (jumpToValue instanceof JumpTo) {
						jumpTo = (JumpTo) jumpToValue;
					} else if (jumpToValue instanceof String) {
						jumpTo = JumpTo.fromStringOrNull((String) jumpToValue);
					}
				}
				return resolveJump(jumpTo, modelDestination, endDestination, defaultDestination);
			};

			Map<String, String> destinations = new HashMap<>();
			destinations.put(defaultDestination, defaultDestination);

			if (canJumpTo.contains(JumpTo.end)) {
				destinations.put(endDestination, endDestination);
			}
			if (canJumpTo.contains(JumpTo.tool)) {
				destinations.put(AGENT_TOOL_NAME, AGENT_TOOL_NAME);
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
		graph.addConditionalEdges(loopExitNode, edge_async(agentInstance.makeModelToTools(loopEntryNode, exitNode)), Map.of(AGENT_TOOL_NAME, AGENT_TOOL_NAME, exitNode, exitNode, loopEntryNode, loopEntryNode));

		// Tools to model routing
		graph.addConditionalEdges(AGENT_TOOL_NAME, edge_async(agentInstance.makeToolsToModelEdge(loopEntryNode, exitNode)), Map.of(loopEntryNode, loopEntryNode, exitNode, exitNode));
	}

	private static String resolveJump(JumpTo jumpTo, String modelDestination, String endDestination, String defaultDestination) {
		if (jumpTo == null) {
			return defaultDestination;
		}

		return switch (jumpTo) {
			case model -> modelDestination;
			case end -> endDestination;
			case tool -> AGENT_TOOL_NAME;
		};
	}

	private KeyStrategyFactory buildMessagesKeyStrategyFactory(List<? extends Hook> hooks) {
		return () -> {
			HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
			if (outputKey != null && !outputKey.isEmpty()) {
				keyStrategyHashMap.put(outputKey, outputKeyStrategy == null ? new ReplaceStrategy() : outputKeyStrategy);
			}
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
			// Priority 1: Check for jump_to instruction from hooks
			// This allows afterModel hooks to control workflow execution
			Object jumpToValue = state.value("jump_to").orElse(null);
			if (jumpToValue != null) {
				JumpTo jumpTo = null;
				if (jumpToValue instanceof JumpTo) {
					jumpTo = (JumpTo) jumpToValue;
				} else if (jumpToValue instanceof String) {
					jumpTo = JumpTo.fromStringOrNull((String) jumpToValue);
				}
				
				// If a valid jump_to instruction exists, execute it immediately
				if (jumpTo != null) {
					return switch (jumpTo) {
						case model -> modelDestination;
						case end -> endDestination;
						case tool -> AGENT_TOOL_NAME;
					};
				}
			}
			
			// Priority 2: Check message content for tool calls
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
					return AGENT_TOOL_NAME;
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
						Set<String> requestedToolIds = assistantMessage.getToolCalls().stream()
								.map(AssistantMessage.ToolCall::id)
								.collect(java.util.stream.Collectors.toSet());

						Set<String> executedToolIds = toolResponseMessage.getResponses().stream()
								.map(ToolResponseMessage.ToolResponse::id)
								.collect(java.util.stream.Collectors.toSet());

						if (executedToolIds.containsAll(requestedToolIds)) {
							return modelDestination; // All requested tools were executed or responded
						} else {
							return AGENT_TOOL_NAME; // Some tools are still pending
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

	/**
	 * Collects model interceptors from hooks (ModelHook and AgentHook) and merges them
	 * with the current model interceptors.
	 * <p>
	 * If interceptors with the same name exist, the ones from ReactAgent configuration
	 * take priority over those from hooks.
	 *
	 * @return merged list of model interceptors, or null if no interceptors exist
	 */
	private List<ModelInterceptor> collectAndMergeModelInterceptors() {
		List<ModelInterceptor> result = new ArrayList<>();
		Set<String> addedNames = new HashSet<>();

		// Add current model interceptors if they exist (higher priority)
		if (this.modelInterceptors != null && !this.modelInterceptors.isEmpty()) {
			for (ModelInterceptor interceptor : this.modelInterceptors) {
				result.add(interceptor);
				addedNames.add(interceptor.getName());
			}
		}

		// Collect interceptors from hooks (skip if name already exists)
		if (this.hooks != null && !this.hooks.isEmpty()) {
			for (Hook hook : this.hooks) {
				List<ModelInterceptor> hookInterceptors = hook.getModelInterceptors();
				if (hookInterceptors != null && !hookInterceptors.isEmpty()) {
					for (ModelInterceptor interceptor : hookInterceptors) {
						String name = interceptor.getName();
						if (!addedNames.contains(name)) {
							result.add(interceptor);
							addedNames.add(name);
						} else {
							logger.info("Skipping model interceptor '{}' from hook '{}' because an interceptor with the same name already exists in ReactAgent configuration", name, hook.getName());
						}
					}
				}
			}
		}

		return result.isEmpty() ? null : result;
	}

	/**
	 * Collects tool interceptors from hooks (ModelHook and AgentHook) and merges them
	 * with the current tool interceptors.
	 * <p>
	 * If interceptors with the same name exist, the ones from ReactAgent configuration
	 * take priority over those from hooks.
	 *
	 * @return merged list of tool interceptors, or null if no interceptors exist
	 */
	private List<ToolInterceptor> collectAndMergeToolInterceptors() {
		List<ToolInterceptor> result = new ArrayList<>();
		Set<String> addedNames = new HashSet<>();

		// Add current tool interceptors if they exist (higher priority)
		if (this.toolInterceptors != null && !this.toolInterceptors.isEmpty()) {
			for (ToolInterceptor interceptor : this.toolInterceptors) {
				result.add(interceptor);
				addedNames.add(interceptor.getName());
			}
		}

		// Collect interceptors from hooks (skip if name already exists)
		if (this.hooks != null && !this.hooks.isEmpty()) {
			for (Hook hook : this.hooks) {
				List<ToolInterceptor> hookInterceptors = hook.getToolInterceptors();
				if (hookInterceptors != null && !hookInterceptors.isEmpty()) {
					for (ToolInterceptor interceptor : hookInterceptors) {
						String name = interceptor.getName();
						if (!addedNames.contains(name)) {
							result.add(interceptor);
							addedNames.add(name);
						} else {
							logger.info("Skipping tool interceptor '{}' from hook '{}' because an interceptor with the same name already exists in ReactAgent configuration", name, hook.getName());
						}
					}
				}
			}
		}

		return result.isEmpty() ? null : result;
	}

	public String instruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
		llmNode.setInstruction(instruction);
	}

	public void setSystemPrompt(String systemPrompt) {
		llmNode.setSystemPrompt(systemPrompt);
	}

	public Map<String, Object> getThreadState(String threadId) {
		return threadIdStateMap.get(threadId);
	}

	public class AgentToSubCompiledGraphNodeAdapter implements NodeActionWithConfig, ResumableSubGraphAction {

		private String nodeId;

		private boolean includeContents;

		private boolean returnReasoningContents;

		private String instruction;

		private CompiledGraph childGraph;

		private CompileConfig parentCompileConfig;

		public AgentToSubCompiledGraphNodeAdapter(String nodeId, boolean includeContents, boolean returnReasoningContents,
				CompiledGraph childGraph, String instruction, CompileConfig parentCompileConfig) {
			this.nodeId = nodeId;
			this.includeContents = includeContents;
			this.returnReasoningContents = returnReasoningContents;
			this.instruction = instruction;
			this.childGraph = childGraph;
			this.parentCompileConfig = parentCompileConfig;
		}

		@Override
		public String getResumeSubGraphId() {
			return resumeSubGraphId(nodeId);
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState, RunnableConfig config) throws Exception {
			final boolean resumeSubgraph = config.metadata(resumeSubGraphId(nodeId), new TypeRef<Boolean>() {}).orElse(false);

			RunnableConfig subGraphRunnableConfig = getSubGraphRunnableConfig(config);
			Flux<GraphResponse<NodeOutput>> subGraphResult;
			Object parentMessages = null;

			AgentInstructionMessage instructionMessage = null;
			if (StringUtils.hasLength(instruction)) {
				instructionMessage = AgentInstructionMessage.builder().text(instruction).build();
			}
			if (includeContents) {
				Map<String, Object> stateForChild = new HashMap<>(parentState.data());
				List<Object> newMessages;
				if (stateForChild.get("messages") != null) {
					newMessages = new ArrayList<>((List<Object>)stateForChild.remove("messages"));
				} else {
					newMessages = new ArrayList<>();
				}
				// by default, includeContents is true, we pass down the messages from the parent state
				if (StringUtils.hasLength(instruction)) {
					// instruction will be added as a special UserMessage to the child graph.
					newMessages.add(instructionMessage);
				}
				stateForChild.put("messages", newMessages);
				subGraphResult = childGraph.graphResponseStream(stateForChild, subGraphRunnableConfig);
			} else {
				Map<String, Object> stateForChild = new HashMap<>(parentState.data());
				parentMessages = stateForChild.remove("messages");
				if (StringUtils.hasLength(instruction)) {
					// instruction will be added as a special UserMessage to the child graph.
					stateForChild.put("messages", instructionMessage);
				}
				subGraphResult = childGraph.graphResponseStream(stateForChild, subGraphRunnableConfig);
			}

			Map<String, Object> result = new HashMap<>();

			String outputKeyToParent = StringUtils.hasLength(ReactAgent.this.outputKey) ? ReactAgent.this.outputKey : "messages";
			result.put(outputKeyToParent, getGraphResponseFlux(parentState, subGraphResult, instructionMessage));
			if (parentMessages != null) {
				result.put("messages", parentMessages);
			}
			return result;
		}

		private @NotNull Flux<GraphResponse<NodeOutput>> getGraphResponseFlux(OverAllState parentState, Flux<GraphResponse<NodeOutput>> subGraphResult, AgentInstructionMessage instructionMessage) {
			// Use buffer(2, 1) to create sliding windows: [elem0, elem1], [elem1, elem2], ..., [elemN-1, elemN], [elemN]
			// For windows with 2 elements, emit the first (previous element)
			// For the last window with 1 element, process it specially
			return subGraphResult
					.buffer(2, 1)
					.flatMap(window -> {
						if (window.size() == 1) {
							// Last window: process the last element with message filtering
							return Flux.just(processLastResponse(window.get(0), parentState, instructionMessage));
						} else {
							// Regular window: emit the first element (previous, delayed by one)
							return Flux.just(window.get(0));
						}
					}, 1); // Concurrency of 1 to maintain order
		}

		/**
		 * Process the last response by filtering messages based on parent state and returnReasoningContents flag.
		 *
		 * @param lastResponse the last response from sub-graph
		 * @param parentState the parent state containing messages to filter out
		 * @return processed GraphResponse with filtered messages
		 */
		private GraphResponse<NodeOutput> processLastResponse(GraphResponse<NodeOutput> lastResponse, OverAllState parentState, AgentInstructionMessage instructionMessage) {
			if (lastResponse == null) {
				return lastResponse;
			}
			
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
							} else {
								if (!messages.isEmpty()) {
									if (instructionMessage != null) {
										finalMessages = new ArrayList<>();
										finalMessages.add(instructionMessage);
										finalMessages.add(messages.get(messages.size() - 1));
									} else {
										finalMessages = List.of(messages.get(messages.size() - 1));
									}
								} else {
									finalMessages = List.of();
								}
							}

							Map<String, Object> newResultMap = new HashMap<>(resultMap);
							newResultMap.put("messages", finalMessages);
							return GraphResponse.done(newResultMap);
						}
					}
				}
			}
			return lastResponse;
		}

		private RunnableConfig getSubGraphRunnableConfig(RunnableConfig config) {
			RunnableConfig subGraphRunnableConfig = RunnableConfig.builder(config)
					.checkPointId(null)
					.clearContext()
					.nextNode(null)
					.addMetadata("_AGENT_", subGraphId(nodeId)) // subGraphId is the same as the name of the agent that created it
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
									.map(threadId -> format("%s_%s", threadId, subGraphId(nodeId)))
									.orElseGet(() -> subGraphId(nodeId)))
							.nextNode(null)
							.checkPointId(null)
							.clearContext()
							.addMetadata("_AGENT_", subGraphId(nodeId)) // subGraphId is the same as the name of the agent that created it
							.build();
				}
			}
			return subGraphRunnableConfig;
		}

	}

	/**
	 * Internal class that adapts a ReactAgent to be used as a SubGraph Node.
	 */
	private class AgentSubGraphNode extends Node implements SubGraphNode {

		private final CompiledGraph subGraph;

		public AgentSubGraphNode(String id, boolean includeContents, boolean returnReasoningContents, CompiledGraph subGraph, String instruction) {
			super(Objects.requireNonNull(id, "id cannot be null"),
					(config) -> node_async(new AgentToSubCompiledGraphNodeAdapter(id, includeContents, returnReasoningContents, subGraph, instruction, config)));
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
