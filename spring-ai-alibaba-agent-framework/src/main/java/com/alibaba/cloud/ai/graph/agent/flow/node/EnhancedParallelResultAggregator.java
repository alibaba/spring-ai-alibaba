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

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Enhanced parallel result aggregator that supports custom merge strategies and
 * concurrency control. This class serves as a bridge between the FlowGraphBuilder and
 * ParallelAgent's merge strategies.
 */
public class EnhancedParallelResultAggregator implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(EnhancedParallelResultAggregator.class);

	/**
	 * Prefix for system/internal keys. Keys starting with this prefix are automatically
	 * filtered out during extra state propagation, so new framework-internal keys only
	 * need to follow this naming convention instead of being added to a hardcoded set.
	 */
	private static final String SYSTEM_KEY_PREFIX = "__";

	/**
	 * System keys from sub-graph state that should not be propagated to the parent state.
	 * These are internal graph execution keys, not user data. Kept as a fallback for
	 * legacy keys that do not follow the prefix convention.
	 */
	private static final Set<String> SYSTEM_STATE_KEYS = Set.of(
			OverAllState.DEFAULT_INPUT_KEY,
			LoopStrategy.MESSAGE_KEY,
			GraphLifecycleListener.EXECUTION_ID_KEY
	);

	private final String outputKey;

	private final List<BaseAgent> subAgents;

	private final Object mergeStrategy;

	private final Integer maxConcurrency;

	/**
	 * Key strategies from hooks, controlling how extra state values are merged
	 * when the same key is written by multiple sub-agents.
	 */
	private final Map<String, KeyStrategy> keyStrategies;

	public EnhancedParallelResultAggregator(String outputKey, List<BaseAgent> subAgents, Object mergeStrategy,
											Integer maxConcurrency) {
		this(outputKey, subAgents, mergeStrategy, maxConcurrency, Map.of());
	}

	public EnhancedParallelResultAggregator(String outputKey, List<BaseAgent> subAgents, Object mergeStrategy,
											Integer maxConcurrency, Map<String, KeyStrategy> keyStrategies) {
		this.outputKey = outputKey;
		this.subAgents = subAgents;
		this.mergeStrategy = mergeStrategy != null ? mergeStrategy : KeyStrategy.REPLACE;
		this.maxConcurrency = maxConcurrency;
		this.keyStrategies = keyStrategies != null ? keyStrategies : Map.of();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.debug("Starting enhanced result aggregation for {} sub-agents", subAgents.size());

		Map<String, Object> result = new HashMap<>();
		Map<String, Object> subAgentResults = new HashMap<>();
		// Collect extra state data written by tools via ToolContext in sub-agent graphs
		Map<String, Object> extraStateFromSubAgents = new HashMap<>();

		// Collect all sub-agent output keys for filtering extra state
		Set<String> allOutputKeys = new HashSet<>();
		for (BaseAgent subAgent : subAgents) {
			if (subAgent.getOutputKey() != null) {
				allOutputKeys.add(subAgent.getOutputKey());
			}
		}

		// Collect results from all sub-agents
		for (BaseAgent subAgent : subAgents) {
			String subAgentOutputKey = subAgent.getOutputKey();
			if (subAgentOutputKey != null) {
				Optional<Object> agentResult = state.value(subAgentOutputKey);
				if (agentResult.isPresent()) {
					if (agentResult.get() instanceof GraphResponse<?> graphResponse) {
						if (graphResponse.resultValue().isPresent() && graphResponse.resultValue().get() instanceof Map subGraphState) {
							subAgentResults.put(subAgentOutputKey, subGraphState.get(subAgentOutputKey));
							// Extract extra state data from sub-graph (e.g., data written via ToolContext)
							extractExtraStateFromSubGraph(subGraphState, allOutputKeys, extraStateFromSubAgents);
						} else {
							subAgentResults.put(subAgentOutputKey, graphResponse.resultValue().get());
						}
					} else {
						subAgentResults.put(subAgentOutputKey, agentResult.get());
					}
					logger.debug("Collected result from {}: {} = {}", subAgent.name(), subAgentOutputKey,
							agentResult.get());
				}
				else {
					logger.warn("No output found for sub-agent: {} (outputKey: {})", subAgent.name(),
							subAgentOutputKey);
				}
			}
		}

		// Apply merge strategy if provided
		Object finalResult;
		if (mergeStrategy instanceof ParallelAgent.MergeStrategy strategy) {
			finalResult = strategy.merge(subAgentResults, state);
		}
		else {
			// Default behavior: return all results as a map
			finalResult = new HashMap<>(subAgentResults);
		}

		// Only add the merged result if outputKey is not null
		if (outputKey != null && !outputKey.trim().isEmpty()) {
			result.put(outputKey, finalResult);
			logger.debug("Enhanced result aggregation completed. Final result stored under key: {}", outputKey);
		} else {
			logger.debug("Enhanced result aggregation completed. No outputKey specified, skipping merged result storage.");
		}

		Map<String, KeyStrategy> releaseStrategyResults = subAgentResults.entrySet().stream()
				.collect(HashMap::new,
						(map, entry) -> map.put(entry.getKey(), KeyStrategy.REPLACE),
						HashMap::putAll);

		state.updateStateWithKeyStrategies(subAgentResults, releaseStrategyResults);

		result.putAll(subAgentResults);

		// Propagate extra state data from sub-agents to parent state
		if (!extraStateFromSubAgents.isEmpty()) {
			logger.debug("Propagating {} extra state entries from sub-agents: {}",
					extraStateFromSubAgents.size(), extraStateFromSubAgents.keySet());
			result.putAll(extraStateFromSubAgents);
		}

		return result;
	}

	/**
	 * Extracts extra state data from a sub-graph's final state map.
	 * This captures data written by tools via ToolContext (e.g., AGENT_STATE_FOR_UPDATE_CONTEXT_KEY)
	 * that would otherwise be lost during result aggregation.
	 *
	 * <p>System keys are filtered by both a prefix convention ({@value SYSTEM_KEY_PREFIX})
	 * and a hardcoded fallback set ({@link #SYSTEM_STATE_KEYS}). When multiple sub-agents
	 * write the same extra key, the configured {@link KeyStrategy} (from hooks) is used to
	 * merge values; if no strategy is found, {@link KeyStrategy#REPLACE} is applied and a
	 * warning is logged.</p>
	 *
	 * @param subGraphState the sub-graph's complete final state map
	 * @param outputKeys all known sub-agent output keys to exclude
	 * @param extraState the map to collect extra state entries into
	 */
	private void extractExtraStateFromSubGraph(Map<String, Object> subGraphState,
											   Set<String> outputKeys,
											   Map<String, Object> extraState) {
		for (Map.Entry<String, Object> entry : subGraphState.entrySet()) {
			String key = entry.getKey();
			if (isSystemKey(key) || outputKeys.contains(key)) {
				continue;
			}
			if (extraState.containsKey(key)) {
				Object oldValue = extraState.get(key);
				Object newValue = entry.getValue();
				KeyStrategy strategy = keyStrategies.getOrDefault(key, KeyStrategy.REPLACE);
				Object mergedValue = strategy.apply(oldValue, newValue);
				extraState.put(key, mergedValue);
				logger.warn("Extra state key collision detected: key='{}', "
						+ "oldValue='{}', newValue='{}', strategy={}, mergedValue='{}'",
						key, oldValue, newValue, strategy.getClass().getSimpleName(), mergedValue);
			}
			else {
				extraState.put(key, entry.getValue());
				logger.debug("Extracted extra state from sub-graph: {} = {}", key, entry.getValue());
			}
		}
	}

	/**
	 * Determines whether a key is a system/internal key that should not be propagated.
	 * A key is considered internal if it starts with the system prefix ({@value SYSTEM_KEY_PREFIX})
	 * or is present in the hardcoded {@link #SYSTEM_STATE_KEYS} fallback set.
	 */
	private static boolean isSystemKey(String key) {
		return key.startsWith(SYSTEM_KEY_PREFIX) || SYSTEM_STATE_KEYS.contains(key);
	}

}
