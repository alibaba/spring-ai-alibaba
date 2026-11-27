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
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.exception.AgentException;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.AssistantMessageSanitizerInterceptor;
import com.alibaba.cloud.ai.graph.agent.factory.AgentBuilderFactory;
import com.alibaba.cloud.ai.graph.agent.factory.DefaultAgentBuilderFactory;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.InstructionAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.InterruptionHook;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.ToolInjection;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectModelHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.StreamingModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.strategy.MessageIdStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.model.tool.ToolCallDecisions;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallResults;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_MODEL_NAME;
import static com.alibaba.cloud.ai.graph.RunnableConfig.AGENT_TOOL_NAME;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static com.alibaba.cloud.ai.graph.agent.hook.InterruptionHook.INTERRUPTION_FEEDBACK_KEY;
import static com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectConstants.FINISH_REASON_METADATA_KEY;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.childConfig;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.resumeSubGraphId;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.shouldResume;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.subGraphId;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;


public class ReactAgent extends BaseAgent {
	Logger logger = LoggerFactory.getLogger(ReactAgent.class);

	private final ConcurrentMap<String, Map<String, Object>> threadIdStateMap;

	private final AgentLlmNode llmNode;

	private final AgentToolNode toolNode;

	private List<? extends Hook> hooks;

	private List<ModelInterceptor> modelInterceptors;

	private List<ToolInterceptor> toolInterceptors;

	private List<StreamingModelInterceptor> streamingInterceptors;

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
		this.streamingInterceptors = builder.streamingInterceptors;
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
		if (this.streamingInterceptors != null && !this.streamingInterceptors.isEmpty()) {
			this.llmNode.setStreamingInterceptors(this.streamingInterceptors);
		}
		if (this.hooks != null) {
			this.llmNode.setStreamingOutputFilters(this.hooks.stream()
					.flatMap(hook -> hook.getStreamingOutputFilters().stream()).toList());
		}

		hasTools = !toolNode.getToolCallbacks().isEmpty() || builder.dynamicTools || builder.resolver != null
				|| (mergedModelInterceptors != null && mergedModelInterceptors.stream().anyMatch(interceptor ->
						!(interceptor instanceof AssistantMessageSanitizerInterceptor)));
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

	/**
	 * Calls the agent with the given inputs map and returns the assistant message.
	 * <p>
	 * When you need to pass additional parameters beyond {@code messages} and {@code input},
	 * use this overload.
	 * <p>
	 * Reserved keys: {@code messages} and {@code input} are used as question/input for the
	 * agent. Other keys can be arbitrary and are passed as graph state, e.g. for prompt
	 * placeholders or any other state values.
	 *
	 * @param inputs the input map (reserved: messages, input; other keys as state)
	 * @return the assistant message response
	 * @throws GraphRunnerException if the graph execution fails
	 */
	public AssistantMessage call(Map<String, Object> inputs) throws GraphRunnerException {
		return doMessageInvoke(inputs, null);
	}

	/**
	 * Calls the agent with the given inputs map and runtime config, returns the assistant message.
	 * <p>
	 * When you need to pass additional parameters beyond {@code messages} and {@code input},
	 * use this overload.
	 * <p>
	 * Reserved keys: {@code messages} and {@code input} are used as question/input for the
	 * agent. Other keys can be arbitrary and are passed as graph state, e.g. for prompt
	 * placeholders or any other state values.
	 *
	 * @param inputs the input map (reserved: messages, input; other keys as state)
	 * @param config runtime configuration controlling execution behavior
	 * @return the assistant message response
	 * @throws GraphRunnerException if the graph execution fails
	 */
	public AssistantMessage call(Map<String, Object> inputs, RunnableConfig config) throws GraphRunnerException {
		return doMessageInvoke(inputs, config);
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
		Map<String, Object> inputs = buildMessageInput(message);
		return extractAssistantMessage(doInvoke(inputs, config));
	}

	private AssistantMessage doMessageInvoke(Map<String, Object> inputs, RunnableConfig config) throws GraphRunnerException {
		return extractAssistantMessage(doInvoke(inputs, config));
	}

