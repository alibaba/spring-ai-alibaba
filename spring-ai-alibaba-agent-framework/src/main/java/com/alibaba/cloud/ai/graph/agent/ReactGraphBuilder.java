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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.ToolInjection;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;

/**
 * A builder class responsible for constructing the StateGraph for a ReactAgent.
 * This class isolates the graph construction logic from the main agent logic.
 */
public class ReactGraphBuilder {

    private final ReactAgent agent;

    public ReactGraphBuilder(ReactAgent agent) {
        this.agent = agent;
    }

    public StateGraph build() throws GraphStateException {
        if (agent.hooks == null) {
            agent.hooks = new ArrayList<>();
        }

        // Validate hook uniqueness
        Set<String> hookNames = new HashSet<>();
        for (Hook hook : agent.hooks) {
            if (!hookNames.add(hook.getName())) {
                throw new IllegalArgumentException("Duplicate hook instances found");
            }

            // set agent name to every hook node.
            hook.setAgentName(agent.name());
        }

        // Create graph with state serializer
        StateGraph graph = new StateGraph(agent.name(), agent.buildMessagesKeyStrategyFactory(agent.hooks),
                agent.stateSerializer);

        graph.addNode("model", node_async(agent.llmNode));
        if (agent.hasTools) {
            graph.addNode("tool", node_async(agent.toolNode));
        }

        // some hooks may need tools so they can do some initialization/cleanup on
        // start/end of agent loop
        setupToolsForHooks(agent.hooks, agent.toolNode);

        // Categorize hooks by position
        List<Hook> beforeAgentHooks = filterHooksByPosition(agent.hooks, HookPosition.BEFORE_AGENT);
        List<Hook> afterAgentHooks = filterHooksByPosition(agent.hooks, HookPosition.AFTER_AGENT);
        List<Hook> beforeModelHooks = filterHooksByPosition(agent.hooks, HookPosition.BEFORE_MODEL);
        List<Hook> afterModelHooks = filterHooksByPosition(agent.hooks, HookPosition.AFTER_MODEL);

        // Add hook nodes for beforeAgent hooks
        for (Hook hook : beforeAgentHooks) {
            if (hook instanceof AgentHook agentHook) {
                graph.addNode(hook.getName() + ".before", agentHook::beforeAgent);
            } else if (hook instanceof MessagesAgentHook messagesAgentHook) {
                graph.addNode(hook.getName() + ".before", MessagesAgentHook.beforeAgentAction(messagesAgentHook));
            }
        }

        // Add hook nodes for afterAgent hooks
        for (Hook hook : afterAgentHooks) {
            if (hook instanceof AgentHook agentHook) {
                graph.addNode(hook.getName() + ".after", agentHook::afterAgent);
            } else if (hook instanceof MessagesAgentHook messagesAgentHook) {
                graph.addNode(hook.getName() + ".after", MessagesAgentHook.afterAgentAction(messagesAgentHook));
            }
        }

        // Add hook nodes for beforeModel hooks
        for (Hook hook : beforeModelHooks) {
            if (hook instanceof ModelHook modelHook) {
                graph.addNode(hook.getName() + ".beforeModel", modelHook::beforeModel);
            } else if (hook instanceof MessagesModelHook messagesModelHook) {
                graph.addNode(hook.getName() + ".beforeModel", MessagesModelHook.beforeModelAction(messagesModelHook));
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
            } else if (hook instanceof MessagesModelHook messagesModelHook) {
                graph.addNode(hook.getName() + ".afterModel", MessagesModelHook.afterModelAction(messagesModelHook));
            }
        }

        // Determine node flow
        String entryNode = determineEntryNode(beforeAgentHooks, beforeModelHooks);
        String loopEntryNode = determineLoopEntryNode(beforeModelHooks);
        String loopExitNode = determineLoopExitNode(afterModelHooks);
        String exitNode = determineExitNode(afterAgentHooks);

        // Set up edges
        graph.addEdge(START, entryNode);
        setupHookEdges(graph, beforeAgentHooks, afterAgentHooks, beforeModelHooks, afterModelHooks, entryNode,
                loopEntryNode, loopExitNode, exitNode, agent);
        return graph;
    }

    /**
     * Setup and inject tools for hooks that implement ToolInjection interface. Only
     * the
     * tool matching the hook's required tool name or type will be injected.
     * 
     * @param hooks    the list of hooks
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
     * Find the matching tool based on hook's requirements. Matching priority: 1) by
     * name,
     * 2) by type, 3) first available tool
     * 
     * @param toolInjection  the hook that needs a tool
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
     * If a hook implements the Prioritized interface, it will be sorted by its order.
     * Hooks that don't implement Prioritized will maintain their original order.
     *
     * @param hooks    the list of hooks to filter
     * @param position the position to filter by
     * @return list of hooks that should execute at the specified position
     */
    private static List<Hook> filterHooksByPosition(List<? extends Hook> hooks, HookPosition position) {
        List<Hook> filtered = hooks.stream().filter(hook -> {
            HookPosition[] positions = hook.getHookPositions();
            return Arrays.asList(positions).contains(position);
        }).collect(Collectors.toList());

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
        prioritizedHooks.sort(
                (h1, h2) -> Integer.compare(((Prioritized) h1).getOrder(), ((Prioritized) h2).getOrder()));

        // Combine: prioritized hooks first (sorted), then non-prioritized hooks
        // (original order)
        List<Hook> result = new ArrayList<>(prioritizedHooks);
        result.addAll(nonPrioritizedHooks);

        return result;
    }

