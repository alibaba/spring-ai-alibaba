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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.VariableAggregatorNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class VariableAggregatorNodeDataConverter extends AbstractNodeDataConverter<VariableAggregatorNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.AGGREGATOR.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<VariableAggregatorNodeData>> getDialectConverters() {
		return Arrays.stream(AggregatorNodeDialectConverter.values())
			.map(AggregatorNodeDialectConverter::dialectConverter)
			.toList();
	}

	private enum AggregatorNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@SuppressWarnings("unchecked")
			@Override
			public VariableAggregatorNodeData parse(Map<String, Object> data) {
				VariableAggregatorNodeData.AdvancedSettings adv;
				Object advRaw = data.get("advanced_settings");
				if (advRaw instanceof Map<?, ?>) {
					Map<String, Object> map = (Map<String, Object>) advRaw;
					adv = new VariableAggregatorNodeData.AdvancedSettings();
					adv.setGroupEnabled(Boolean.TRUE.equals(map.get("group_enabled")));
					Object groupsRaw = map.get("groups");
					if (groupsRaw instanceof List<?>) {
						try {
							ObjectMapper om = new ObjectMapper();
							String json = om.writeValueAsString(groupsRaw);
							var list = om.readValue(json, new TypeReference<List<VariableAggregatorNodeData.Groups>>() {
							});
							adv.setGroups(list);
						}
						catch (JsonProcessingException e) {
							throw new RuntimeException("Failed to parse 'advanced_settings.groups' JSON", e);
						}
					}
					else {
						adv.setGroups(Collections.emptyList());
					}
				}
				else {
					adv = new VariableAggregatorNodeData.AdvancedSettings().setGroupEnabled(false)
						.setGroups(Collections.emptyList());
				}

				List<List<String>> vars = Collections.emptyList();
				Object varRaw = data.get("variables");
				if (varRaw instanceof List<?>) {
					vars = (List<List<String>>) varRaw;
				}

				String outputTypeStr = (String) data.get("output_type");
				VariableType outputType = VariableType.fromDifyValue(outputTypeStr).orElse(VariableType.OBJECT);
				List<VariableSelector> inputs = Collections.emptyList();
				List<Variable> outputs = new ArrayList<>();
				VariableAggregatorNodeData variableAggregatorNodeData = new VariableAggregatorNodeData(inputs, outputs,
						vars, outputType, adv);
				if (data.containsKey("output_key")) {
					variableAggregatorNodeData.setOutputKey((String) data.get("output_key"));
				}
				outputs.add(new Variable(variableAggregatorNodeData.getOutputKey(),
						variableAggregatorNodeData.getOutputType()));
				return variableAggregatorNodeData;
			}

			@Override
			public Map<String, Object> dump(VariableAggregatorNodeData nd) {
				Map<String, Object> result = new LinkedHashMap<>();
				result.put("variables", nd.getVariables());
				if (nd.getOutputType() != null) {
					result.put("output_type", nd.getOutputType());
				}
				var adv = nd.getAdvancedSettings();
				if (adv != null) {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("group_enabled", adv.isGroupEnabled());
					List<Map<String, Object>> gos = new ArrayList<>();
					for (var g : adv.getGroups()) {
						Map<String, Object> gm = new LinkedHashMap<>();
						gm.put("group_name", g.getGroupName());
						gm.put("groupId", g.getGroupId());
						gm.put("output_type", g.getOutputType());
						gm.put("variables", g.getVariables());
						gos.add(gm);
					}
					m.put("groups", gos);
					result.put("advanced_settings", m);
				}
				return result;
			}
		}),

		CUSTOM(defaultCustomDialectConverter(VariableAggregatorNodeData.class));

		private final DialectConverter<VariableAggregatorNodeData> converter;

		AggregatorNodeDialectConverter(DialectConverter<VariableAggregatorNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<VariableAggregatorNodeData> dialectConverter() {
			return this.converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "variableAggregatorNode" + count;
	}

	@Override
	public BiConsumer<VariableAggregatorNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> {
				// 设置输入变量的Selector
				BiConsumer<VariableAggregatorNodeData, Map<String, String>> consumer = ((nodeData, idToVarName) -> {
					// 设置输出变量
					if (nodeData.getAdvancedSettings() != null && nodeData.getAdvancedSettings().isGroupEnabled()) {
						List<Variable> outputs = nodeData.getAdvancedSettings()
							.getGroups()
							.stream()
							.map(group -> new Variable(group.getGroupName(),
									VariableType.fromDifyValue(group.getOutputType()).orElse(VariableType.OBJECT)))
							.toList();
						nodeData.setOutputs(outputs);
					}
					else {
						Variable output = new Variable("output", nodeData.getOutputType());
						nodeData.setOutputs(List.of(output));
					}
					nodeData.setOutputKey(nodeData.getVarName() + "_output");

					// 设置输入变量
					List<VariableSelector> selectors;
					if (nodeData.getAdvancedSettings() != null && nodeData.getAdvancedSettings().isGroupEnabled()) {
						selectors = nodeData.getAdvancedSettings()
							.getGroups()
							.stream()
							.map(VariableAggregatorNodeData.Groups::getVariables)
							.flatMap(List::stream)
							.map(list -> new VariableSelector(list.get(0), list.get(1)))
							.toList();
						// 设置Group自己的Selector
						nodeData.getAdvancedSettings().getGroups().forEach(group -> {
							group.setVariableSelectors(group.getVariables()
								.stream()
								.map(list -> new VariableSelector(list.get(0), list.get(1)).setNameInCode(
										idToVarName.getOrDefault(list.get(0), list.get(0)) + "_" + list.get(1)))
								.toList());
						});
					}
					else {
						selectors = nodeData.getVariables()
							.stream()
							.map(variable -> new VariableSelector(variable.get(0), variable.get(1)))
							.toList();
					}
					nodeData.setInputs(selectors);
				});
				yield consumer.andThen(super.postProcessConsumer(dialectType));
			}
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
