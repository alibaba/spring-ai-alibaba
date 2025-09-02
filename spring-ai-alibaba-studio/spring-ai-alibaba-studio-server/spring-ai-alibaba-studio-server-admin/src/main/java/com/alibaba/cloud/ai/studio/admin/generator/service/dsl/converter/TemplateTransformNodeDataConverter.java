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
package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.TemplateTransformNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import org.springframework.stereotype.Component;

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

				List<Variable> outputs = List.of(new Variable("result", VariableType.STRING));

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
					outputVars.put(variable.getName(), Map.of("type", variable.getValueType().difyValue()));
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

	private String replacePlaceholders(String text, Map<String, String> data) {
		Pattern pattern = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");
		Matcher matcher = pattern.matcher(text);

		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String key = matcher.group(1);
			String value = data.get(key);

			if (value != null) {
				String replacement = "{{ " + value + " }}";
				matcher.appendReplacement(result, replacement);
			}
		}
		matcher.appendTail(result);
		return result.toString();
	}

	@Override
	public BiConsumer<TemplateTransformNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				nodeData.setOutputKey(
						nodeData.getVarName() + "_" + TemplateTransformNodeData.getDefaultOutputSchema().getName());
				nodeData.setOutputs(List.of(TemplateTransformNodeData.getDefaultOutputSchema()));
			}).andThen(super.postProcessConsumer(dialectType)).andThen((nodeData, idToVarName) -> {
				// todo: 支持Jinja2的if、for语句
				// 将模板中的占位变量替换为工作流的中间变量
				Map<String, String> argToStateName = nodeData.getInputs()
					.stream()
					.collect(Collectors.toMap(VariableSelector::getLabel, VariableSelector::getNameInCode));
				nodeData.setTemplate(this.replacePlaceholders(nodeData.getTemplate(), argToStateName));
			});
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
