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
import com.alibaba.cloud.ai.graph.plugin.GraphPlugin;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Node that executes a GraphPlugin.
 */
public class PluginNode implements NodeAction {

	private final GraphPlugin plugin;

	private final String paramsKey;

	private final String outputKey;

	private PluginNode(Builder builder) {
		this.plugin = builder.plugin;
		this.paramsKey = builder.paramsKey;
		this.outputKey = builder.outputKey;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		// Get parameters from state if paramsKey is set
		Map<String, Object> params = new HashMap<>();
		if (StringUtils.hasLength(paramsKey)) {
			params = (Map<String, Object>) state.value(paramsKey).orElse(new HashMap<>());
		}

		// Execute the plugin
		Map<String, Object> result = plugin.execute(params);

		// Update state with result
		Map<String, Object> updatedState = new HashMap<>();
		updatedState.put("result", result);
		if (StringUtils.hasLength(outputKey)) {
			updatedState.put(outputKey, result);
		}
		return updatedState;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private GraphPlugin plugin;

		private String paramsKey;

		private String outputKey;

		private Builder() {
		}

		public Builder plugin(GraphPlugin plugin) {
			this.plugin = plugin;
			return this;
		}

		public Builder paramsKey(String paramsKey) {
			this.paramsKey = paramsKey;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public PluginNode build() {
			if (plugin == null) {
				throw new IllegalArgumentException("Plugin must be set");
			}
			return new PluginNode(this);
		}

	}

}
