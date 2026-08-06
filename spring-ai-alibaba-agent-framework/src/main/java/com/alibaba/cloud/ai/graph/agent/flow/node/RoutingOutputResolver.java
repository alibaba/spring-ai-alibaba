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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.FlowAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.outputKeyToParent;

/**
 * Resolves the parent-state output keys a routed agent may expose.
 */
final class RoutingOutputResolver {

	private static final Logger logger = LoggerFactory.getLogger(RoutingOutputResolver.class);

	private final String routingAgentName;

	private final List<Agent> subAgents;

	private final List<RoutingOutputStrategy> strategies;

	RoutingOutputResolver(String routingAgentName, List<? extends Agent> subAgents) {
		this.routingAgentName = routingAgentName;
		this.subAgents = List.copyOf(subAgents);
		// Keep this order from specific to generic because several agent types inherit
		// from FlowAgent.
		this.strategies = List.of(
				new BaseAgentOutputStrategy(),
				new ParallelAgentOutputStrategy(),
				new LlmRoutingAgentOutputStrategy(),
				new SequentialAgentOutputStrategy(),
				new FlowAgentOutputStrategy());
	}

	int subAgentCount() {
		return subAgents.size();
	}

	List<Agent> subAgents() {
		return subAgents;
	}

	/**
	 * Captures route-selection facts that must stay consistent during one merge pass.
	 * @param state current graph state
	 * @return immutable context for this merge pass
	 */
	MergeContext createContext(OverAllState state) {
		Optional<Set<String>> routedAgentNames = currentRoutedAgentNames(state);
		long routedAgentCount = countRoutedAgents(state, routedAgentNames);
		boolean hasRoutingMarkers = routedAgentNames.isPresent() || routedAgentCount > 0;
		long routingMergedOutputOwnerCount = countRoutingMergedOutputOwners(state, hasRoutingMarkers,
				routedAgentNames);
		boolean exposeMergedResultToParent = routingAgentName != null
				&& state.value(routingAgentName + "_input").isPresent();
		return new MergeContext(routedAgentNames, routedAgentCount, hasRoutingMarkers,
				routingMergedOutputOwnerCount, exposeMergedResultToParent);
	}

	/**
	 * Collects the best attributable output for one top-level routed sub-agent.
	 * @param state current graph state
	 * @param subAgent top-level sub-agent being collected
	 * @param context merge context created for this pass
	 * @param collectedOutputKeys output keys already attributed to earlier sub-agents
	 * @return collected text when the sub-agent has an attributable result
	 */
	Optional<CollectedAgentResult> collectAgentResult(OverAllState state, Agent subAgent,
			MergeContext context, Set<String> collectedOutputKeys) {
		boolean topLevelRoutedAgent = isTopLevelRoutedAgent(state, subAgent, context.routedAgentNames());
		if (context.hasRoutingMarkers() && !topLevelRoutedAgent) {
			logger.debug("Skipping unrouted sub-agent {}", subAgent.name());
			return Optional.empty();
		}

		boolean allowDefaultMessagesOutput = context.routedAgentCount() == 1 && topLevelRoutedAgent;
		boolean allowRoutingMergedOutput = context.routingMergedOutputOwnerCount() == 1
				&& usesRoutingMergedOutput(subAgent);
		boolean preferWrapperOutput = shouldPreferWrapperOutput(state, subAgent, context.routedAgentCount(),
				topLevelRoutedAgent, context.routingMergedOutputOwnerCount(), context.routedAgentNames());
		List<RoutingOutputCandidate> candidates = resolveCandidates(subAgent, allowDefaultMessagesOutput,
				allowRoutingMergedOutput, preferWrapperOutput);

		List<String> sourceTexts = new ArrayList<>();
		boolean collectMultipleOutputs = collectsMultipleOutputs(subAgent);
		boolean collectedWrapperOutput = false;
		for (RoutingOutputCandidate candidate : candidates) {
			if (collectedOutputKeys.contains(candidate.outputKey())) {
				continue;
			}
			if (collectMultipleOutputs && !sourceTexts.isEmpty() && candidate.wrapperOutput()) {
				continue;
			}
			Optional<Object> outputOpt = state.value(candidate.outputKey());
			if (outputOpt.isEmpty()) {
				continue;
			}
			String text = RoutingMergeNode.extractText(outputOpt.get(), candidate.outputKey());
			if (text == null || text.isBlank()) {
				continue;
			}
			collectedOutputKeys.add(candidate.outputKey());
			sourceTexts.add(text);
			collectedWrapperOutput = collectedWrapperOutput || candidate.wrapperOutput();
			logger.debug("Collected result from {} (key: {})", subAgent.name(), candidate.outputKey());
			if (!collectMultipleOutputs || (preferWrapperOutput && candidate.wrapperOutput())) {
				break;
			}
		}
		if (sourceTexts.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new CollectedAgentResult(String.join("\n\n", sourceTexts), collectedWrapperOutput));
	}

