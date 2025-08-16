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
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public class BranchNode implements NodeAction {

	private final String inputKey;

	private final String outputKey;

	public BranchNode(String outputKey, String inputKey) {
		if (!StringUtils.hasLength(inputKey) || !StringUtils.hasLength(outputKey)) {
			throw new IllegalArgumentException("inputKey and outputKey must not be null or empty.");
		}
		this.inputKey = inputKey;
		this.outputKey = outputKey;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Map<String, Object> updatedState = new HashMap<>();
		String value = state.value(inputKey).map(Object::toString).orElse(null);
		updatedState.put(this.outputKey, value);
		return updatedState;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String outputKey;

		private String inputKey;

		public BranchNode build() {
			return new BranchNode(outputKey, inputKey);
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder inputKey(String inputKey) {
			this.inputKey = inputKey;
			return this;
		}

	}

}
