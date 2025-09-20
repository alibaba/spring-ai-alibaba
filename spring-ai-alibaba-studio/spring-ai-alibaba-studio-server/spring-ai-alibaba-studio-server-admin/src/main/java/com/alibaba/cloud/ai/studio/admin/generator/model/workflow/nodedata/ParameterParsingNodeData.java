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
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParameterParsingNodeData extends NodeData {

	public static List<Variable> getDefaultOutputSchema(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> List.of(new Variable("__is_success", VariableType.BOOLEAN),
					new Variable("__reason", VariableType.STRING));
			case STUDIO -> List.of(new Variable("_is_completed", VariableType.BOOLEAN),
					new Variable("_reason", VariableType.STRING));
			default -> List.of();
		};
	}

	private VariableSelector inputSelector;

	private String chatModeName;

	private Map<String, Object> modeParams;

	private List<Param> parameters;

	private String instruction;

	private String successKey = "success";

	private String dataKey = "data";

	private String reasonKey = "reason";

	public record Param(String name, VariableType type, String description) {
		@Override
		public String toString() {
			return String.format("ParameterParsingNode.param(%s, %s, %s)", ObjectToCodeUtil.toCode(name()),
					ObjectToCodeUtil.toCode(Optional.ofNullable(type()).orElse(VariableType.STRING).value()),
					ObjectToCodeUtil.toCode(description()));
		}
	}

	public VariableSelector getInputSelector() {
		return inputSelector;
	}

	public void setInputSelector(VariableSelector inputSelector) {
		this.inputSelector = inputSelector;
	}

	public String getChatModeName() {
		return chatModeName;
	}

	public void setChatModeName(String chatModeName) {
		this.chatModeName = chatModeName;
	}

	public Map<String, Object> getModeParams() {
		return modeParams;
	}

	public void setModeParams(Map<String, Object> modeParams) {
		this.modeParams = modeParams;
	}

	public List<Param> getParameters() {
		return parameters;
	}

	public void setParameters(List<Param> parameters) {
		this.parameters = parameters;
	}

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}

	public String getSuccessKey() {
		return successKey;
	}

	public void setSuccessKey(String successKey) {
		this.successKey = successKey;
	}

	public String getDataKey() {
		return dataKey;
	}

	public void setDataKey(String dataKey) {
		this.dataKey = dataKey;
	}

	public String getReasonKey() {
		return reasonKey;
	}

	public void setReasonKey(String reasonKey) {
		this.reasonKey = reasonKey;
	}

}
