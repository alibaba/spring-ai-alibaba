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
public class KnowledgeRetrievalNodeSection implements NodeSection {

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

		if (d.getUserPromptKey() != null) {
			sb.append(String.format(".userPromptKey(\"%s\")%n", escape(d.getUserPromptKey())));
		}
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
		sb.append(".vectorStore(vectorStore)\n");

		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", id, varName));
		return sb.toString();
	}

}
