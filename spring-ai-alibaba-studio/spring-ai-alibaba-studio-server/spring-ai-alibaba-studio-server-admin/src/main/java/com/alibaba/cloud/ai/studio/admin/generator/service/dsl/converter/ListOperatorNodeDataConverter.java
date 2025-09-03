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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.ListOperatorNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import org.springframework.stereotype.Component;

/**
 * Convert the ListOperatorNode configuration in the Dify DSL to and from the
 * ListOperatorNodeData object.
 */
@Component
public class ListOperatorNodeDataConverter extends AbstractNodeDataConverter<ListOperatorNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.LIST_OPERATOR.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<ListOperatorNodeData>> getDialectConverters() {
		return Stream.of(ListOperatorNodeConverter.values())
			.map(ListOperatorNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum ListOperatorNodeConverter {

		DIFY(new DialectConverter<>() {
			@SuppressWarnings("unchecked")
			@Override
			public ListOperatorNodeData parse(Map<String, Object> data) {
				ListOperatorNodeData nd = new ListOperatorNodeData();

				// inputs => variable
				List<String> sel = (List<String>) data.get("variable");
				if (sel != null && sel.size() == 2) {
					nd.setInputs(Collections.singletonList(new VariableSelector(sel.get(0), sel.get(1))));
				}

				// 过滤条件
				Object filterObj = data.get("filter_by");
				if (filterObj instanceof Map<?, ?>) {
					Map<String, Object> filterMap = (Map<String, Object>) filterObj;
					Boolean enabled = (Boolean) filterMap.getOrDefault("enabled", false);
					if (enabled) {
						Object conditionObj = filterMap.get("conditions");
						if (conditionObj instanceof List<?>) {
							List<Map<String, Object>> conditionList = (List<Map<String, Object>>) conditionObj;
							List<ListOperatorNodeData.FilterCondition> filterConditions = conditionList.stream()
								.map(mp -> ListOperatorNodeData.FilterCondition
									.ofDify(mp.get("comparison_operator").toString(), mp.get("value").toString()))
								.toList();
							nd.setFilters(filterConditions);
						}
					}
				}

				// 限制数量
				Object limitObj = data.get("limit");
				if (limitObj instanceof Map<?, ?>) {
					Map<String, Object> limitMap = (Map<String, Object>) limitObj;
					Boolean enabled = (Boolean) limitMap.getOrDefault("enabled", false);
					if (enabled) {
						Integer size = (Integer) limitMap.get("size");
						nd.setLimitNumber(size);
					}
				}

				// 排序规则
				Object orderObj = data.get("order_by");
				if (orderObj instanceof Map<?, ?>) {
					Map<String, Object> orderMap = (Map<String, Object>) orderObj;
					Boolean enabled = (Boolean) orderMap.getOrDefault("enabled", false);
					if (enabled) {
						String value = orderMap.get("value").toString();
						if (value.equalsIgnoreCase("asc")) {
							nd.setOrder(ListOperatorNodeData.Ordered.ASC);
						}
						else if (value.equalsIgnoreCase("desc")) {
							nd.setOrder(ListOperatorNodeData.Ordered.DESC);
						}
					}
				}

				// element_class_type
				nd.setElementClassType(
						VariableType.fromDifyValue((String) data.get("item_var_type")).orElse(VariableType.OBJECT));

				return nd;
			}

			@Override
			public Map<String, Object> dump(ListOperatorNodeData nd) {
				Map<String, Object> m = new LinkedHashMap<>();

				return m;
			}

			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}
		}), CUSTOM(defaultCustomDialectConverter(ListOperatorNodeData.class));

		private final DialectConverter<ListOperatorNodeData> converter;

		ListOperatorNodeConverter(DialectConverter<ListOperatorNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<ListOperatorNodeData> dialectConverter() {
			return converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "listOperatorNode" + count;
	}

	@Override
	public BiConsumer<ListOperatorNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				Variable output = ListOperatorNodeData.defaultOutputSchema();
				nodeData.setOutputKey(nodeData.getVarName() + "_" + output.getName());
				nodeData.setOutputs(List.of(output));
			})
				.andThen(super.postProcessConsumer(dialectType))
				.andThen((nodeData, idToVarName) -> nodeData.setInputKey(nodeData.getInputs().get(0).getNameInCode()));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
