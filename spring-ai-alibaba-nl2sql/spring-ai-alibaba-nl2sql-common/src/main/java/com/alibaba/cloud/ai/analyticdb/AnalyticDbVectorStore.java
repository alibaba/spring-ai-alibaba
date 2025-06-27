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
package com.alibaba.cloud.ai.analyticdb;

import com.aliyun.gpdb20160503.Client;
import com.aliyun.gpdb20160503.models.*;
import com.aliyun.tea.TeaException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author HeYQ
 * @since 2024-10-23 20:29
 */
public class AnalyticDbVectorStore extends AbstractObservationVectorStore implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(AnalyticDbVectorStore.class);

	private static final String DATA_BASE_SYSTEM = "analytic_db";

	private static final String REF_DOC_NAME = "refDocId";

	private static final String METADATA_FIELD_NAME = "metadata";

	private static final String CONTENT_FIELD_NAME = "content";

	private static final String DOC_NAME = "docId";

	private static final int DEFAULT_TOP_K = 4;

	private static final Double DEFAULT_SIMILARITY_THRESHOLD = 0.0;

	public final FilterExpressionConverter filterExpressionConverter = new AdVectorFilterExpressionConverter();

	// private final boolean initializeSchema;

	private final String collectionName;

	private final AnalyticDbConfig config;

	private final Client client;

	private final ObjectMapper objectMapper;

	private final Integer defaultTopK;

	private final Double defaultSimilarityThreshold;

	protected AnalyticDbVectorStore(Builder builder) throws Exception {
		super(builder);
		// collection_name must be updated every time
		this.collectionName = builder.collectionName;
		this.config = builder.config;
		this.client = builder.client;
		this.objectMapper = JsonMapper.builder().addModules(JacksonUtils.instantiateAvailableModules()).build();
		this.defaultSimilarityThreshold = builder.defaultSimilarityThreshold;
		this.defaultTopK = builder.defaultTopK;
	}

	public static Builder builder(String collectionName, AnalyticDbConfig config, Client client,
			EmbeddingModel embeddingModel) {
		return new Builder(collectionName, config, client, embeddingModel);
	}

	/**
	 * initialize vector db
	 */
	private void initialize() throws Exception {
		initializeVectorDataBase();
		createNameSpaceIfNotExists();
		createCollectionIfNotExists((long) this.embeddingModel.dimensions());
	}

	private void initializeVectorDataBase() throws Exception {
		InitVectorDatabaseRequest request = new InitVectorDatabaseRequest().setDBInstanceId(config.getDbInstanceId())
			.setRegionId(config.getRegionId())
			.setManagerAccount(config.getManagerAccount())
			.setManagerAccountPassword(config.getManagerAccountPassword());
		InitVectorDatabaseResponse initVectorDatabaseResponse = client.initVectorDatabase(request);
		logger.debug("successfully initialize vector database, response body:{}", initVectorDatabaseResponse.getBody());

	}

	private void createNameSpaceIfNotExists() throws Exception {
		try {
			DescribeNamespaceRequest request = new DescribeNamespaceRequest()
				.setDBInstanceId(this.config.getDbInstanceId())
				.setRegionId(this.config.getRegionId())
				.setNamespace(this.config.getNamespace())
				.setManagerAccount(this.config.getManagerAccount())
				.setManagerAccountPassword(this.config.getManagerAccountPassword());
			this.client.describeNamespace(request);
		}
		catch (TeaException e) {
			if (Objects.equals(e.getStatusCode(), 404)) {
				CreateNamespaceRequest request = new CreateNamespaceRequest()
					.setDBInstanceId(this.config.getDbInstanceId())
					.setRegionId(this.config.getRegionId())
					.setNamespace(this.config.getNamespace())
					.setManagerAccount(this.config.getManagerAccount())
					.setManagerAccountPassword(this.config.getManagerAccountPassword())
					.setNamespacePassword(this.config.getNamespacePassword());
				this.client.createNamespace(request);
			}
			else {
				throw new Exception("failed to create namespace:{}", e);
			}
		}
	}

	private void createCollectionIfNotExists(Long embeddingDimension) throws Exception {
		try {
			// Describe the collection to check if it exists
			DescribeCollectionRequest describeRequest = new DescribeCollectionRequest()
				.setDBInstanceId(this.config.getDbInstanceId())
				.setRegionId(this.config.getRegionId())
				.setNamespace(this.config.getNamespace())
				.setNamespacePassword(this.config.getNamespacePassword())
				.setCollection(this.collectionName);
			this.client.describeCollection(describeRequest);
			logger.debug("collection" + this.collectionName + "already exists");
		}
		catch (TeaException e) {
			if (Objects.equals(e.getStatusCode(), 404)) {
				// Collection does not exist, create it
				ObjectNode metadataNode = objectMapper.createObjectNode();
				metadataNode.put(REF_DOC_NAME, "text");
				metadataNode.put(CONTENT_FIELD_NAME, "text");
				metadataNode.put(METADATA_FIELD_NAME, "jsonb");
				String metadata = objectMapper.writeValueAsString(metadataNode);
				CreateCollectionRequest createRequest = new CreateCollectionRequest()
					.setDBInstanceId(this.config.getDbInstanceId())
					.setRegionId(this.config.getRegionId())
					.setManagerAccount(this.config.getManagerAccount())
					.setManagerAccountPassword(this.config.getManagerAccountPassword())
					.setNamespace(this.config.getNamespace())
					.setCollection(this.collectionName)
					.setDimension(embeddingDimension)
					.setMetrics(this.config.getMetrics())
					.setMetadata(metadata)
					.setFullTextRetrievalFields(CONTENT_FIELD_NAME);
				this.client.createCollection(createRequest);
				logger.debug("collection" + this.collectionName + "created");
			}
			else {
				throw new RuntimeException(
						"Failed to create collection " + this.collectionName + ": " + e.getMessage());
			}
		}
	}

	@Override
	public void doAdd(List<Document> documents) {
		Assert.notNull(documents, "The document list should not be null.");
		if (CollectionUtils.isEmpty(documents)) {
			return; // nothing to do;
		}
		List<float[]> embeddings = this.embeddingModel.embed(documents, EmbeddingOptionsBuilder.builder().build(),
				this.batchingStrategy);

		List<UpsertCollectionDataRequest.UpsertCollectionDataRequestRows> rows = new ArrayList<>(10);
		for (int i = 0; i < documents.size(); i++) {
			Document doc = documents.get(i);
			logger.info("Processing document id = {}", doc.getId());

			Map<String, String> metadata = new HashMap<>();
			String refDocId;
			Map<String, Object> docMetadata = doc.getMetadata();
			String docName = (String) docMetadata.get(DOC_NAME);
			refDocId = docName != null && !docName.isEmpty() ? docName : doc.getId();
			metadata.put(REF_DOC_NAME, refDocId);
			metadata.put(CONTENT_FIELD_NAME, doc.getText());
			try {
				metadata.put(METADATA_FIELD_NAME, objectMapper.writeValueAsString(doc.getMetadata()));
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to serialize metadata for document id = " + doc.getId(), e);
			}

			float[] floatEmbeddings = embeddings.get(i);
			List<Double> embedding = IntStream.range(0, floatEmbeddings.length)
				.mapToObj(j -> (double) floatEmbeddings[j])
				.toList();

			rows.add(new UpsertCollectionDataRequest.UpsertCollectionDataRequestRows().setVector(embedding)
				.setMetadata(metadata));
		}
		UpsertCollectionDataRequest request = new UpsertCollectionDataRequest()
			.setDBInstanceId(this.config.getDbInstanceId())
			.setRegionId(this.config.getRegionId())
			.setNamespace(this.config.getNamespace())
			.setNamespacePassword(this.config.getNamespacePassword())
			.setCollection(this.collectionName)
			.setRows(rows);
		try {
			this.client.upsertCollectionData(request);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to add collection data by IDs: " + e.getMessage(), e);
		}
	}

	@Override
	public void doDelete(List<String> ids) {
		if (ids.isEmpty()) {
			return;
		}
		String idsStr = ids.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", ", "(", ")"));
		DeleteCollectionDataRequest request = new DeleteCollectionDataRequest()
			.setDBInstanceId(this.config.getDbInstanceId())
			.setRegionId(this.config.getRegionId())
			.setNamespace(this.config.getNamespace())
			.setNamespacePassword(this.config.getNamespacePassword())
			.setCollection(this.collectionName)
			.setCollectionData(null)
			.setCollectionDataFilter("refDocId IN " + idsStr);
		try {
			DeleteCollectionDataResponse deleteCollectionDataResponse = this.client.deleteCollectionData(request);
			logger.debug("delete collection data response:{}", deleteCollectionDataResponse.getBody());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to delete collection data by IDs: " + e.getMessage(), e);
		}
	}

	@Override
	public void doDelete(Filter.Expression filterExpression) {
		String nativeFilterExpression = this.filterExpressionConverter.convertExpression(filterExpression);
		DeleteCollectionDataRequest request = new DeleteCollectionDataRequest()
			.setDBInstanceId(this.config.getDbInstanceId())
			.setRegionId(this.config.getRegionId())
			.setNamespace(this.config.getNamespace())
			.setNamespacePassword(this.config.getNamespacePassword())
			.setCollection(this.collectionName)
			.setCollectionData(null)
			.setCollectionDataFilter(nativeFilterExpression);
		try {
			DeleteCollectionDataResponse deleteCollectionDataResponse = this.client.deleteCollectionData(request);
			logger.debug("delete collection data response:{}", deleteCollectionDataResponse.getBody());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to delete collection data by filterExpression: " + e.getMessage(), e);
		}
	}

	@Override
	public List<Document> similaritySearch(String query) {
		return this.similaritySearch(SearchRequest.builder()
			.query(query)
			.topK(this.defaultTopK)
			.similarityThreshold(this.defaultSimilarityThreshold)
			.build());
	}

	@Override
	public List<Document> doSimilaritySearch(SearchRequest searchRequest) {
		double scoreThreshold = searchRequest.getSimilarityThreshold();
		boolean includeValues = searchRequest.hasFilterExpression();
		int topK = searchRequest.getTopK();
		String filterExpress = null;
		if (includeValues) {
			filterExpress = (searchRequest.getFilterExpression() != null)
					? this.filterExpressionConverter.convertExpression(searchRequest.getFilterExpression()) : "";
		}

		QueryCollectionDataRequest request = new QueryCollectionDataRequest()
			.setDBInstanceId(this.config.getDbInstanceId())
			.setRegionId(this.config.getRegionId())
			.setNamespace(this.config.getNamespace())
			.setNamespacePassword(this.config.getNamespacePassword())
			.setCollection(this.collectionName)
			.setIncludeValues(includeValues)
			.setMetrics(this.config.getMetrics())
			.setVector(null)
			.setContent(searchRequest.getQuery())
			.setTopK((long) topK)
			.setFilter(filterExpress);
		try {
			QueryCollectionDataResponse response = this.client.queryCollectionData(request);
			List<Document> documents = new ArrayList<>();
			for (QueryCollectionDataResponseBody.QueryCollectionDataResponseBodyMatchesMatch match : response.getBody()
				.getMatches()
				.getMatch()) {
				if (match.getScore() != null && match.getScore() > scoreThreshold) {
					Map<String, String> metadata = match.getMetadata();
					String pageContent = metadata.get(CONTENT_FIELD_NAME);
					Map<String, Object> metadataJson = objectMapper.readValue(metadata.get(METADATA_FIELD_NAME),
							new TypeReference<HashMap<String, Object>>() {
							});
					Document doc = new Document(pageContent, metadataJson);
					documents.add(doc);
				}
			}
			return documents;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to search by full text: " + e.getMessage(), e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initialize();
		logger.debug("created AnalyticdbVector client success");
	}

	@Override
	public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {

		return VectorStoreObservationContext.builder(DATA_BASE_SYSTEM, operationName)
			.collectionName(this.collectionName)
			.dimensions(this.embeddingModel.dimensions())
			.namespace(this.config.getNamespace())
			.similarityMetric(this.config.getMetrics());
	}

	/**
	 * Builder class for creating {@link AnalyticDbVectorStore} instances.
	 * <p>
	 * Provides a fluent API for configuring all aspects of the Analyticdb vector store.
	 *
	 * @since 1.0.0
	 */
	public static class Builder extends AbstractVectorStoreBuilder<Builder> {

		private final String collectionName;

		private final AnalyticDbConfig config;

		private final Client client;

		private int defaultTopK = DEFAULT_TOP_K;

		private Double defaultSimilarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;

		private Builder(String collectionName, AnalyticDbConfig config, Client client, EmbeddingModel embeddingModel) {
			super(embeddingModel);
			Assert.notNull(client, "Client must not be null");
			this.client = client;
			Assert.notNull(collectionName, "Collection name must not be null");
			this.collectionName = collectionName.toLowerCase();
			this.config = config;
		}

		/**
		 * Sets the default maximum number of similar documents to return.
		 * @param defaultTopK the maximum number of documents
		 * @return the builder instance
		 * @throws IllegalArgumentException if defaultTopK is negative
		 */
		public Builder defaultTopK(int defaultTopK) {
			Assert.isTrue(defaultTopK >= 0, "The topK should be positive value.");
			this.defaultTopK = defaultTopK;
			return this;
		}

		/**
		 * Sets the default similarity threshold for returned documents.
		 * @param defaultSimilarityThreshold the similarity threshold (must be between 0.0
		 * and 1.0)
		 * @return the builder instance
		 * @throws IllegalArgumentException if defaultSimilarityThreshold is not between
		 * 0.0 and 1.0
		 */
		public Builder defaultSimilarityThreshold(Double defaultSimilarityThreshold) {
			Assert.isTrue(defaultSimilarityThreshold >= 0.0 && defaultSimilarityThreshold <= 1.0,
					"The similarity threshold must be in range [0.0:1.00].");
			this.defaultSimilarityThreshold = defaultSimilarityThreshold;
			return this;
		}

		@Override
		public AnalyticDbVectorStore build() {
			try {
				return new AnalyticDbVectorStore(this);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

}
