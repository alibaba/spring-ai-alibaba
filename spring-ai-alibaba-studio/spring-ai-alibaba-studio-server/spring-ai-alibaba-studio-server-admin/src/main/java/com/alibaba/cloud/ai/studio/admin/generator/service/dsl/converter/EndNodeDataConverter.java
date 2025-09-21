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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.EndNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

@Component
public class EndNodeDataConverter extends AbstractNodeDataConverter<EndNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.END.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<EndNodeData>> getDialectConverters() {
		return Stream.of(EndNodeDialectConverter.values()).map(EndNodeDialectConverter::dialectConverter).toList();
	}

	private enum EndNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public EndNodeData parse(Map<String, Object> data) {
				List<Map<String, Object>> outputsMap = (List<Map<String, Object>>) data.get("outputs");
				List<VariableSelector> inputs = outputsMap.stream().map(output -> {
					List<String> valueSelector = (List<String>) output.get("value_selector");
					String variable = (String) output.get("variable");
					return new VariableSelector(valueSelector.get(0), valueSelector.get(1)).setLabel(variable);
				}).toList();
				List<Variable> outputs = List.of();
				return new EndNodeData(inputs, outputs);
			}

			@Override
			public Map<String, Object> dump(EndNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				List<Map<String, Object>> outputsMap = nodeData.getInputs()
					.stream()
					.map(input -> Map.of("value_selector", List.of(input.getNamespace(), input.getName()), "variable",
							input.getLabel()))
					.toList();
				data.put("outputs", outputsMap);
				return data;
			}
		}), STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public EndNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				EndNodeData nodeData = new EndNodeData();

				String outputType = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"output_type");
				nodeData.setOutputType(outputType);
				String textTemplate = MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param",
						"text_template");
				nodeData.setTextTemplate(textTemplate);

				// 获取输出键
				List<?> jsonParams = MapReadUtil.getMapDeepValue(data, List.class, "config", "node_param",
						"json_params");
				// 转换为VariableSelector
				List<VariableSelector> variableSelectors = Stream.ofNullable(jsonParams)
					.flatMap(List::stream)
					.map(MapReadUtil::safeCastToMapWithStringKey)
					.map(mp -> {
						String label = MapReadUtil.getMapDeepValue(mp, String.class, "key");
						String inputParam = MapReadUtil.getMapDeepValue(mp, String.class, "value");
						String nameSpace = "";
						String name = "";

						Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\.(\\w+)\\}");
						Matcher matcher = pattern.matcher(Optional.ofNullable(inputParam).orElse(""));
						if (matcher.find()) {
							nameSpace = matcher.group(1);
							name = matcher.group(2);
						}

						return new VariableSelector(nameSpace, name, label);
					})
					.toList();
				nodeData.setInputs(variableSelectors);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(EndNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(EndNodeData.class));

		private final DialectConverter<EndNodeData> dialectConverter;

		public DialectConverter<EndNodeData> dialectConverter() {
			return dialectConverter;
		}

		EndNodeDialectConverter(DialectConverter<EndNodeData> converter) {
			this.dialectConverter = converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "end" + count;
	}

	@Override
	public BiConsumer<EndNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case STUDIO -> emptyProcessConsumer().andThen((data, idToVarName) -> {
				// 格式化textTemplate
				data.setTextTemplate(this.convertVarTemplate(dialectType, data.getTextTemplate(), idToVarName));
				// 设置输出键
				String outputKey = data.getVarName() + "_" + EndNodeData.getDefaultOutputSchema().getName();
				data.setOutputKey(outputKey);
				data.setOutputs(List.of(EndNodeData.getDefaultOutputSchema()));
			}).andThen(super.postProcessConsumer(dialectType));
			case DIFY -> emptyProcessConsumer().andThen((data, idToVarName) -> {
				String outputKey = data.getVarName() + "_" + EndNodeData.getDefaultOutputSchema().getName();
				data.setOutputKey(outputKey);
				data.setOutputs(List.of(EndNodeData.getDefaultOutputSchema()));
			}).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
