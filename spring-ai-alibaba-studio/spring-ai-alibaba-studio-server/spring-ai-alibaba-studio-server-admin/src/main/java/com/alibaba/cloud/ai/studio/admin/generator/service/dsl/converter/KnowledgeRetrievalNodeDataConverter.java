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
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.KnowledgeRetrievalNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;

import com.alibaba.cloud.ai.studio.admin.generator.utils.MapReadUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeRetrievalNodeDataConverter extends AbstractNodeDataConverter<KnowledgeRetrievalNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.RETRIEVER.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<KnowledgeRetrievalNodeData>> getDialectConverters() {
		return Stream.of(KnowledgeRetrievalNodeConverter.values())
			.map(KnowledgeRetrievalNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum KnowledgeRetrievalNodeConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}

			@Override
			public KnowledgeRetrievalNodeData parse(Map<String, Object> data) {
				KnowledgeRetrievalNodeData nodeData = new KnowledgeRetrievalNodeData();
				nodeData.setDialectType(DSLDialectType.DIFY);
				// 获取必要信息
				Integer topK = MapReadUtil.getMapDeepValue(data, Integer.class, "multiple_retrieval_config", "top_k");
				Double threshold = MapReadUtil.getMapDeepValue(data, Double.class, "multiple_retrieval_config",
						"score_threshold");
				List<String> knowledgeBaseIds = MapReadUtil
					.safeCastToList(MapReadUtil.getMapDeepValue(data, List.class, "dataset_ids"), String.class);
				List<String> inputParams = MapReadUtil.safeCastToList(
						MapReadUtil.getMapDeepValue(data, List.class, "query_variable_selector"), String.class);
				if (inputParams != null && inputParams.size() >= 2) {
					nodeData.setInputs(List.of(new VariableSelector(inputParams.get(0), inputParams.get(1))));
				}
				else {
					nodeData.setInputs(List.of(new VariableSelector("sys", "query")));
				}

				// 设置信息
				nodeData.setTopK(topK);
				nodeData.setThreshold(threshold);
				nodeData.setKnowledgeBaseIds(knowledgeBaseIds);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(KnowledgeRetrievalNodeData nd) {
				throw new UnsupportedOperationException();
			}
		}), STUDIO(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.STUDIO.equals(dialectType);
			}

			private static final Pattern VAR_TEMPLATE_PATTERN = Pattern.compile("\\$\\{(\\w+)\\.(\\w+)}");

			@Override
			public KnowledgeRetrievalNodeData parse(Map<String, Object> data) throws JsonProcessingException {
				KnowledgeRetrievalNodeData nodeData = new KnowledgeRetrievalNodeData();
				nodeData.setDialectType(DSLDialectType.STUDIO);
				// 获取必要信息
				Integer topK = MapReadUtil.getMapDeepValue(data, Integer.class, "config", "node_param", "top_k");
				Double threshold = MapReadUtil.getMapDeepValue(data, Double.class, "config", "node_param",
						"similarity_threshold");
				List<String> knowledgeBaseIds = MapReadUtil.safeCastToList(
						MapReadUtil.getMapDeepValue(data, List.class, "config", "node_param", "knowledge_base_ids"),
						String.class);
				List<Map<String, Object>> inputParams = MapReadUtil
					.safeCastToListWithMap(MapReadUtil.getMapDeepValue(data, String.class, "config", "input_params"));
				// Studio DSL 此值是一个模板值
				String inputKey;
				if (inputParams != null && !inputParams.isEmpty()) {
					inputKey = inputParams.get(0).get("value").toString();
				}
				else {
					inputKey = "${sys.query}";
				}
				Matcher matcher = VAR_TEMPLATE_PATTERN.matcher(inputKey);
				if (matcher.find()) {
					nodeData.setInputs(List.of(new VariableSelector(matcher.group(1), matcher.group(2))));
				}
				else {
					nodeData.setInputs(List.of(new VariableSelector("sys", "query")));
				}

				// 设置信息
				nodeData.setTopK(topK);
				nodeData.setThreshold(threshold);
				nodeData.setKnowledgeBaseIds(knowledgeBaseIds);
				return nodeData;
			}

			@Override
			public Map<String, Object> dump(KnowledgeRetrievalNodeData nodeData) {
				throw new UnsupportedOperationException();
			}
		}), CUSTOM(defaultCustomDialectConverter(KnowledgeRetrievalNodeData.class));

		private final DialectConverter<KnowledgeRetrievalNodeData> converter;

		KnowledgeRetrievalNodeConverter(DialectConverter<KnowledgeRetrievalNodeData> converter) {
			this.converter = converter;
		}

		public DialectConverter<KnowledgeRetrievalNodeData> dialectConverter() {
			return converter;
		}

	}

	@Override
	public String generateVarName(int count) {
		return "retrievalNode" + count;
	}

	@Override
	public BiConsumer<KnowledgeRetrievalNodeData, Map<String, String>> postProcessConsumer(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY, STUDIO -> this.emptyProcessConsumer().andThen((nodeData, idToVarName) ->
				// 设置输出
				nodeData.setOutputs(KnowledgeRetrievalNodeData.getDefaultOutputSchemas(dialectType)))
				.andThen(super.postProcessConsumer(dialectType))
				.andThen((nodeData, idToVarName) -> {
					// 获取最终的输入输出Key名
					nodeData.setOutputKey(nodeData.getOutputs().get(0).getName());
					nodeData.setInputKey(nodeData.getInputs().get(0).getNameInCode());
				});
			default -> super.postProcessConsumer(dialectType);
		};
	}

}
