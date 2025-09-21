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

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.QuestionClassifierNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.stereotype.Component;

/**
 * @author HeYQ
 * @since 2024-12-12 23:54
 */
@Component
public class QuestionClassifyNodeDataConverter extends AbstractNodeDataConverter<QuestionClassifierNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.QUESTION_CLASSIFIER.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<QuestionClassifierNodeData>> getDialectConverters() {
		return Stream.of(QuestionClassifyNodeDialectConverter.values())
			.map(QuestionClassifyNodeDialectConverter::dialectConverter)
			.toList();
	}

	private enum QuestionClassifyNodeDialectConverter {

		DIFY(new DialectConverter<>() {

			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public QuestionClassifierNodeData parse(Map<String, Object> data) {
				QuestionClassifierNodeData nodeData = new QuestionClassifierNodeData();

				// 获取必要的信息
				String modeName = this.exactChatModelName(DSLDialectType.DIFY, data);
				Map<String, Object> modeParams = this.exactChatModelParam(DSLDialectType.DIFY, data);
				List<String> inputSelectorList = Optional
					.ofNullable(MapReadUtil.safeCastToList(
							MapReadUtil.getMapDeepValue(data, List.class, "query_variable_selector"), String.class))
					.orElseThrow();
				VariableSelector selector = new VariableSelector(inputSelectorList.get(0), inputSelectorList.get(1));
				String outputKey = QuestionClassifierNodeData.getDefaultOutputSchema(DSLDialectType.DIFY).getName();
				List<QuestionClassifierNodeData.ClassConfig> classes = Optional
					.ofNullable(
							MapReadUtil.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "classes")))
					.orElseThrow()
					.stream()
					.filter(map -> map.containsKey("id") && map.containsKey("name"))
					.map(map -> new QuestionClassifierNodeData.ClassConfig(map.get("id").toString(),
							map.get("name").toString()))
					.toList();
				String promptTemplate = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, String.class, "instruction"))
					.orElse("");

				// 设置基本信息
				nodeData.setChatModeName(modeName);
				nodeData.setModeParams(modeParams);
				nodeData.setInputSelector(selector);
				nodeData.setOutputKey(outputKey);
				nodeData.setClasses(classes);
				nodeData.setPromptTemplate(promptTemplate);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(QuestionClassifierNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			@Override
			public QuestionClassifierNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				QuestionClassifierNodeData nodeData = new QuestionClassifierNodeData();
				// 从data中提取必要信息
				String modeName = this.exactChatModelName(DSLDialectType.STUDIO, data);
				Map<String, Object> modeParams = this.exactChatModelParam(DSLDialectType.STUDIO, data);

				VariableSelector selector = this.varTemplateToSelector(DSLDialectType.STUDIO, MapReadUtil
					.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, List.class, "config", "input_params"))
					.get(0)
					.get("value")
					.toString());
				String outputKey = QuestionClassifierNodeData.getDefaultOutputSchema(DSLDialectType.STUDIO).getName();
				List<QuestionClassifierNodeData.ClassConfig> classes = Optional
					.ofNullable(MapReadUtil.safeCastToListWithMap(
							MapReadUtil.getMapDeepValue(data, List.class, "config", "node_param", "conditions")))
					.orElseThrow()
					.stream()
					.filter(map -> map.containsKey("id") && map.containsKey("subject"))
					.map(map -> {
						String id = map.get("id").toString();
						String subject = map.get("subject").toString();
						if ("default".equalsIgnoreCase(id)) {
							subject = "default";
						}
						return new QuestionClassifierNodeData.ClassConfig(id, subject);
					})
					.toList();
				String promptTemplate = Optional
					.ofNullable(MapReadUtil.getMapDeepValue(data, String.class, "config", "node_param", "instruction"))
					.orElse("");

				// 设置基本信息
				nodeData.setChatModeName(modeName);
				nodeData.setModeParams(modeParams);
				nodeData.setInputSelector(selector);
				nodeData.setOutputKey(outputKey);
				nodeData.setClasses(classes);
				nodeData.setPromptTemplate(promptTemplate);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(QuestionClassifierNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		})

		, CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(QuestionClassifierNodeData.class));

		private final DialectConverter<QuestionClassifierNodeData> dialectConverter;

		public DialectConverter<QuestionClassifierNodeData> dialectConverter() {
			return dialectConverter;
		}

		QuestionClassifyNodeDialectConverter(DialectConverter<QuestionClassifierNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

	public String generateVarName(int count) {
		return "questionClassifyNode" + count;
	}

	@Override
	public BiConsumer<QuestionClassifierNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		BiConsumer<QuestionClassifierNodeData, Map<String, String>> consumer = emptyProcessConsumer()
			.andThen((nodeData, idToVarName) -> {
				nodeData.setOutputs(List.of(QuestionClassifierNodeData.getDefaultOutputSchema(dialectType)));
				nodeData.setInputs(List.of(nodeData.getInputSelector()));
			})
			.andThen(super.postProcessConsumer(dialectType))
			.andThen((nodeData, idToVarName) -> {
				nodeData.setOutputKey(nodeData.getOutputs().get(0).getName());
				nodeData.setInputSelector(nodeData.getInputs().get(0));
				// 替换掉类别和指导中的占位变量
				nodeData
					.setPromptTemplate(this.convertVarTemplate(dialectType, nodeData.getPromptTemplate(), idToVarName));
				nodeData.setClasses(nodeData.getClasses()
					.stream()
					.map(classConfig -> new QuestionClassifierNodeData.ClassConfig(classConfig.id(),
							this.convertVarTemplate(dialectType, classConfig.classTemplate(), idToVarName)))
					.toList());
			});
		return switch (dialectType) {
			case DIFY -> consumer;
			case STUDIO -> consumer.andThen((nodeData, idToVarName) -> {
				// 将classConfig的id里添加nodeId（为了与Edge里的sourceHandle保持一致）
				Map<String, String> varNameToId = idToVarName.entrySet()
					.stream()
					.collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));
				String nodeId = varNameToId.getOrDefault(nodeData.getVarName(), nodeData.getVarName());
				nodeData.setClasses(nodeData.getClasses()
					.stream()
					.map(classConfig -> new QuestionClassifierNodeData.ClassConfig(nodeId + "_" + classConfig.id(),
							classConfig.classTemplate()))
					.toList());
			});
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
