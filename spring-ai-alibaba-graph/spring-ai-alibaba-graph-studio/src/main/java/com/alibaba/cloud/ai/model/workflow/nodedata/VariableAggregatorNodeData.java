/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/***
 *
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
@NoArgsConstructor
@Data
public class VariableAggregatorNodeData extends NodeData {

	private List<List<String>> variables;

	private String outputType;

	private AdvancedSettings advancedSettings;

	@Builder
	public VariableAggregatorNodeData(List<VariableSelector> inputs, List<Variable> outputs,
			List<List<String>> variables, String outputType, AdvancedSettings advancedSettings) {
		super(inputs, outputs);
		this.variables = variables;
		this.outputType = outputType;
		this.advancedSettings = advancedSettings;
	}

	@Data
	public static class Groups {

		private String outputType;

		private List<List<String>> variables;

		private String groupName;

		private String groupId;

	}

	@Data
	public static class AdvancedSettings {

		private boolean groupEnabled;

		private List<Groups> groups;

	}

}