	/**
	 * Clears a stale wrapper when the current answer came from a nested routing merge key.
	 * @param state current graph state
	 * @param subAgent sub-agent whose wrapper may be stale
	 * @param result collected result for the sub-agent
	 * @param staleWrapperOutputKeys removal set updated by this method
	 */
	void markStaleWrapperIfNeeded(OverAllState state, Agent subAgent, CollectedAgentResult result,
			Set<String> staleWrapperOutputKeys) {
		String staleWrapperOutputKey = outputKeyToParent(subAgent.name());
		if (!usesRoutingMergedOutput(subAgent) || result.fromWrapperOutput()
				|| state.value(staleWrapperOutputKey).isEmpty()) {
			return;
		}
		staleWrapperOutputKeys.add(staleWrapperOutputKey);
		logger.debug("RoutingMergeNode: routing result for {} came from merged output; clearing stale wrapper key {}",
				subAgent.name(), staleWrapperOutputKey);
	}

	/**
	 * Returns the parent-state wrapper key for this routing graph.
	 * @return wrapper key for this router's compiled subgraph result
	 */
	String wrapperOutputKey() {
		return outputKeyToParent(routingAgentName);
	}

	/**
	 * Resolves candidate output keys in the exact order they should be probed.
	 * @param agent agent whose outputs are being resolved
	 * @param allowDefaultMessagesOutput whether the shared messages key can be used
	 * @param allowRoutingMergedOutput whether nested router merged output can be used
	 * @param preferWrapperOutput whether the wrapper key should be probed first
	 * @return ordered output candidates
	 */
	private List<RoutingOutputCandidate> resolveCandidates(Agent agent, boolean allowDefaultMessagesOutput,
			boolean allowRoutingMergedOutput, boolean preferWrapperOutput) {
		List<RoutingOutputCandidate> candidates = new ArrayList<>();
		Deque<RoutingOutputResolutionStep> steps = new ArrayDeque<>();
		steps.push(new ExpandAgentOutputStep(agent, allowDefaultMessagesOutput, allowRoutingMergedOutput,
				preferWrapperOutput));
		while (!steps.isEmpty()) {
			RoutingOutputResolutionStep step = steps.pop();
			if (step instanceof EmitOutputCandidateStep emitStep) {
				candidates.add(emitStep.candidate());
				continue;
			}
			ExpandAgentOutputStep expandStep = (ExpandAgentOutputStep) step;
			if (expandStep.preferWrapperOutput()) {
				candidates.add(wrapperCandidate(expandStep.agent()));
			}
			else {
				// Delay wrapper output so explicit nested outputs win unless wrapper
				// isolation is required for multi-routed workflows with shared keys.
				steps.push(new EmitOutputCandidateStep(wrapperCandidate(expandStep.agent())));
			}
			strategyFor(expandStep.agent())
				.ifPresent(strategy -> strategy.appendCandidates(expandStep, steps, candidates));
		}
		return candidates;
	}

	/**
	 * Creates the namespaced wrapper candidate for a nested FlowAgent.
	 * @param agent agent whose wrapper key should be created
	 * @return wrapper output candidate
	 */
	private RoutingOutputCandidate wrapperCandidate(Agent agent) {
		return new RoutingOutputCandidate(outputKeyToParent(agent.name()), true);
	}

