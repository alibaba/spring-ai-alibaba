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
package com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata;

import java.util.Collections;
import java.util.List;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;

public class AnswerNodeData extends NodeData {

	public static List<Variable> getDefaultOutputs() {
		return List.of(new Variable("answer", VariableType.STRING));
	}

	// a string template
	private String answer;

	private String outputKey;

	public AnswerNodeData() {
		super(Collections.emptyList(), Collections.emptyList());
	}

	public AnswerNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public String getAnswer() {
		return answer;
	}

	public AnswerNodeData setAnswer(String answer) {
		this.answer = answer;
		return this;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

}
