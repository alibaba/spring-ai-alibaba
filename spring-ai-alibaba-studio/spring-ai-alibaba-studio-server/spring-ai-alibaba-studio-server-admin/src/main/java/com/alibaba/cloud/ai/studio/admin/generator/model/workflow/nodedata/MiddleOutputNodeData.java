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

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import java.util.List;

public class MiddleOutputNodeData extends NodeData {

	public static List<Variable> getDefaultOutputSchemas(DSLDialectType dialectType) {
		return switch (dialectType) {
			case STUDIO -> List.of(new Variable("output", VariableType.STRING));
			default -> List.of();
		};
	}

	private String outputTemplate;

	private List<String> varKeys;

	private String outputKey;

	public String getOutputTemplate() {
		return outputTemplate;
	}

	public void setOutputTemplate(String outputTemplate) {
		this.outputTemplate = outputTemplate;
	}

	public List<String> getVarKeys() {
		return varKeys;
	}

	public void setVarKeys(List<String> varKeys) {
		this.varKeys = varKeys;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

}