	/**
	 * Finds the output strategy for an agent without exposing a default no-op strategy.
	 * @param agent agent to match
	 * @return matching strategy, or empty when the agent type has no output strategy
	 */
	private Optional<RoutingOutputStrategy> strategyFor(Agent agent) {
		return strategies.stream()
			.filter(strategy -> strategy.supports(agent))
			.findFirst();
	}

	/**
	 * Prefers wrapper output when raw nested keys are ambiguous across routed workflows.
	 * @param state current graph state
	 * @param agent agent being evaluated
	 * @param routedAgentCount number of currently selected top-level sub-agents
	 * @param topLevelRoutedAgent whether the agent belongs to the current route
	 * @param routingMergedOutputOwnerCount selected agents that may expose nested routing
	 * merge output
	 * @param routedAgentNames optional namespaced routing selection marker
	 * @return true when wrapper output should be probed before raw nested keys
	 */
	private boolean shouldPreferWrapperOutput(OverAllState state, Agent agent, long routedAgentCount,
			boolean topLevelRoutedAgent, long routingMergedOutputOwnerCount, Optional<Set<String>> routedAgentNames) {
		if (routedAgentCount <= 1 || !topLevelRoutedAgent || !(agent instanceof FlowAgent)
				|| state.value(outputKeyToParent(agent.name())).isEmpty()) {
			return false;
		}

		Set<String> outputKeys = resolvedNonWrapperOutputKeys(agent, routingMergedOutputOwnerCount);
		if (outputKeys.isEmpty()) {
			return true;
		}

		return subAgents.stream()
			.filter(other -> other != agent)
			.filter(other -> isTopLevelRoutedAgent(state, other, routedAgentNames))
			.map(other -> resolvedNonWrapperOutputKeys(other, routingMergedOutputOwnerCount))
			.anyMatch(otherOutputKeys -> otherOutputKeys.stream().anyMatch(outputKeys::contains));
	}

	/**
	 * Collects raw candidate keys so sibling workflows can be checked for shared keys.
	 * @param agent agent whose non-wrapper output keys should be resolved
	 * @param routingMergedOutputOwnerCount selected agents that may expose nested routing
	 * merge output
	 * @return raw non-wrapper output keys
	 */
	private Set<String> resolvedNonWrapperOutputKeys(Agent agent, long routingMergedOutputOwnerCount) {
		boolean allowRoutingMergedOutput = routingMergedOutputOwnerCount == 1 && usesRoutingMergedOutput(agent);
		Set<String> outputKeys = new HashSet<>();
		for (RoutingOutputCandidate candidate : resolveCandidates(agent, false, allowRoutingMergedOutput, false)) {
			if (!candidate.wrapperOutput()) {
				outputKeys.add(candidate.outputKey());
			}
		}
		return outputKeys;
	}

	/**
	 * Counts only currently selected top-level agents, ignoring stale checkpoint inputs.
	 * @param state current graph state
	 * @param routedAgentNames optional namespaced routing selection marker
	 * @return number of selected top-level sub-agents
	 */
	private long countRoutedAgents(OverAllState state, Optional<Set<String>> routedAgentNames) {
		if (routedAgentNames.isPresent()) {
			Set<String> selectedAgents = routedAgentNames.get();
			return subAgents.stream().filter(agent -> selectedAgents.contains(agent.name())).count();
		}
		return subAgents.stream().filter(agent -> isTopLevelRoutedAgent(state, agent, routedAgentNames)).count();
	}

	/**
	 * Counts selected agents whose visible answer may come from a nested router merge.
	 * @param state current graph state
	 * @param hasRoutingMarkers whether the state contains current routing markers
	 * @param routedAgentNames optional namespaced routing selection marker
	 * @return number of selected agents that may use routing merged output
	 */
	private long countRoutingMergedOutputOwners(OverAllState state, boolean hasRoutingMarkers,
			Optional<Set<String>> routedAgentNames) {
		return subAgents.stream()
			.filter(agent -> !hasRoutingMarkers || isTopLevelRoutedAgent(state, agent, routedAgentNames))
			.filter(this::usesRoutingMergedOutput)
			.count();
	}

