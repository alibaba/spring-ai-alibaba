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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VariableAggregatorNode implements NodeAction {

	private final List<List<String>> variables;

	private final String outputKey;

	private final String outputType;

	private final AdvancedSettings advancedSettings;

	public VariableAggregatorNode(List<List<String>> variables, String outputKey, String outputType,
			AdvancedSettings advancedSettings) {
		this.variables = variables;
		this.outputKey = outputKey;
		this.outputType = outputType;
		this.advancedSettings = advancedSettings;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Map<String, Object> result = new HashMap<>();

		if ("group".equals(outputType) && advancedSettings != null && advancedSettings.isGroupEnabled()) {
			Map<String, Object> groupedResult = new HashMap<>();
			for (Group group : advancedSettings.getGroups()) {
				List<Object> values = new ArrayList<>();
				for (List<String> path : group.getVariables()) {
					Object value = getValueByPath(state, path);
					if (value != null) {
						values.add(value);
					}
				}
				groupedResult.put(group.getGroupName(), convertOutput(values, group.getOutputType()));
			}
			result.put(outputKey, groupedResult);
		}
		else {
			List<Object> allValues = new ArrayList<>();
			for (List<String> path : variables) {
				Object value = getValueByPath(state, path);
				if (value != null) {
					allValues.add(value);
				}
			}
			result.put(outputKey, convertOutput(allValues, outputType));
		}

		return result;
	}

	private Object getValueByPath(OverAllState state, List<String> path) {
		Object current = null;
		for (String key : path) {
			if (current == null) {
				current = state.value(key).orElse(null);
			}
			else if (current instanceof Map) {
				current = ((Map<?, ?>) current).get(key);
			}
			else {
				return null;
			}
		}
		return current;
	}

	private Object convertOutput(List<Object> values, String type) {
		switch (type) {
			case "list":
				return new ArrayList<>(values);
			case "string":
				return String.join("\n", values.stream().map(Object::toString).collect(Collectors.toList()));
			default:
				return values;
		}
	}

	public static class AdvancedSettings {

		private boolean groupEnabled;

		private List<Group> groups;

		public boolean isGroupEnabled() {
			return groupEnabled;
		}

		public AdvancedSettings setGroupEnabled(boolean groupEnabled) {
			this.groupEnabled = groupEnabled;
			return this;
		}

		public List<Group> getGroups() {
			return groups;
		}

		public AdvancedSettings setGroups(List<Group> groups) {
			this.groups = groups;
			return this;
		}

	}

	public static class Group {

		private String outputType;

		private List<List<String>> variables;

		private String groupName;

		private String groupId;

		public String getOutputType() {
			return outputType;
		}

		public void setOutputType(String outputType) {
			this.outputType = outputType;
		}

		public List<List<String>> getVariables() {
			return variables;
		}

		public void setVariables(List<List<String>> variables) {
			this.variables = variables;
		}

		public String getGroupName() {
			return groupName;
		}

		public void setGroupName(String groupName) {
			this.groupName = groupName;
		}

		public String getGroupId() {
			return groupId;
		}

		public void setGroupId(String groupId) {
			this.groupId = groupId;
		}

	}

	public static class Builder {

		private List<List<String>> variables;

		private String outputKey;

		private String outputType = "list";

		private AdvancedSettings advancedSettings;

		public Builder variables(List<List<String>> variables) {
			this.variables = variables;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder outputType(String outputType) {
			this.outputType = outputType;
			return this;
		}

		public Builder advancedSettings(AdvancedSettings advancedSettings) {
			this.advancedSettings = advancedSettings;
			return this;
		}

		public VariableAggregatorNode build() {
			return new VariableAggregatorNode(variables, outputKey, outputType, advancedSettings);
		}

	}

	public static Builder builder() {
		return new Builder();
	}

}
