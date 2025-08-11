/*
 * Copyright 2024-2026 the original author or authors.
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

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.AssignerNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Component
public class AssignerNodeDataConverter extends AbstractNodeDataConverter<AssignerNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.ASSIGNER.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<AssignerNodeData>> getDialectConverters() {
		return Stream.of(AssignerNodeDialectConverter.values())
			.map(AssignerNodeDialectConverter::dialectConverter)
			.toList();
	}

	private enum AssignerNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public AssignerNodeData parse(Map<String, Object> data) {
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

				List<Map<String, Object>> itemsList = (List<Map<String, Object>>) data.get("items");
				List<AssignerNodeData.AssignerItem> items = itemsList.stream().map(item -> {
					AssignerNodeData.AssignerItem ai = new AssignerNodeData.AssignerItem();
					ai.setInputType((String) item.get("input_type"));
					ai.setOperation((String) item.get("operation"));
					Object valueObj = item.get("value");
					if (valueObj instanceof List<?> valueList && valueList.size() >= 2) {
						ai.setValue(new VariableSelector(valueList.get(0).toString(), valueList.get(1).toString()));
					}
					Object variableObj = item.get("variable_selector");
					if (variableObj instanceof List<?> variableList && variableList.size() >= 2) {
						ai.setVariableSelector(
								new VariableSelector(variableList.get(0).toString(), variableList.get(1).toString()));
					}
					ai.setWriteMode((String) item.get("write_mode"));
					return ai;
				}).toList();

				AssignerNodeData nodeData = new AssignerNodeData();
				nodeData.setItems(items);
				nodeData.setTitle((String) data.get("title"));
				nodeData.setDesc((String) data.get("desc"));
				nodeData.setVersion((String) data.get("version"));

				return nodeData;
			}

			@Override
			public Map<String, Object> dump(AssignerNodeData nodeData) {
				Map<String, Object> dataMap = new HashMap<>();
				dataMap.put("type", "assigner");
				dataMap.put("title", nodeData.getTitle());
				dataMap.put("desc", nodeData.getDesc());
				dataMap.put("version", nodeData.getVersion());
				List<Map<String, Object>> itemsList = nodeData.getItems()
					.stream()
					.map(item -> Map.of("input_type", item.getInputType(), "operation", item.getOperation(), "value",
							item.getValue(), "variable_selector", item.getVariableSelector(), "write_mode",
							item.getWriteMode()))
					.toList();
				dataMap.put("items", itemsList);

				Map<String, Object> ret = new HashMap<>();
				ret.put("data", dataMap);
				return ret;
			}
		}), CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(AssignerNodeData.class));

		private final DialectConverter<AssignerNodeData> dialectConverter;

		public DialectConverter<AssignerNodeData> dialectConverter() {
			return dialectConverter;
		}

		AssignerNodeDialectConverter(DialectConverter<AssignerNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "assignerNode" + count;
	}

	@Override
	public void postProcessOutput(AssignerNodeData data, String varName) {
		// 赋值节点没有输出
	}

	@Override
	public BiConsumer<AssignerNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> {
				BiConsumer<AssignerNodeData, Map<String, String>> consumer = (nodeData, idToVarName) -> {
					// 将赋值的多组变量放进Inputs里，方便格式化格式
					List<VariableSelector> selectors = nodeData.getItems()
						.stream()
						.flatMap(item -> Stream.of(item.getValue(), item.getVariableSelector()))
						.toList();
					nodeData.setInputs(selectors);
				};
				yield consumer.andThen(super.postProcessConsumer(dialectType));
			}
			case CUSTOM -> super.postProcessConsumer(dialectType);
		};
	}

}
