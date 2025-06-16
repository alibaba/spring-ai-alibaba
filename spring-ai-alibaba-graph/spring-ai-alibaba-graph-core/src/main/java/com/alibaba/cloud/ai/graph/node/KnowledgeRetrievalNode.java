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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.util.StringUtils;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KnowledgeRetrievalNode implements NodeAction {

	private String userPromptKey;

	private String userPrompt;

	private String topKKey;

	private Integer topK;

	private String similarityThresholdKey;

	private Double similarityThreshold;

	private String filterExpressionKey;

	private Filter.Expression filterExpression;

	private String enableRankerKey;

	private Boolean enableRanker = false;

	private String rerankModelKey;

	private RerankModel rerankModel;

	private String rerankOptionsKey;

	private DashScopeRerankOptions rerankOptions;

	private String vectorStoreKey;

	private VectorStore vectorStore;

	List<Document> documents;

	private static final Logger logger = LoggerFactory.getLogger(KnowledgeRetrievalNode.class);

	public KnowledgeRetrievalNode() {
	}

	public KnowledgeRetrievalNode(String userPrompt, Integer topK, Double similarityThreshold,
			Filter.Expression filterExpression, Boolean enableRanker, RerankModel rerankModel,
			DashScopeRerankOptions rerankOptions, VectorStore vectorStore) {
		this.userPrompt = userPrompt;
		this.topK = topK;
		this.similarityThreshold = similarityThreshold;
		this.filterExpression = filterExpression;
		this.enableRanker = enableRanker;
		this.rerankModel = rerankModel;
		this.rerankOptions = rerankOptions;
		this.vectorStore = vectorStore;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		initNodeWithState(state);
		DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
			.similarityThreshold(similarityThreshold)
			.topK(topK)
			.filterExpression(filterExpression)
			.vectorStore(vectorStore)
			.build();
		Query query = new Query(userPrompt);
		documents = documentRetriever.retrieve(query);
		documents = enableRanker
				? ranking(query, documents, new KnowledgeRetrievalDocumentRanker(rerankModel, rerankOptions))
				: documents;
		StringBuilder newUserPrompt = new StringBuilder(userPrompt);
		for (Document document : documents) {
			newUserPrompt.append("Document: ").append(document.getFormattedContent()).append("\n");
		}
		Map<String, Object> updatedState = new HashMap<>();

		if (StringUtils.hasLength(this.userPromptKey)) {
			updatedState.put(this.userPromptKey, newUserPrompt.toString());
		}
		else {
			updatedState.put("user_prompt", newUserPrompt.toString());
		}
		return updatedState;
	}

	private void initNodeWithState(OverAllState state) {
		if (StringUtils.hasLength(userPromptKey)) {
			this.userPrompt = (String) state.value(userPromptKey).orElse(this.userPrompt);
		}
		if (StringUtils.hasLength(topKKey)) {
			this.topK = (Integer) state.value(topKKey).orElse(this.topK);
		}

		if (StringUtils.hasLength(similarityThresholdKey)) {
			this.similarityThreshold = (Double) state.value(similarityThresholdKey).orElse(this.similarityThreshold);
		}
		if (StringUtils.hasLength(filterExpressionKey)) {
			this.filterExpression = (Filter.Expression) state.value(filterExpressionKey).orElse(this.filterExpression);
		}
		if (StringUtils.hasLength(enableRankerKey)) {
			this.enableRanker = (Boolean) state.value(enableRankerKey).orElse(this.enableRanker);
		}
		if (StringUtils.hasLength(rerankModelKey)) {
			this.rerankModel = (RerankModel) state.value(rerankModelKey).orElse(this.rerankModel);
		}
		if (StringUtils.hasLength(rerankOptionsKey)) {
			this.rerankOptions = (DashScopeRerankOptions) state.value(rerankOptionsKey).orElse(this.rerankOptions);
		}
		if (StringUtils.hasLength(vectorStoreKey)) {
			this.vectorStore = (VectorStore) state.value(vectorStoreKey).orElse(this.vectorStore);
		}
	}

	private List<Document> ranking(Query query, List<Document> documents, DocumentPostProcessor documentPostProcessor) {
		if (documents.size() <= 1) {
			return documents;
		}

		try {
			List<Document> rankedDocuments = documentPostProcessor.process(query, documents);
			return rankedDocuments;
		}
		catch (Exception e) {
			logger.error("ranking error", e);
			return documents;
		}
	}

	public static class KnowledgeRetrievalDocumentRanker implements DocumentPostProcessor {

		private RerankModel rerankModel;

		private DashScopeRerankOptions rerankOptions;

		public KnowledgeRetrievalDocumentRanker(RerankModel rerankModel, DashScopeRerankOptions rerankOptions) {
			this.rerankModel = rerankModel;
			this.rerankOptions = rerankOptions;
		}

		@Override
		public List<Document> apply(Query query, List<Document> documents) {
			return List.of();
		}

		@NotNull
		@Override
		public List<Document> process(@Nullable Query query, @Nullable List<Document> documents) {

			try {
				List<Document> reorderDocs = new ArrayList<>();
				if (Objects.nonNull(query) && StringUtils.hasText(query.text())) {
					RerankRequest rerankRequest = new RerankRequest(query.text(), documents, rerankOptions);
					RerankResponse rerankResp = rerankModel.call(rerankRequest);
					Map<String, Document> docMap = documents.stream()
						.collect(Collectors.toMap(Document::getId, Function.identity()));
					rerankResp.getResults().forEach(res -> {
						Document outputDocs = res.getOutput();

						Document doc = docMap.get(outputDocs.getId());
						if (doc != null) {
							reorderDocs.add(doc);
						}
					});
				}

				return reorderDocs;
			}
			catch (Exception e) {
				logger.error("rank error", e);
				return documents;
			}
		}

	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String userPromptKey;

		private String userPrompt;

		private String topKKey;

		private Integer topK;

		private String similarityThresholdKey;

		private Double similarityThreshold;

		private String filterExpressionKey;

		private Filter.Expression filterExpression;

		private String enableRankerKey;

		private Boolean enableRanker;

		private String rerankModelKey;

		private RerankModel rerankModel;

		private String rerankOptionsKey;

		private DashScopeRerankOptions rerankOptions;

		private String vectorStoreKey;

		private VectorStore vectorStore;

		public Builder userPromptKey(String userPromptKey) {
			this.userPromptKey = userPromptKey;
			return this;
		}

		public Builder userPrompt(String userPrompt) {
			this.userPrompt = userPrompt;
			return this;
		}

		public Builder topKKey(String topKKey) {
			this.topKKey = topKKey;
			return this;
		}

		public Builder topK(Integer topK) {
			this.topK = topK;
			return this;
		}

		public Builder similarityThresholdKey(String similarityThresholdKey) {
			this.similarityThresholdKey = similarityThresholdKey;
			return this;
		}

		public Builder similarityThreshold(Double similarityThreshold) {
			this.similarityThreshold = similarityThreshold;
			return this;
		}

		public Builder filterExpressionKey(String filterExpressionKey) {
			this.filterExpressionKey = filterExpressionKey;
			return this;
		}

		public Builder filterExpression(Filter.Expression filterExpression) {
			this.filterExpression = filterExpression;
			return this;
		}

		public Builder enableRankerKey(String enableRankerKey) {
			this.enableRankerKey = enableRankerKey;
			return this;
		}

		public Builder enableRanker(Boolean enableRanker) {
			this.enableRanker = enableRanker;
			return this;
		}

		public Builder rerankModelKey(String rerankModelKey) {
			this.rerankModelKey = rerankModelKey;
			return this;
		}

		public Builder rerankModel(RerankModel rerankModel) {
			this.rerankModel = rerankModel;
			return this;
		}

		public Builder rerankOptionsKey(String rerankOptionsKey) {
			this.rerankOptionsKey = rerankOptionsKey;
			return this;
		}

		public Builder rerankOptions(DashScopeRerankOptions rerankOptions) {
			this.rerankOptions = rerankOptions;
			return this;
		}

		public Builder vectorStoreKey(String vectorStoreKey) {
			this.vectorStoreKey = vectorStoreKey;
			return this;
		}

		public Builder vectorStore(VectorStore vectorStore) {
			this.vectorStore = vectorStore;
			return this;
		}

		public KnowledgeRetrievalNode build() {
			KnowledgeRetrievalNode knowledgeRetrievalNode = new KnowledgeRetrievalNode();
			knowledgeRetrievalNode.userPromptKey = this.userPromptKey;
			knowledgeRetrievalNode.userPrompt = this.userPrompt;
			knowledgeRetrievalNode.topKKey = this.topKKey;
			knowledgeRetrievalNode.topK = this.topK;
			knowledgeRetrievalNode.similarityThresholdKey = this.similarityThresholdKey;
			knowledgeRetrievalNode.similarityThreshold = this.similarityThreshold;
			knowledgeRetrievalNode.filterExpressionKey = this.filterExpressionKey;
			knowledgeRetrievalNode.filterExpression = this.filterExpression;
			knowledgeRetrievalNode.enableRankerKey = this.enableRankerKey;
			knowledgeRetrievalNode.enableRanker = this.enableRanker;
			knowledgeRetrievalNode.rerankModelKey = this.rerankModelKey;
			knowledgeRetrievalNode.rerankModel = this.rerankModel;
			knowledgeRetrievalNode.rerankOptionsKey = this.rerankOptionsKey;
			knowledgeRetrievalNode.rerankOptions = this.rerankOptions;
			knowledgeRetrievalNode.vectorStoreKey = this.vectorStoreKey;
			knowledgeRetrievalNode.vectorStore = this.vectorStore;
			return knowledgeRetrievalNode;
		}

	}

}
