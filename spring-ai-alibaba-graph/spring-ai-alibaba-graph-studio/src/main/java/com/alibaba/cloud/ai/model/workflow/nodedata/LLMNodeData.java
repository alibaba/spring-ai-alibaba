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
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
@NoArgsConstructor
@Data
public class LLMNodeData extends NodeData {

	public static final Variable DEFAULT_OUTPUT_SCHEMA = new Variable("text", VariableType.STRING.value());

	private ModelConfig model;

	private List<PromptTemplate> promptTemplate;

	private MemoryConfig memoryConfig;

	public LLMNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class PromptTemplate {

		private String role;

		private String text;

	}

	@Data
	public static class ModelConfig {

		public static final String MODE_COMPLETION = "completion";

		public static final String MODE_CHAT = "chat";

		private String mode;

		private String name;

		private String provider;

		private CompletionParams completionParams;

	}

	@Data
	public static class CompletionParams {

		private Integer maxTokens;

		private Float repetitionPenalty;

		private String responseFormat;

		private Integer seed;

		private List<String> stop;

		private Float temperature;

		private Float topP;

		private Integer topK;

	}

	@Data
	@Accessors(chain = true)
	public static class MemoryConfig {

		private Boolean enabled = false;

		private Integer windowSize = 20;

		private Boolean windowEnabled = true;

		private Boolean includeLastMessage = false;

		private String lastMessageTemplate;

	}

}
