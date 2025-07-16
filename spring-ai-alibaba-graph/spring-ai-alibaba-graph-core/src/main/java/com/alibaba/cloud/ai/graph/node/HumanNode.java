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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.RunnableErrors;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HumanNode implements NodeAction {

	// always or conditioned
	private String interruptStrategy;

	private Function<OverAllState, Boolean> interruptCondition;

	private Function<OverAllState, Map<String, Object>> stateUpdateFunc;

	public HumanNode() {
		this.interruptStrategy = "always";
	}

	public HumanNode(String interruptStrategy, Function<OverAllState, Boolean> interruptCondition) {
		this.interruptStrategy = interruptStrategy;
		this.interruptCondition = interruptCondition;
	}

	public HumanNode(String interruptStrategy, Function<OverAllState, Boolean> interruptCondition,
			Function<OverAllState, Map<String, Object>> stateUpdateFunc) {
		this.interruptStrategy = interruptStrategy;
		this.interruptCondition = interruptCondition;
		this.stateUpdateFunc = stateUpdateFunc;
	}

	//
	@Override
	public Map<String, Object> apply(OverAllState state) throws GraphRunnerException {
		var shouldInterrupt = interruptStrategy.equals("always")
				|| (interruptStrategy.equals("conditioned") && interruptCondition.apply(state));
		if (shouldInterrupt) {
			interrupt(state);
			Map<String, Object> data = Map.of();
			if (state.humanFeedback() != null) {
				if (stateUpdateFunc != null) {
					data = stateUpdateFunc.apply(state);
				}
				else {
					// check and only update keys defined in state.
					data = state.humanFeedback().data();
					Map<String, Object> filtered = data.entrySet()
						.stream()
						.filter(e -> state.value(e.getKey()).isPresent())
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
					data = state.updateState(filtered);
				}
			}

			state.withoutResume();
			return data;
		}
		return Map.of();
	}

	private void interrupt(OverAllState state) throws GraphRunnerException {
		if (state.humanFeedback() == null || !state.isResume()) {
			throw RunnableErrors.subGraphInterrupt.exception("interrupt");
		}
	}

	public String think(OverAllState state) {
		return state.humanFeedback().nextNodeId();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String interruptStrategy = "always";

		private Function<OverAllState, Boolean> interruptCondition = state -> true;

		private Function<OverAllState, Map<String, Object>> stateUpdateFunc = null;

		public Builder interruptStrategy(String interruptStrategy) {
			this.interruptStrategy = interruptStrategy;
			return this;
		}

		public Builder interruptCondition(Function<OverAllState, Boolean> interruptCondition) {
			this.interruptCondition = interruptCondition;
			return this;
		}

		public Builder stateUpdateFunc(Function<OverAllState, Map<String, Object>> stateUpdateFunc) {
			this.stateUpdateFunc = stateUpdateFunc;
			return this;
		}

		public HumanNode build() {
			return new HumanNode(interruptStrategy, interruptCondition, stateUpdateFunc);
		}

	}

}
