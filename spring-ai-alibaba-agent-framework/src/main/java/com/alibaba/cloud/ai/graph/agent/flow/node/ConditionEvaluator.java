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
 * A node action that evaluates conditions to determine the next execution path. This
 * class examines the state and sets a condition flag for routing decisions.
 */
public class ConditionEvaluator implements NodeAction {

	private static final String CONDITION_KEY = "_condition_result";

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Map<String, Object> updatedState = new HashMap<>();

		// Default condition evaluation logic
		// This can be extended to support custom condition evaluation
		String conditionResult = evaluateCondition(state);
		updatedState.put(CONDITION_KEY, conditionResult);

		return updatedState;
	}

	/**
	 * Evaluates the condition based on the current state. Override this method to
	 * implement custom condition logic.
	 * @param state the current state
	 * @return the condition result string
	 */
	protected String evaluateCondition(OverAllState state) {
		// Simple example: check if input contains certain keywords
		String input = state.value("input", "").toString().toLowerCase();

		if (input.contains("error") || input.contains("exception")) {
			return "error_handling";
		}
		else if (input.contains("data") || input.contains("analyze")) {
			return "data_processing";
		}
		else if (input.contains("report") || input.contains("summary")) {
			return "report_generation";
		}
		else {
			return "default";
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		public ConditionEvaluator build() {
			return new ConditionEvaluator();
		}

	}

}
