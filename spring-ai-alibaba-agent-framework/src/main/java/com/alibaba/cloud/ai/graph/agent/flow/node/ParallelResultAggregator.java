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

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

/**
 * A node action that aggregates results from parallel execution of multiple agents. This
 * class collects outputs from parallel agents and combines them into a single result.
 */
public class ParallelResultAggregator implements NodeAction {

	private final String outputKey;

	public ParallelResultAggregator(String outputKey) {
		this.outputKey = outputKey;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Map<String, Object> updatedState = new HashMap<>();

		// Collect all agent results
		Map<String, Object> aggregatedResults = new HashMap<>();

		// Iterate through all state data to find agent outputs
		Map<String, Object> stateData = state.data();
		for (Map.Entry<String, Object> entry : stateData.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value != null && !key.equals("messages")) { // Skip system keys like
				// messages
				aggregatedResults.put(key, value);
			}
		}

		// Create a summary or combined result
		StringBuilder combinedResult = new StringBuilder();
		combinedResult.append("Parallel execution results:\n");

		for (Map.Entry<String, Object> entry : aggregatedResults.entrySet()) {
			combinedResult.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
		}

		updatedState.put(this.outputKey, combinedResult.toString());
		return updatedState;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String outputKey;

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public ParallelResultAggregator build() {
			if (outputKey == null || outputKey.trim().isEmpty()) {
				throw new IllegalArgumentException("outputKey must not be null or empty");
			}
			return new ParallelResultAggregator(outputKey);
		}

	}

}
