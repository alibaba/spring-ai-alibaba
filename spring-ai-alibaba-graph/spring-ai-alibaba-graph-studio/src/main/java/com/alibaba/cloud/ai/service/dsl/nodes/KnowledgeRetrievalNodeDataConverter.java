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

import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.KnowledgeRetrievalNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KnowledgeRetrievalNodeDataConverter extends AbstractNodeDataConverter<KnowledgeRetrievalNodeData> {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.RETRIEVER.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<KnowledgeRetrievalNodeData>> getDialectConverters() {
		return Stream.of(KnowledgeRetrievalNodeDataConverter.KnowledgeRetrievalNodeConverter.values())
			.map(KnowledgeRetrievalNodeDataConverter.KnowledgeRetrievalNodeConverter::dialectConverter)
			.collect(Collectors.toList());
	}

	private enum KnowledgeRetrievalNodeConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialect) {
				return DSLDialectType.DIFY.equals(dialect);
			}

			@SuppressWarnings("unchecked")
			@Override
			public KnowledgeRetrievalNodeData parse(Map<String, Object> data) {
				KnowledgeRetrievalNodeData nd = new KnowledgeRetrievalNodeData();
				// inputs
				List<String> sel = (List<String>) data.get("variable_selector");
				if (sel != null && sel.size() == 2) {
					nd.setInputs(Collections.singletonList(new VariableSelector(sel.get(0), sel.get(1))));
				}
				// userPromptKey & userPrompt
				nd.setUserPromptKey((String) data.get("user_prompt_key"));
				nd.setUserPrompt((String) data.get("user_prompt"));
				// topKKey & topK
				nd.setTopKKey((String) data.get("top_k_key"));
				if (data.get("top_k") != null) {
					nd.setTopK(((Number) data.get("top_k")).intValue());
				}
				// similarityThresholdKey & similarityThreshold
				nd.setSimilarityThresholdKey((String) data.get("similarity_threshold_key"));
				if (data.get("similarity_threshold") != null) {
					nd.setSimilarityThreshold(((Number) data.get("similarity_threshold")).doubleValue());
				}
				// filterExpressionKey & filterExpression
				nd.setFilterExpressionKey((String) data.get("filter_expression_key"));
				if (data.get("filter_expression") != null) {
					nd.setFilterExpression(
							objectMapper.convertValue(data.get("filter_expression"), Filter.Expression.class));
				}
				// enableRankerKey & enableRanker
				nd.setEnableRankerKey((String) data.get("enable_ranker_key"));
				if (data.get("enable_ranker") != null) {
					nd.setEnableRanker((Boolean) data.get("enable_ranker"));
				}
				// rerankModelKey & rerankModel
				nd.setRerankModelKey((String) data.get("rerank_model_key"));
				if (data.get("rerank_model") != null) {
					nd.setRerankModel(objectMapper.convertValue(data.get("rerank_model"), RerankModel.class));
				}
				// rerankOptionsKey & rerankOptions
				nd.setRerankOptionsKey((String) data.get("rerank_options_key"));
				if (data.get("rerank_options") != null) {
					nd.setRerankOptions(
							objectMapper.convertValue(data.get("rerank_options"), DashScopeRerankOptions.class));
				}
				// vectorStoreKey & vectorStore
				nd.setVectorStoreKey((String) data.get("vector_store_key"));
				return nd;
			}

			@Override
			public Map<String, Object> dump(KnowledgeRetrievalNodeData nd) {
				Map<String, Object> m = new LinkedHashMap<>();
				// variable_selector
				if (nd.getInputs() != null && !nd.getInputs().isEmpty()) {
					VariableSelector vs = nd.getInputs().get(0);
					m.put("variable_selector", List.of(vs.getNamespace(), vs.getName()));
				}
				// user_prompt_key & user_prompt
				if (nd.getUserPromptKey() != null) {
					m.put("user_prompt_key", nd.getUserPromptKey());
				}
				if (nd.getUserPrompt() != null) {
					m.put("user_prompt", nd.getUserPrompt());
				}
				// top_k_key & top_k
				if (nd.getTopKKey() != null) {
					m.put("top_k_key", nd.getTopKKey());
				}
				if (nd.getTopK() != null) {
					m.put("top_k", nd.getTopK());
				}
				// similarity_threshold_key & similarity_threshold
				if (nd.getSimilarityThresholdKey() != null) {
					m.put("similarity_threshold_key", nd.getSimilarityThresholdKey());
				}
				if (nd.getSimilarityThreshold() != null) {
					m.put("similarity_threshold", nd.getSimilarityThreshold());
				}
				// filter_expression_key & filter_expression
				if (nd.getFilterExpressionKey() != null) {
					m.put("filter_expression_key", nd.getFilterExpressionKey());
				}
				if (nd.getFilterExpression() != null) {
					m.put("filter_expression", nd.getFilterExpression());
				}
				// enable_ranker_key & enable_ranker
				if (nd.getEnableRankerKey() != null) {
					m.put("enable_ranker_key", nd.getEnableRankerKey());
				}
				if (nd.getEnableRanker() != null) {
					m.put("enable_ranker", nd.getEnableRanker());
				}
				// rerank_model_key & rerank_model
				if (nd.getRerankModelKey() != null) {
					m.put("rerank_model_key", nd.getRerankModelKey());
				}
				if (nd.getRerankModel() != null) {
					m.put("rerank_model", nd.getRerankModel());
				}
				// rerank_options_key & rerank_options
				if (nd.getRerankOptionsKey() != null) {
					m.put("rerank_options_key", nd.getRerankOptionsKey());
				}
				if (nd.getRerankOptions() != null) {
					m.put("rerank_options", nd.getRerankOptions());
				}
				// vector_store_key
				if (nd.getVectorStoreKey() != null) {
					m.put("vector_store_key", nd.getVectorStoreKey());
				}
				return m;
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

}
