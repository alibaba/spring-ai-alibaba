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
package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.TemplateTransformNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class TemplateTransformNodeDataConverter extends AbstractNodeDataConverter<TemplateTransformNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.TEMPLATE_TRANSFORM.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<TemplateTransformNodeData>> getDialectConverters() {
		return Stream.of(TemplateTransformNodeDialectConverter.values())
			.map(TemplateTransformNodeDialectConverter::dialectConverter)
			.toList();
	}

	private enum TemplateTransformNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public TemplateTransformNodeData parse(Map<String, Object> data) {
				List<Map<String, Object>> variables = (List<Map<String, Object>>) data.get("variables");
				List<VariableSelector> inputs = new ArrayList<>();
				if (variables != null) {
					inputs = variables.stream().map(variable -> {
						List<String> selector = (List<String>) variable.get("value_selector");
						return new VariableSelector(selector.get(0), selector.get(1),
								(String) variable.get("variable"));
					}).toList();
				}

				List<Variable> outputs = List.of(new Variable("result", VariableType.STRING.value()));

				return new TemplateTransformNodeData(inputs, outputs).setTemplate((String) data.get("template"));
			}

			@Override
			public Map<String, Object> dump(TemplateTransformNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				data.put("template", nodeData.getTemplate());

				List<Map<String, Object>> inputVars = new ArrayList<>();
				nodeData.getInputs().forEach(v -> {
					inputVars.add(
							Map.of("variable", v.getLabel(), "value_selector", List.of(v.getNamespace(), v.getName())));
				});
				data.put("variables", inputVars);

				Map<String, Object> outputVars = new HashMap<>();
				nodeData.getOutputs().forEach(variable -> {
					outputVars.put(variable.getName(), Map.of("type",
							VariableType.fromValue(variable.getValueType()).orElse(VariableType.STRING).difyValue()));
				});
				data.put("outputs", outputVars);

				return data;
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(TemplateTransformNodeData.class));

		private final DialectConverter<TemplateTransformNodeData> dialectConverter;

		public DialectConverter<TemplateTransformNodeData> dialectConverter() {
			return dialectConverter;
		}

		TemplateTransformNodeDialectConverter(DialectConverter<TemplateTransformNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "templateTransformNode" + count;
	}

	@Override
	public void postProcess(TemplateTransformNodeData nodeData, String varName) {
		String origKey = nodeData.getOutputKey();
		String newKey = varName + "_output";

		if (origKey == null) {
			nodeData.setOutputKey(newKey);
		}
		nodeData.setOutputs(List.of(new Variable(nodeData.getOutputKey(), VariableType.STRING.value())));
	}

}