	private AssistantMessage extractAssistantMessage(Optional<OverAllState> state) {
		if (StringUtils.hasLength(outputKey)) {
			return state.flatMap(s -> s.value(outputKey))
					.map(msg -> (AssistantMessage) msg)
					.orElseThrow(() -> new IllegalStateException("Output key " + outputKey + " not found in agent state"));
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

		// Always inject default InstructionAgentHook so instruction is handled in beforeAgent
		List<Hook> effectiveHooks = new ArrayList<>();
		effectiveHooks.add(InstructionAgentHook.create());
		effectiveHooks.addAll(hooks);
		if (hasTools && hooks.stream().noneMatch(HumanInTheLoopHook.class::isInstance)) {
			effectiveHooks.add(HumanInTheLoopHook.builder().build());
		}

		// Validate hook uniqueness
		Set<String> hookNames = new HashSet<>();
		for (Hook hook : effectiveHooks) {
			if (!hookNames.add(Hook.getFullHookName(hook))) {
				throw new IllegalArgumentException("Duplicate hook instances found");
			}

			// set agent name to every hook node.
			hook.setAgentName(this.name);
			hook.setAgent(this);
		}

		// Create graph with state serializer
		StateGraph graph = new StateGraph(name, buildMessagesKeyStrategyFactory(effectiveHooks), stateSerializer);

		graph.addNode(AGENT_MODEL_NAME, node_async(this.llmNode));
		if (hasTools) {
			graph.addNode(AGENT_TOOL_NAME, this.toolNode::applyAsync);
		}

		// some hooks may need tools so they can do some initialization/cleanup on start/end of agent loop
		setupToolsForHooks(effectiveHooks, toolNode);

		// Categorize hooks by position
		List<Hook> beforeAgentHooks = filterHooksByPosition(effectiveHooks, HookPosition.BEFORE_AGENT);
		List<Hook> afterAgentHooks = filterHooksByPosition(effectiveHooks, HookPosition.AFTER_AGENT);
		List<Hook> beforeModelHooks = filterHooksByPosition(effectiveHooks, HookPosition.BEFORE_MODEL);
		List<Hook> afterModelHooks = filterHooksByPosition(effectiveHooks, HookPosition.AFTER_MODEL);

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

	public boolean requiresToolConfirmation(AssistantMessage.ToolCall call, RunnableConfig config) {
		return toolNode.requiresConfirmation(call, config);
	}

	public boolean requiresToolConfirmation(AssistantMessage message, AssistantMessage.ToolCall call,
			RunnableConfig config, boolean explicitlyConfigured) {
		return toolNode.requiresConfirmation(message, call, config, explicitlyConfigured);
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
			addHookEdge(graph, loopExitNode, exitNode, loopEntryNode, exitNode, afterModelHooks.get(0).canJumpTo());
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

			graph.addConditionalEdges(name, consumeJump(router), destinations);
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
		graph.addConditionalEdges(loopExitNode, consumeJump(agentInstance.makeModelToTools(loopEntryNode, exitNode)), Map.of(AGENT_TOOL_NAME, AGENT_TOOL_NAME, exitNode, exitNode, loopEntryNode, loopEntryNode));

		// Tools to model routing
		graph.addConditionalEdges(AGENT_TOOL_NAME, agentInstance.makeToolsToModelEdge(loopEntryNode, exitNode),
				Map.of(loopEntryNode, loopEntryNode, exitNode, exitNode));
	}

	private static AsyncCommandAction consumeJump(EdgeAction edge) {
		return AsyncCommandAction.node_async((state, config) -> new Command(edge.apply(state),
				state.value("jump_to").isPresent()
						? Map.of("jump_to", OverAllState.MARK_FOR_REMOVAL) : Map.of()));
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
			keyStrategyHashMap.put("messages", new MessageIdStrategy());

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
						List<String> requestedToolIds = ToolCallDecisions.toolCallKeys(assistantMessage);
						Set<String> executedToolIds = ToolCallResults.settledKeys(assistantMessage, toolResponseMessage.getResults());

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

	private AsyncCommandAction makeToolsToModelEdge(String modelDestination, String endDestination) {
		boolean usesCustomHook = hooks.stream().anyMatch(ReturnDirectModelHook.class::isInstance);
		var hook = new ReturnDirectModelHook();
		hook.setAgent(this);
		var directOutput = MessagesModelHook.beforeModelAction(hook);
		return (state, config) -> {
			var messages = state.<List<Message>>value("messages").orElseGet(List::of);
			if (!usesCustomHook && !messages.isEmpty()
					&& messages.get(messages.size() - 1) instanceof ToolResponseMessage response
					&& FINISH_REASON.equals(response.getMetadata().get(FINISH_REASON_METADATA_KEY))) {
				return directOutput.apply(state, config).thenApply(update -> {
					update.remove("jump_to");
					return new Command(endDestination, update);
				});
			}
			return CompletableFuture.completedFuture(new Command(modelDestination));
		};
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

		private CompiledGraph childGraph;

		private CompileConfig parentCompileConfig;

		public AgentToSubCompiledGraphNodeAdapter(String nodeId, boolean includeContents, boolean returnReasoningContents,
				CompiledGraph childGraph, String instruction, CompileConfig parentCompileConfig) {
			this.nodeId = nodeId;
			this.includeContents = includeContents;
			this.returnReasoningContents = returnReasoningContents;
			this.childGraph = childGraph;
			this.parentCompileConfig = parentCompileConfig;
		}

		@Override
		public String getResumeSubGraphId() {
			return resumeSubGraphId(nodeId);
		}

		@Override
		public Map<String, Object> apply(OverAllState parentState, RunnableConfig config) throws Exception {
			RunnableConfig subGraphRunnableConfig = getSubGraphRunnableConfig(config);
			Map<String, Object> stateForChild = new HashMap<>();
			if (!shouldResume(nodeId, config)) {
				stateForChild.putAll(parentState.data());
				if (includeContents) {
					var messages = parentState.<List<Object>>value("messages").orElseGet(List::of);
					stateForChild.put("messages", new ArrayList<>(messages));
				}
				else {
					stateForChild.remove("messages");
				}
			}
			var subGraphResult = childGraph.graphResponseStream(stateForChild, subGraphRunnableConfig);
			String outputKeyToParent = StringUtils.hasLength(outputKey) ? outputKey : "messages";
			return Map.of(outputKeyToParent, getGraphResponseFlux(parentState, subGraphResult));
		}

		private Flux<GraphResponse<NodeOutput>> getGraphResponseFlux(OverAllState parentState,
				Flux<GraphResponse<NodeOutput>> subGraphResult) {
			return subGraphResult.map(response -> response.isDone() ? projectResult(response, parentState) : response);
		}

		@SuppressWarnings("unchecked")
		private GraphResponse<NodeOutput> projectResult(GraphResponse<NodeOutput> response, OverAllState parentState) {
			if (!(response.resultValue().orElse(null) instanceof Map<?, ?> state)
					|| !(state.get("messages") instanceof List<?> childMessages)) {
				return response;
			}
			var messages = new ArrayList<>(childMessages);
			if (includeContents) {
				parentState.<List<Object>>value("messages")
						.ifPresent(parentMessages -> parentMessages.forEach(messages::remove));
			}
			List<?> output = returnReasoningContents || messages.isEmpty()
					? messages : List.of(messages.get(messages.size() - 1));
			var projected = new HashMap<>((Map<String, Object>) state);
			projected.put("messages", output);
			return GraphResponse.done(projected, response.getAllMetadata());
		}

		private RunnableConfig getSubGraphRunnableConfig(RunnableConfig config) {
			return childConfig(nodeId, config, parentCompileConfig, childGraph.compileConfig)
					.addMetadata(RunnableConfig.AGENT_NAME_KEY, subGraphId(nodeId))
					.clearContext()
					.build();
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