	/**
	 * Checks whether a top-level sub-agent belongs to the current routing decision.
	 * @param state current graph state
	 * @param agent top-level sub-agent being checked
	 * @param routedAgentNames optional namespaced routing selection marker
	 * @return true if the agent is selected in the current route
	 */
	private boolean isTopLevelRoutedAgent(OverAllState state, Agent agent, Optional<Set<String>> routedAgentNames) {
		if (routedAgentNames.isPresent()) {
			return routedAgentNames.get().contains(agent.name());
		}
		return state.value(agent.name() + "_input").isPresent();
	}

	/**
	 * Reads the namespaced routing marker written by RoutingNode for this router.
	 * @param state current graph state
	 * @return selected top-level agent names when a routing marker exists
	 */
	private Optional<Set<String>> currentRoutedAgentNames(OverAllState state) {
		Optional<Object> value = routingAgentName != null
				? state.value(RoutingNode.routedAgentNamesKey(routingAgentName))
				: state.value(RoutingNode.ROUTED_AGENT_NAMES_KEY);
		if (value.isEmpty()) {
			return Optional.empty();
		}

		Set<String> agentNames = new LinkedHashSet<>();
		Object routedAgents = value.get();
		if (routedAgents instanceof Iterable<?> iterable) {
			for (Object agentName : iterable) {
				if (agentName instanceof String name && !name.isBlank()) {
					agentNames.add(name);
				}
			}
		}
		else if (routedAgents instanceof String name && !name.isBlank()) {
			agentNames.add(name);
		}
		return Optional.of(agentNames);
	}

	/**
	 * Detects whether an agent tree contains a routing graph whose merged answer matters.
	 * @param agent root of the agent tree to inspect
	 * @return true if the tree contains a nested routing agent
	 */
	private boolean usesRoutingMergedOutput(Agent agent) {
		Deque<Agent> stack = new ArrayDeque<>();
		stack.push(agent);
		while (!stack.isEmpty()) {
			Agent current = stack.pop();
			if (current instanceof LlmRoutingAgent) {
				return true;
			}
			if (current instanceof ParallelAgent parallelAgent && parallelAgent.mergeOutputKey() != null) {
				continue;
			}
			if (current instanceof SequentialAgent sequentialAgent) {
				List<Agent> nestedAgents = sequentialAgent.subAgents();
				if (nestedAgents != null && !nestedAgents.isEmpty()) {
					stack.push(nestedAgents.get(nestedAgents.size() - 1));
				}
			}
			else if (current instanceof FlowAgent flowAgent) {
				pushAll(flowAgent.subAgents(), stack);
			}
		}
		return false;
	}

	/**
	 * Checks whether one routed workflow should contribute multiple child outputs.
	 * @param agent routed agent or workflow to inspect
	 * @return true if the workflow should contribute more than one output
	 */
	private boolean collectsMultipleOutputs(Agent agent) {
		Agent current = agent;
		while (true) {
			if (current instanceof ParallelAgent parallelAgent) {
				return parallelAgent.mergeOutputKey() == null;
			}
			if (current instanceof LlmRoutingAgent) {
				return false;
			}
			if (current instanceof SequentialAgent sequentialAgent) {
				List<Agent> nestedAgents = sequentialAgent.subAgents();
				if (nestedAgents == null || nestedAgents.isEmpty()) {
					return false;
				}
				current = nestedAgents.get(nestedAgents.size() - 1);
			}
			else if (current instanceof FlowAgent flowAgent) {
				List<Agent> nestedAgents = flowAgent.subAgents();
				if (nestedAgents == null || nestedAgents.size() != 1) {
					return false;
				}
				current = nestedAgents.get(0);
			}
			else {
				return false;
			}
		}
	}

	/**
	 * Pushes agents in reverse order so stack traversal preserves list order.
	 * @param agents agents to push
	 * @param stack stack receiving the agents
	 */
	private static void pushAll(List<Agent> agents, Deque<Agent> stack) {
		if (agents == null) {
			return;
		}
		for (int i = agents.size() - 1; i >= 0; i--) {
			stack.push(agents.get(i));
		}
	}

	record MergeContext(Optional<Set<String>> routedAgentNames, long routedAgentCount,
			boolean hasRoutingMarkers, long routingMergedOutputOwnerCount, boolean exposeMergedResultToParent) {
	}

	record CollectedAgentResult(String text, boolean fromWrapperOutput) {
	}

}
