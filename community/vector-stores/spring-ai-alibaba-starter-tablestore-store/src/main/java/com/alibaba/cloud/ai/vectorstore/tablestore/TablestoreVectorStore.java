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
package com.alibaba.cloud.ai.vectorstore.tablestore;

import com.aliyun.openservices.tablestore.agent.knowledge.KnowledgeStoreImpl;
import com.aliyun.openservices.tablestore.agent.model.DocumentHit;
import com.aliyun.openservices.tablestore.agent.model.Response;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tablestore Vector Store.
 */
public class TablestoreVectorStore extends AbstractObservationVectorStore implements InitializingBean {

	private final KnowledgeStoreImpl knowledgeStore;

	private final boolean initializeTable;

	protected TablestoreVectorStore(Builder builder) {
		super(builder);
		this.knowledgeStore = builder.knowledgeStore;
		this.initializeTable = builder.initializeTable;
	}

	@Override
	public void doAdd(List<Document> documents) {
		List<float[]> embeddings = this.embeddingModel.embed(documents, EmbeddingOptionsBuilder.builder().build(),
				this.batchingStrategy);

		for (int i = 0; i < documents.size(); i++) {
			Document document = documents.get(i);
			float[] embedding = embeddings.get(i);
			knowledgeStore
				.putDocument(Utils.toTablestoreDocument(knowledgeStore.enableMultiTenant(), embedding, document));
		}
	}

	@Override
	public void doDelete(List<String> idList) {
		for (String id : idList) {
			knowledgeStore.deleteDocument(id);
		}
	}

	@Override
	protected void doDelete(Filter.Expression filterExpression) {
		Set<String> tenantIds = new HashSet<>();
		var metadataFilter = TablestoreExpressionConverter.convertOperand(filterExpression, tenantIds,
				knowledgeStore.enableMultiTenant());
		knowledgeStore.deleteDocument(tenantIds, metadataFilter);
	}

	@Override
	public List<Document> doSimilaritySearch(SearchRequest request) {
		Assert.notNull(request, "The search request must not be null.");
		String queryText = request.getQuery();
		float[] queryEmbed = embeddingModel.embed(queryText);
		int topK = request.getTopK();
		double similarityThreshold = request.getSimilarityThreshold();
		Set<String> tenantIds = new HashSet<>();
		var metadataFilter = TablestoreExpressionConverter.convertOperand(request.getFilterExpression(), tenantIds,
				knowledgeStore.enableMultiTenant());
		Map<String, Object> varArgs = new HashMap<>();
		varArgs.put(KnowledgeStoreImpl.FLAG_SKIP_WRAP_TENANT_IDS, true);
		Response<DocumentHit> hitResponse = knowledgeStore.vectorSearch(queryEmbed, topK, (float) similarityThreshold,
				tenantIds, metadataFilter, null, varArgs);
		List<DocumentHit> hits = hitResponse.getHits();
		List<Document> documents = new ArrayList<>(hits.size());
		for (DocumentHit hit : hits) {
			documents.add(Utils.toSpringAIDocument(hit));
		}
		return documents;
	}

	@Override
	public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
		VectorStoreObservationContext.Builder builder = VectorStoreObservationContext.builder("tablestore",
				operationName);
		builder.namespace(knowledgeStore.getTableName());
		builder.collectionName(knowledgeStore.getSearchIndexName());
		builder.dimensions(knowledgeStore.getEmbeddingDimension());
		builder.fieldName(knowledgeStore.getEmbeddingField());
		builder.similarityMetric(knowledgeStore.getEmbeddingMetricType().name());
		return builder;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (!initializeTable) {
			return;
		}
		knowledgeStore.initTable();
	}

	public static Builder builder(KnowledgeStoreImpl knowledgeStore, EmbeddingModel embeddingModel) {
		return new Builder(knowledgeStore, embeddingModel);
	}

	public static class Builder extends AbstractVectorStoreBuilder<Builder> {

		private final KnowledgeStoreImpl knowledgeStore;

		private boolean initializeTable = false;

		public Builder(KnowledgeStoreImpl knowledgeStore, EmbeddingModel embeddingModel) {
			super(embeddingModel);
			this.knowledgeStore = knowledgeStore;
		}

		public Builder initializeTable(boolean initializeTable) {
			this.initializeTable = initializeTable;
			return this;
		}

		@Override
		public TablestoreVectorStore build() {
			return new TablestoreVectorStore(this);
		}

	}

	public KnowledgeStoreImpl getKnowledgeStore() {
		return knowledgeStore;
	}

	public boolean isInitializeTable() {
		return initializeTable;
	}

}
