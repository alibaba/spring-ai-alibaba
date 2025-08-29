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

import java.util.List;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;

public class EndNodeData extends NodeData {

	public static Variable getDefaultOutputSchema() {
		return new Variable("output", VariableType.ARRAY_STRING.value());
	}

	private String outputKey;

	private String outputType;

	private String textTemplate;

	public String getOutputKey() {
		return outputKey;
	}

	public EndNodeData setOutputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public String getTextTemplate() {
		return textTemplate;
	}

	public void setTextTemplate(String textTemplate) {
		this.textTemplate = textTemplate;
	}

	public EndNodeData() {
	}

	public EndNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

}
