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

package com.alibaba.cloud.ai.studio.admin.generator.service.dsl.converter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.AssignerNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

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
				AssignerNodeData nodeData = new AssignerNodeData();
				List<AssignerNodeData.AssignItem> items = Stream
					.ofNullable(
							MapReadUtil.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "items")))
					.flatMap(List::stream)
					.filter(map -> map.containsKey("operation") && map.containsKey("variable_selector"))
					.map(map -> {
						List<String> variableSelectorList = Optional
							.ofNullable(MapReadUtil.safeCastToList(
									MapReadUtil.getMapDeepValue(map, List.class, "variable_selector"), String.class))
							.orElseThrow();
						VariableSelector variableSelector = new VariableSelector(variableSelectorList.get(0),
								variableSelectorList.get(1));

						AssignerNodeData.WriteMode writeMode = AssignerNodeData.WriteMode.fromDslValue(
								DSLDialectType.DIFY, MapReadUtil.getMapDeepValue(map, String.class, "operation"));

						VariableSelector inputSelector = null;
						String inputConst = null;
						if (AssignerNodeData.WriteMode.INPUT_CONSTANT.equals(writeMode)) {
							inputConst = map.get("value").toString();
						}
						else if (AssignerNodeData.WriteMode.OVER_WRITE.equals(writeMode)) {
							List<String> inputSelectorList = Optional
								.ofNullable(MapReadUtil.safeCastToList(
										MapReadUtil.getMapDeepValue(map, List.class, "value"), String.class))
								.orElseThrow();
							inputSelector = new VariableSelector(inputSelectorList.get(0), inputSelectorList.get(1));
						}

						return new AssignerNodeData.AssignItem(variableSelector, inputSelector, writeMode, inputConst);
					})
					.toList();
				nodeData.setItems(items);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(AssignerNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		}), STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public AssignerNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				AssignerNodeData nodeData = new AssignerNodeData();
				List<AssignerNodeData.AssignItem> items = Stream
					.ofNullable(MapReadUtil.safeCastToListWithMap(
							MapReadUtil.getMapDeepValue(data, List.class, "config", "node_param", "inputs")))
					.flatMap(List::stream)
					.filter(map -> map.containsKey("left") && map.containsKey("right"))
					.map(map -> {
						VariableSelector targetSelector = this.varTemplateToSelector(DSLDialectType.STUDIO,
								MapReadUtil.getMapDeepValue(map, String.class, "left", "value"));

						AssignerNodeData.WriteMode writeMode = AssignerNodeData.WriteMode.fromDslValue(
								DSLDialectType.STUDIO,
								MapReadUtil.getMapDeepValue(map, String.class, "right", "value_from"));
						VariableSelector inputSelector = null;
						String inputConst = null;
						if (AssignerNodeData.WriteMode.INPUT_CONSTANT.equals(writeMode)) {
							inputConst = MapReadUtil.getMapDeepValue(map, String.class, "right", "value");
						}
						else if (AssignerNodeData.WriteMode.OVER_WRITE.equals(writeMode)) {
							inputSelector = this.varTemplateToSelector(DSLDialectType.STUDIO,
									MapReadUtil.getMapDeepValue(map, String.class, "right", "value"));
						}

						return new AssignerNodeData.AssignItem(targetSelector, inputSelector, writeMode, inputConst);
					})
					.toList();
				nodeData.setItems(items);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(AssignerNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(AssignerNodeData.class));

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
	public BiConsumer<AssignerNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY, STUDIO -> super.postProcessConsumer(dialectType).andThen((nodeData, idToVarName) -> {
				nodeData.getItems().forEach(item -> {
					Consumer<VariableSelector> consumer = selector -> {
						selector
							.setNameInCode(idToVarName.getOrDefault(selector.getNamespace(), selector.getNamespace())
									+ "_" + selector.getName());
					};
					Optional.ofNullable(item.targetSelector()).ifPresent(consumer);
					Optional.ofNullable(item.inputSelector()).ifPresent(consumer);
				});
			});
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
