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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

	private DocumentRetriever documentRetriever;

	private boolean augmentPrompt = true;

	// When some properties have both key and value set (e.g. userPrompt and userPromptKey), prioritize using the value from state key or the preset value
	private boolean isKeyFirst = true;

	private String outputKey;

	private String retrievalMode;

	private String embeddingModelName;

	private String embeddingProviderName;

	private Double vectorWeight;

	private static final Logger logger = LoggerFactory.getLogger(KnowledgeRetrievalNode.class);

	public KnowledgeRetrievalNode() {
	}

	public KnowledgeRetrievalNode(String userPrompt, Integer topK, Double similarityThreshold,
			Filter.Expression filterExpression, Boolean enableRanker, RerankModel rerankModel,
			DashScopeRerankOptions rerankOptions, VectorStore vectorStore, String outputKey) {
		this.userPrompt = userPrompt;
		this.topK = topK;
		this.similarityThreshold = similarityThreshold;
		this.filterExpression = filterExpression;
		this.enableRanker = enableRanker;
		this.rerankModel = rerankModel;
		this.rerankOptions = rerankOptions;
		this.vectorStore = vectorStore;
		this.outputKey = outputKey;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String prompt = resolve(state, this.userPromptKey, this.userPrompt);
		Query query = new Query(prompt);
		List<Document> documents = retriever(state).retrieve(query);
		if (Boolean.TRUE.equals(resolve(state, this.enableRankerKey, this.enableRanker))) {
			RerankModel model = resolve(state, this.rerankModelKey, this.rerankModel);
			DashScopeRerankOptions options = resolve(state, this.rerankOptionsKey, this.rerankOptions);
			documents = ranking(query, documents, new KnowledgeRetrievalDocumentRanker(model, options));
		}

		Map<String, Object> updatedState = new HashMap<>();
		if (this.augmentPrompt) {
			StringBuilder newUserPrompt = new StringBuilder(prompt);
			for (Document document : documents) {
				newUserPrompt.append("Document: ").append(document.getFormattedContent()).append("\n");
			}
			String promptKey = StringUtils.hasLength(this.userPromptKey) ? this.userPromptKey : "user_prompt";
			updatedState.put(promptKey, newUserPrompt.toString());
		}
		updatedState.put(Optional.ofNullable(this.outputKey).orElse("output"), documents);
		return updatedState;
	}

	private DocumentRetriever retriever(OverAllState state) {
		if (this.documentRetriever != null) {
			return this.documentRetriever;
		}
		return VectorStoreDocumentRetriever.builder()
			.similarityThreshold(resolve(state, this.similarityThresholdKey, this.similarityThreshold))
			.topK(resolve(state, this.topKKey, this.topK))
			.filterExpression(resolve(state, this.filterExpressionKey, this.filterExpression))
			.vectorStore(resolve(state, this.vectorStoreKey, this.vectorStore))
			.build();
	}

	private <T> T resolve(OverAllState state, String key, T value) {
		if (StringUtils.hasLength(key) && (this.isKeyFirst || value == null)) {
			return state.<T>value(key).orElse(value);
		}
		return value;
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

		@NonNull
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

		private Boolean enableRanker = false;

		private String rerankModelKey;

		private RerankModel rerankModel;

		private String rerankOptionsKey;

		private DashScopeRerankOptions rerankOptions;

		private String vectorStoreKey;

		private VectorStore vectorStore;

		private DocumentRetriever documentRetriever;

		private boolean augmentPrompt = true;

		private String outputKey;

		private String retrievalMode;

		private String embeddingModelName;

		private String embeddingProviderName;

		private Double vectorWeight;

		private boolean isKeyFirst = true;

		public Builder userPromptKey(String userPromptKey) {
			this.userPromptKey = userPromptKey;
			return this;
		}

		public Builder inputKey(String val) {
			this.userPromptKey = val;
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

		/**
		 * Use a Spring AI retriever with its own search options. Vector store, top-K,
		 * similarity threshold and filter settings must be configured on the retriever.
		 * @param documentRetriever the retriever to call with the resolved prompt
		 * @return this builder
		 */
		public Builder documentRetriever(DocumentRetriever documentRetriever) {
			Assert.notNull(documentRetriever, "documentRetriever cannot be null");
			this.documentRetriever = documentRetriever;
			return this;
		}

		/**
		 * Control whether document content is appended to the prompt in graph state.
		 * @param augmentPrompt whether to write the augmented prompt (defaults to true)
		 * @return this builder
		 */
		public Builder augmentPrompt(boolean augmentPrompt) {
			this.augmentPrompt = augmentPrompt;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder retrievalMode(String retrievalMode) {
			this.retrievalMode = retrievalMode;
			return this;
		}

		public Builder embeddingModelName(String val) {
			this.embeddingModelName = val;
			return this;
		}

		public Builder embeddingProviderName(String val) {
			this.embeddingProviderName = val;
			return this;
		}

		public Builder vectorWeight(Double val) {
			this.vectorWeight = val;
			return this;
		}

		public Builder isKeyFirst(boolean val) {
			this.isKeyFirst = val;
			return this;
		}

		public KnowledgeRetrievalNode build() {
			if (this.documentRetriever != null) {
				Assert.state(this.vectorStore == null && !StringUtils.hasLength(this.vectorStoreKey)
						&& this.topK == null && !StringUtils.hasLength(this.topKKey)
						&& this.similarityThreshold == null && !StringUtils.hasLength(this.similarityThresholdKey)
						&& this.filterExpression == null && !StringUtils.hasLength(this.filterExpressionKey)
						&& this.retrievalMode == null && this.embeddingModelName == null
						&& this.embeddingProviderName == null && this.vectorWeight == null,
						"Configure search options on the documentRetriever instead of on the node");
			}
			Assert.state(this.augmentPrompt || !Objects.equals(
					StringUtils.hasLength(this.userPromptKey) ? this.userPromptKey : "user_prompt",
					Optional.ofNullable(this.outputKey).orElse("output")),
					"The input and output keys must differ when augmentPrompt is disabled");
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
			knowledgeRetrievalNode.documentRetriever = this.documentRetriever;
			knowledgeRetrievalNode.augmentPrompt = this.augmentPrompt;
			knowledgeRetrievalNode.outputKey = this.outputKey;
			knowledgeRetrievalNode.retrievalMode = this.retrievalMode;
			knowledgeRetrievalNode.embeddingModelName = this.embeddingModelName;
			knowledgeRetrievalNode.embeddingProviderName = this.embeddingProviderName;
			knowledgeRetrievalNode.vectorWeight = this.vectorWeight;
			knowledgeRetrievalNode.isKeyFirst = this.isKeyFirst;
			return knowledgeRetrievalNode;
		}

	}

}
