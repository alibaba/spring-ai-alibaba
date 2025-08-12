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

package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.KnowledgeRetrievalNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeRetrievalNodeSection implements NodeSection<KnowledgeRetrievalNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.RETRIEVER.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		KnowledgeRetrievalNodeData d = (KnowledgeRetrievalNodeData) node.getData();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("// —— KnowledgeRetrievalNode [%s] ——%n", id));
		sb.append(String.format("KnowledgeRetrievalNode %s = KnowledgeRetrievalNode.builder()%n", varName));

		sb.append(String.format(".inputKey(\"%s\")%n", d.getInputKey()));

		if (d.getUserPrompt() != null) {
			sb.append(String.format(".userPrompt(\"%s\")%n", escape(d.getUserPrompt())));
		}
		if (d.getTopKKey() != null) {
			sb.append(String.format(".topKKey(\"%s\")%n", escape(d.getTopKKey())));
		}
		if (d.getTopK() != null) {
			sb.append(String.format(".topK(%d)%n", d.getTopK()));
		}
		if (d.getSimilarityThresholdKey() != null) {
			sb.append(String.format(".similarityThresholdKey(\"%s\")%n", escape(d.getSimilarityThresholdKey())));
		}
		if (d.getSimilarityThreshold() != null) {
			sb.append(String.format(".similarityThreshold(%s)%n", d.getSimilarityThreshold()));
		}
		if (d.getFilterExpressionKey() != null) {
			sb.append(String.format(".filterExpressionKey(\"%s\")%n", escape(d.getFilterExpressionKey())));
		}
		if (d.getFilterExpression() != null) {
			sb.append(String.format(".filterExpression(%s)%n", d.getFilterExpression().toString()));
		}
		if (d.getEnableRankerKey() != null) {
			sb.append(String.format(".enableRankerKey(\"%s\")%n", escape(d.getEnableRankerKey())));
		}
		if (d.getEnableRanker() != null) {
			sb.append(String.format(".enableRanker(%b)%n", d.getEnableRanker()));
		}
		if (d.getRerankModelKey() != null) {
			sb.append(String.format(".rerankModelKey(\"%s\")%n", escape(d.getRerankModelKey())));
		}
		if (d.getRerankModel() != null) {
			sb.append(String.format(".rerankModel(%s)%n", d.getRerankModel()));
		}
		if (d.getRerankOptionsKey() != null) {
			sb.append(String.format(".rerankOptionsKey(\"%s\")%n", escape(d.getRerankOptionsKey())));
		}
		if (d.getRerankOptions() != null) {
			sb.append(String.format(".rerankOptions(%s)%n", d.getRerankOptions()));
		}
		if (d.getVectorStoreKey() != null) {
			sb.append(String.format(".vectorStoreKey(\"%s\")%n", escape(d.getVectorStoreKey())));
		}

		if (d.getRetrievalMode() != null) {
			sb.append(String.format(".retrievalMode(\"%s\")%n", escape(d.getRetrievalMode())));
		}
		if (d.getEmbeddingModelName() != null) {
			sb.append(String.format(".embeddingModelName(\"%s\")%n", escape(d.getEmbeddingModelName())));
		}
		if (d.getEmbeddingProviderName() != null) {
			sb.append(String.format(".embeddingProviderName(\"%s\")%n", escape(d.getEmbeddingProviderName())));
		}
		if (d.getVectorWeight() != null) {
			sb.append(String.format(".vectorWeight(%s)%n", d.getVectorWeight()));
		}
		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}
		sb.append(".vectorStore(vectorStore)\n");

		sb.append(".isKeyFirst(false).build();\n");

		// 辅助节点代码
		String assistNodeCode = String.format(
				"""
						(state) -> {
							String key = "%s";
							// 将结果转换为Dify工作流中需要的变量
							Map<String, Object> result = %s.apply(state);
							Object object = result.get(key);
							if(object instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Document) {
								// 返回值为Array[Object]（用List<Map>）
								List<Document> documentList = (List<Document>) list;
								List<Map<String, Object>> mapList = documentList.stream().map(document ->
												Map.of("content", document.getFormattedContent(), "title", document.getId(), "url", "", "icon", "", "metadata", document.getMetadata()))
										.toList();
								return Map.of(key, mapList);
							} else {
								return Map.of(key, List.of(Map.of("content", object.toString(), "title", "unknown", "url", "unknown", "icon", "unknown", "metadata", object)));
							}
						}
						""",
				d.getOutputKey(), varName);

		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName,
				assistNodeCode));
		return sb.toString();
	}

}