    private static String determineEntryNode(List<Hook> agentHooks, List<Hook> modelHooks) {

        if (!agentHooks.isEmpty()) {
            return agentHooks.get(0).getName() + ".before";
        } else if (!modelHooks.isEmpty()) {
            return modelHooks.get(0).getName() + ".beforeModel";
        } else {
            return "model";
        }
    }

    private static String determineLoopEntryNode(List<Hook> modelHooks) {

        if (!modelHooks.isEmpty()) {
            return modelHooks.get(0).getName() + ".beforeModel";
        } else {
            return "model";
        }
    }

    private static String determineLoopExitNode(List<Hook> modelHooks) {

        if (!modelHooks.isEmpty()) {
            return modelHooks.get(0).getName() + ".afterModel";
        } else {
            return "model";
        }
    }

    private static String determineExitNode(List<Hook> agentHooks) {

        if (!agentHooks.isEmpty()) {
            return agentHooks.get(agentHooks.size() - 1).getName() + ".after";
        } else {
            return StateGraph.END;
        }
    }

    private static void setupHookEdges(StateGraph graph, List<Hook> beforeAgentHooks, List<Hook> afterAgentHooks,
            List<Hook> beforeModelHooks, List<Hook> afterModelHooks, String entryNode, String loopEntryNode,
            String loopExitNode, String exitNode, ReactAgent agentInstance) throws GraphStateException {

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
            addHookEdge(graph, loopExitNode, exitNode, loopEntryNode, exitNode,
                    afterModelHooks.get(afterModelHooks.size() - 1).canJumpTo());
        } else {
            // No tools and no after_model - direct to exit
            graph.addEdge(loopExitNode, exitNode);
        }
    }

    private static void chainModelHookReverse(StateGraph graph, List<Hook> hooks, String nameSuffix, String defaultNext,
            String modelDestination, String endDestination) throws GraphStateException {

        graph.addEdge(defaultNext, hooks.get(hooks.size() - 1).getName() + nameSuffix);

        for (int i = hooks.size() - 1; i > 0; i--) {
            Hook m1 = hooks.get(i);
            Hook m2 = hooks.get(i - 1);
            addHookEdge(graph, m1.getName() + nameSuffix, m2.getName() + nameSuffix, modelDestination, endDestination,
                    m1.canJumpTo());
        }
    }

    private static void chainAgentHookReverse(StateGraph graph, List<Hook> hooks, String nameSuffix, String defaultNext,
            String modelDestination, String endDestination) throws GraphStateException {
        if (!hooks.isEmpty()) {
            Hook first = hooks.get(0);
            addHookEdge(graph, first.getName() + nameSuffix, StateGraph.END, modelDestination, endDestination,
                    first.canJumpTo());
        }

        for (int i = hooks.size() - 1; i > 0; i--) {
            Hook m1 = hooks.get(i);
            Hook m2 = hooks.get(i - 1);
            addHookEdge(graph, m1.getName() + nameSuffix, m2.getName() + nameSuffix, modelDestination, endDestination,
                    m1.canJumpTo());
        }
    }

    private static void chainHook(StateGraph graph, List<Hook> hooks, String nameSuffix, String defaultNext,
            String modelDestination, String endDestination) throws GraphStateException {

        for (int i = 0; i < hooks.size() - 1; i++) {
            Hook m1 = hooks.get(i);
            Hook m2 = hooks.get(i + 1);
            addHookEdge(graph, m1.getName() + nameSuffix, m2.getName() + nameSuffix, modelDestination, endDestination,
                    m1.canJumpTo());
        }

        if (!hooks.isEmpty()) {
            Hook last = hooks.get(hooks.size() - 1);
            addHookEdge(graph, last.getName() + nameSuffix, defaultNext, modelDestination, endDestination,
                    last.canJumpTo());
        }
    }

    private static void addHookEdge(StateGraph graph, String name, String defaultDestination, String modelDestination,
            String endDestination, List<JumpTo> canJumpTo) throws GraphStateException {

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

    private static void setupToolRouting(StateGraph graph, String loopExitNode, String loopEntryNode, String exitNode,
            ReactAgent agentInstance) throws GraphStateException {

        // Model to tools routing
        graph.addConditionalEdges(loopExitNode, edge_async(agentInstance.makeModelToTools(loopEntryNode, exitNode)),
                Map.of("tool", "tool", exitNode, exitNode, loopEntryNode, loopEntryNode));

        // Tools to model routing
        graph.addConditionalEdges("tool", edge_async(agentInstance.makeToolsToModelEdge(loopEntryNode, exitNode)),
                Map.of(loopEntryNode, loopEntryNode, exitNode, exitNode));
    }

    private static String resolveJump(JumpTo jumpTo, String modelDestination, String endDestination,
            String defaultDestination) {
        if (jumpTo == null) {
            return defaultDestination;
        }

        return switch (jumpTo) {
            case model -> modelDestination;
            case end -> endDestination;
            case tool -> "tool";
        };
    }

}
