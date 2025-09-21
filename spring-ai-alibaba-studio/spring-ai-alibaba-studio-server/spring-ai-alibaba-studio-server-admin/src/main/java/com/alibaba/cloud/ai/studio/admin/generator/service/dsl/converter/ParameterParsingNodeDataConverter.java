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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.ParameterParsingNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

/**
 * Convert the ParameterParsing node configuration in the Dify DSL to and from the
 * ParameterParsingNodeData object.
 */
@Component
public class ParameterParsingNodeDataConverter extends AbstractNodeDataConverter<ParameterParsingNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.PARAMETER_PARSING.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<ParameterParsingNodeData>> getDialectConverters() {
		return Stream.of(ParameterParsingNodeConverter.values())
			.map(ParameterParsingNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum ParameterParsingNodeConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public ParameterParsingNodeData parse(Map<String, Object> data) {
				ParameterParsingNodeData nodeData = new ParameterParsingNodeData();

				// 获取必要信息
				List<String> selectorList = MapReadUtil.safeCastToList(data.get("query"), String.class);
				VariableSelector selector = new VariableSelector(selectorList.get(0), selectorList.get(1));
				String chatModelName = this.exactChatModelName(DSLDialectType.DIFY, data);
				Map<String, Object> modelParams = this.exactChatModelParam(DSLDialectType.DIFY, data);
				List<ParameterParsingNodeData.Param> params = MapReadUtil.safeCastToListWithMap(data.get("parameters"))
					.stream()
					.filter(map -> map.containsKey("name"))
					.map(map -> {
						String name = map.get("name").toString();
						String description = map.getOrDefault("description", "").toString();
						VariableType type = VariableType
							.fromDifyValue(map.getOrDefault("type", VariableType.OBJECT.difyValue()).toString())
							.orElse(VariableType.OBJECT);
						return new ParameterParsingNodeData.Param(name, type, description);
					})
					.toList();
				String instruction = Optional.ofNullable(data.get("instruction")).map(Object::toString).orElse("");

				// 设置信息
				nodeData.setInputSelector(selector);
				nodeData.setChatModeName(chatModelName);
				nodeData.setModeParams(modelParams);
				nodeData.setParameters(params);
				nodeData.setInstruction(instruction);
				nodeData.setSuccessKey("__is_success");
				nodeData.setReasonKey("__reason");
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(ParameterParsingNodeData nd) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}
		}),

		STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public ParameterParsingNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				ParameterParsingNodeData nodeData = new ParameterParsingNodeData();

				// 获取必要信息
				VariableSelector selector = this.varTemplateToSelector(DSLDialectType.STUDIO, MapReadUtil
					.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "input_params"))
					.get(0)
					.get("value")
					.toString());
				String chatModelName = this.exactChatModelName(DSLDialectType.STUDIO, data);
				Map<String, Object> modelParams = this.exactChatModelParam(DSLDialectType.STUDIO, data);
				List<ParameterParsingNodeData.Param> params = Optional
					.ofNullable(MapReadUtil.safeCastToListWithMap(
							MapReadUtil.getMapDeepValue(data, List.class, "config", "node_param", "extract_params")))
					.orElse(List.of())
					.stream()
					.filter(map -> map.containsKey("key"))
					.map(map -> {
						String name = map.get("key").toString();
						String description = map.getOrDefault("desc", "").toString();
						VariableType type = VariableType
							.fromStudioValue(map.getOrDefault("type", VariableType.OBJECT.studioValue()).toString())
							.orElse(VariableType.OBJECT);
						return new ParameterParsingNodeData.Param(name, type, description);
					})
					.toList();
				String instruction = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "instruction"))
					.map(Object::toString)
					.orElse("");

				// 设置必要信息
				nodeData.setInputSelector(selector);
				nodeData.setChatModeName(chatModelName);
				nodeData.setModeParams(modelParams);
				nodeData.setParameters(params);
				nodeData.setInstruction(instruction);
				nodeData.setSuccessKey("_is_completed");
				nodeData.setReasonKey("_reason");
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(ParameterParsingNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, CUSTOM(defaultCustomDialectConverter(ParameterParsingNodeData.class));

		private final DialectConverter<ParameterParsingNodeData> converter;

		ParameterParsingNodeConverter(DialectConverter<ParameterParsingNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<ParameterParsingNodeData> dialectConverter() {
			return converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "parameterParsingNode" + count;
	}

	@Override
	public BiConsumer<ParameterParsingNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY, STUDIO -> emptyProcessConsumer().andThen((nodeData, idToVarName) -> {
				// 设置输出
				List<Variable> outputs = Stream
					.concat(nodeData.getParameters().stream().map(p -> new Variable(p.name(), p.type())),
							ParameterParsingNodeData.getDefaultOutputSchema(dialectType).stream())
					.toList();
				nodeData.setOutputs(outputs);

				// 设置输入以及key
				Optional.ofNullable(nodeData.getInputSelector())
					.ifPresent(selector -> selector
						.setNameInCode(idToVarName.getOrDefault(selector.getNamespace(), selector.getNamespace()) + "_"
								+ selector.getName()));
				nodeData.setSuccessKey(nodeData.getVarName() + "_" + nodeData.getSuccessKey());
				nodeData.setReasonKey(nodeData.getVarName() + "_" + nodeData.getReasonKey());
				nodeData.setDataKey(nodeData.getVarName() + "_" + nodeData.getDataKey());

				// 格式化instruction
				nodeData.setInstruction(this.convertVarTemplate(dialectType, nodeData.getInstruction(), idToVarName));
			}).andThen(super.postProcessConsumer(dialectType));
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
