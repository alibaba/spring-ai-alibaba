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

package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.CodeNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Component
public class CodeNodeDataConverter extends AbstractNodeDataConverter<CodeNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.CODE.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<CodeNodeData>> getDialectConverters() {
		return Stream.of(CodeNodeDialectConverter.values()).map(CodeNodeDialectConverter::dialectConverter).toList();
	}

	private enum CodeNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public CodeNodeData parse(Map<String, Object> data) {
				List<Map<String, Object>> variables = (List<Map<String, Object>>) data.get("variables");
				List<VariableSelector> inputs = variables.stream().map(variable -> {
					List<String> selector = (List<String>) variable.get("value_selector");
					return new VariableSelector(selector.get(0), selector.get(1), (String) variable.get("variable"));
				}).toList();
				Map<String, Map<String, Object>> outputsMap = (Map<String, Map<String, Object>>) data.get("outputs");
				List<Variable> outputs = outputsMap.entrySet().stream().map(entry -> {
					String varName = entry.getKey();
					String difyType = (String) entry.getValue().get("type");
					VariableType varType = VariableType.fromDifyValue(difyType)
						.orElseThrow(() -> new IllegalArgumentException("Unsupported dify variable type: " + difyType));
					return new Variable(varName, varType.value());
				}).toList();

				return new CodeNodeData(inputs, outputs).setCode((String) data.get("code"))
					.setCodeLanguage((String) data.get("code_language"));
			}

			@Override
			public Map<String, Object> dump(CodeNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				data.put("code", nodeData.getCode());
				data.put("code_language", nodeData.getCodeLanguage());
				List<Map<String, Object>> inputVars = new ArrayList<>();
				nodeData.getInputs().forEach(v -> {
					inputVars.add(
							Map.of("variable", v.getLabel(), "value_selector", List.of(v.getNamespace(), v.getName())));
				});
				data.put("variables", inputVars);
				Map<String, Object> outputVars = new HashMap<>();
				nodeData.getOutputs().forEach(variable -> {
					outputVars.put(variable.getName(), Map.of("type",
							VariableType.fromValue(variable.getValueType()).orElse(VariableType.OBJECT).difyValue()));
				});
				data.put("outputs", outputVars);
				return data;
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(CodeNodeData.class));

		private final DialectConverter<CodeNodeData> dialectConverter;

		public DialectConverter<CodeNodeData> dialectConverter() {
			return dialectConverter;
		}

		CodeNodeDialectConverter(DialectConverter<CodeNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

}
