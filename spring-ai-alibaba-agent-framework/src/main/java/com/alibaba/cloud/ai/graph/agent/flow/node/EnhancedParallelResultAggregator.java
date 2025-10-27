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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.a2a.A2aRemoteAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enhanced parallel result aggregator that supports custom merge strategies and
 * concurrency control. This class serves as a bridge between the FlowGraphBuilder and
 * ParallelAgent's merge strategies.
 */
public class EnhancedParallelResultAggregator implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(EnhancedParallelResultAggregator.class);

	private final String outputKey;

	private final List<Agent> subAgents;

	private final Object mergeStrategy;

	private final Integer maxConcurrency;

	public EnhancedParallelResultAggregator(String outputKey, List<Agent> subAgents, Object mergeStrategy,
			Integer maxConcurrency) {
		this.outputKey = outputKey;
		this.subAgents = subAgents;
		this.mergeStrategy = mergeStrategy;
		this.maxConcurrency = maxConcurrency;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.debug("Starting enhanced result aggregation for {} sub-agents", subAgents.size());

		Map<String, Object> result = new HashMap<>();
		Map<String, Object> subAgentResults = new HashMap<>();

		// Collect results from all sub-agents
		for (Agent subAgent : subAgents) {
			String subAgentOutputKey = null;
			if (subAgent instanceof ReactAgent subReactAgent) {
				subAgentOutputKey = subReactAgent.getOutputKey();
			} else if (subAgent instanceof A2aRemoteAgent remoteAgent) { // a2a remote agent
				subAgentOutputKey = remoteAgent.getOutputKey();
			}
			if (subAgentOutputKey != null) {
				Optional<Object> agentResult = state.value(subAgentOutputKey);
				if (agentResult.isPresent()) {
					if (agentResult.get() instanceof GraphResponse<?> graphResponse){
						if (graphResponse.resultValue().isPresent() && graphResponse.resultValue().get() instanceof Map  map) {
							subAgentResults.put(subAgentOutputKey, map.get(subAgentOutputKey));
						}else {
							subAgentResults.put(subAgentOutputKey, graphResponse.resultValue().get());
						}
					}else {
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

		result.put(outputKey, finalResult);

		logger.debug("Enhanced result aggregation completed. Final result stored under key: {}", outputKey);

		return result;
	}

}
