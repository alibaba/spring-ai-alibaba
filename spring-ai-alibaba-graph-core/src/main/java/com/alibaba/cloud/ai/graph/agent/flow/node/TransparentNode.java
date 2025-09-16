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
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import org.springframework.util.StringUtils;

public class TransparentNode implements NodeAction {

	private final List<String> inputKeys;

	private final String outputKey;

	public TransparentNode(String outputKey, List<String> inputKeys) {
		this.inputKeys = inputKeys;
		this.outputKey = outputKey;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Map<String, Object> updatedState = new HashMap<>();
		Object value = state.value(inputKeys.get(0))
			.orElseThrow(
					() -> new IllegalArgumentException("Input key '" + inputKeys.get(0) + "' not found in state: " + state));
		updatedState.put(this.outputKey, value);
		return updatedState;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String outputKey;

		private List<String> inputKeys;

		public TransparentNode build() {
			return new TransparentNode(outputKey, inputKeys);
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder inputKey(List<String> inputKeys) {
			this.inputKeys = inputKeys;
			return this;
		}

	}

}
