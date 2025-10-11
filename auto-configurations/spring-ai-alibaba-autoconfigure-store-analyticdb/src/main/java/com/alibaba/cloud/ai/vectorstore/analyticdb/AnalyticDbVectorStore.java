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

package com.alibaba.cloud.ai.vectorstore.analyticdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.aliyun.gpdb20160503.Client;
import com.aliyun.gpdb20160503.models.CreateCollectionRequest;
import com.aliyun.gpdb20160503.models.CreateNamespaceRequest;
import com.aliyun.gpdb20160503.models.DeleteCollectionDataRequest;
import com.aliyun.gpdb20160503.models.DeleteCollectionDataResponse;
import com.aliyun.gpdb20160503.models.DescribeCollectionRequest;
import com.aliyun.gpdb20160503.models.DescribeNamespaceRequest;
import com.aliyun.gpdb20160503.models.InitVectorDatabaseRequest;
import com.aliyun.gpdb20160503.models.InitVectorDatabaseResponse;
import com.aliyun.gpdb20160503.models.QueryCollectionDataRequest;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponse;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponseBody;
import com.aliyun.gpdb20160503.models.UpsertCollectionDataRequest;
import com.aliyun.gpdb20160503.models.UpsertCollectionDataRequest.UpsertCollectionDataRequestRows;
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

/**
 * Vector store implementation backed by AnalyticDB.
 *
 * @author saladday
 */
public class AnalyticDbVectorStore extends AbstractObservationVectorStore implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(AnalyticDbVectorStore.class);

	private static final String DATABASE_SYSTEM = "analytic_db";

	private static final String REF_DOC_NAME = "refDocId";

	private static final String METADATA_FIELD_NAME = "metadata";

	private static final String CONTENT_FIELD_NAME = "content";

	private static final String DOC_NAME = "docId";

	private static final int DEFAULT_TOP_K = 4;

	private static final Double DEFAULT_SIMILARITY_THRESHOLD = 0.0;

	private final FilterExpressionConverter filterExpressionConverter = new AdVectorFilterExpressionConverter();

	private final String collectionName;

	private final AnalyticDbConfig config;

	private final Client client;

	private final ObjectMapper objectMapper;

	private final Integer defaultTopK;

	private final Double defaultSimilarityThreshold;

	protected AnalyticDbVectorStore(Builder builder) throws Exception {
		super(builder);
		this.collectionName = builder.collectionName;
		this.config = builder.config;
		this.client = builder.client;
		this.objectMapper = JsonMapper.builder().addModules(JacksonUtils.instantiateAvailableModules()).build();
		this.defaultTopK = builder.defaultTopK;
		this.defaultSimilarityThreshold = builder.defaultSimilarityThreshold;
	}

	public static Builder builder(String collectionName, AnalyticDbConfig config, Client client,
			EmbeddingModel embeddingModel) {
		return new Builder(collectionName, config, client, embeddingModel);
	}

	private void initialize() throws Exception {
		initializeVectorDatabase();
		createNamespaceIfNotExists();
		createCollectionIfNotExists(this.embeddingModel.dimensions());
	}

	private void initializeVectorDatabase() throws Exception {
		InitVectorDatabaseRequest request = new InitVectorDatabaseRequest().setDBInstanceId(this.config.getDbInstanceId())
			.setRegionId(this.config.getRegionId())
			.setManagerAccount(this.config.getManagerAccount())
			.setManagerAccountPassword(this.config.getManagerAccountPassword());
		InitVectorDatabaseResponse response = this.client.initVectorDatabase(request);
		log.debug("Successfully initialised vector database, response body: {}", response.getBody());
	}

	private void createNamespaceIfNotExists() throws Exception {
		try {
			DescribeNamespaceRequest request = new DescribeNamespaceRequest().setDBInstanceId(this.config.getDbInstanceId())
				.setRegionId(this.config.getRegionId())
				.setNamespace(this.config.getNamespace())
				.setManagerAccount(this.config.getManagerAccount())
				.setManagerAccountPassword(this.config.getManagerAccountPassword());
			this.client.describeNamespace(request);
		}
		catch (TeaException ex) {
			if (Objects.equals(ex.getStatusCode(), 404)) {
				CreateNamespaceRequest request = new CreateNamespaceRequest()
					.setDBInstanceId(this.config.getDbInstanceId())
					.setRegionId(this.config.getRegionId())
					.setNamespace(this.config.getNamespace())
					.setManagerAccount(this.config.getManagerAccount())
					.setManagerAccountPassword(this.config.getManagerAccountPassword())
					.setNamespacePassword(this.config.getNamespacePassword());
				this.client.createNamespace(request);
			}
			throw new Exception("Failed to create namespace", ex);
		}
	}

	private void createCollectionIfNotExists(int embeddingDimension) throws Exception {
		try {
			DescribeCollectionRequest describeRequest = new DescribeCollectionRequest()
				.setDBInstanceId(this.config.getDbInstanceId())
				.setRegionId(this.config.getRegionId())
				.setNamespace(this.config.getNamespace())
				.setNamespacePassword(this.config.getNamespacePassword())
				.setCollection(this.collectionName);
			this.client.describeCollection(describeRequest);
			log.debug("Collection {} already exists", this.collectionName);
		}
		catch (TeaException ex) {
			if (Objects.equals(ex.getStatusCode(), 404)) {
				ObjectNode metadataNode = this.objectMapper.createObjectNode();
				metadataNode.put(REF_DOC_NAME, "text");
				metadataNode.put(CONTENT_FIELD_NAME, "text");
				metadataNode.put(METADATA_FIELD_NAME, "jsonb");
				String metadata = this.objectMapper.writeValueAsString(metadataNode);

				CreateCollectionRequest createRequest = new CreateCollectionRequest()
					.setDBInstanceId(this.config.getDbInstanceId())
					.setRegionId(this.config.getRegionId())
					.setManagerAccount(this.config.getManagerAccount())
					.setManagerAccountPassword(this.config.getManagerAccountPassword())
					.setNamespace(this.config.getNamespace())
					.setCollection(this.collectionName)
					.setDimension((long) embeddingDimension)
					.setMetrics(this.config.getMetrics())
					.setMetadata(metadata)
					.setFullTextRetrievalFields(CONTENT_FIELD_NAME);

				this.client.createCollection(createRequest);
				log.debug("Collection {} created", this.collectionName);
			}
			throw new RuntimeException("Failed to create collection " + this.collectionName + ": " + ex.getMessage(), ex);
		}
	}

	@Override
	public void doAdd(List<Document> documents) {
		Assert.notNull(documents, "The document list should not be null.");
		if (CollectionUtils.isEmpty(documents)) {
			return;
		}

		List<float[]> embeddings = this.embeddingModel.embed(documents, EmbeddingOptionsBuilder.builder().build(),
				this.batchingStrategy);

		List<UpsertCollectionDataRequestRows> rows = new ArrayList<>(documents.size());
		for (int index = 0; index < documents.size(); index++) {
			Document document = documents.get(index);
			log.info("Processing document id = {}", document.getId());

			Map<String, String> metadata = new HashMap<>();
			Map<String, Object> documentMetadata = document.getMetadata();
			String documentName = (String) documentMetadata.get(DOC_NAME);
			String refDocId = (documentName != null && !documentName.isEmpty()) ? documentName : document.getId();
			metadata.put(REF_DOC_NAME, refDocId);
			metadata.put(CONTENT_FIELD_NAME, document.getText());
			try {
				metadata.put(METADATA_FIELD_NAME, this.objectMapper.writeValueAsString(document.getMetadata()));
			}
			catch (JsonProcessingException ex) {
				throw new RuntimeException("Failed to serialise metadata for document id = " + document.getId(), ex);
			}

		float[] vector = embeddings.get(index);
		List<Double> embeddingVector = IntStream.range(0, vector.length)
			.mapToObj(i -> (double) vector[i])
			.collect(Collectors.toList());

		rows.add(new UpsertCollectionDataRequestRows().setVector(embeddingVector).setMetadata(metadata));
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
		catch (Exception ex) {
			throw new RuntimeException("Failed to add collection data: " + ex.getMessage(), ex);
		}
	}

	@Override
	public void doDelete(List<String> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return;
		}
		String idsExpression = ids.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", ", "(", ")"));
		DeleteCollectionDataRequest request = new DeleteCollectionDataRequest()
			.setDBInstanceId(this.config.getDbInstanceId())
			.setRegionId(this.config.getRegionId())
			.setNamespace(this.config.getNamespace())
			.setNamespacePassword(this.config.getNamespacePassword())
			.setCollection(this.collectionName)
			.setCollectionDataFilter(REF_DOC_NAME + " IN " + idsExpression);
		try {
			DeleteCollectionDataResponse response = this.client.deleteCollectionData(request);
			log.debug("Delete collection data response: {}", response.getBody());
		}
		catch (Exception ex) {
			throw new RuntimeException("Failed to delete collection data by IDs: " + ex.getMessage(), ex);
		}
	}

	@Override
	public void doDelete(Filter.Expression filterExpression) {
		String nativeFilter = this.filterExpressionConverter.convertExpression(filterExpression);
		DeleteCollectionDataRequest request = new DeleteCollectionDataRequest()
			.setDBInstanceId(this.config.getDbInstanceId())
			.setRegionId(this.config.getRegionId())
			.setNamespace(this.config.getNamespace())
			.setNamespacePassword(this.config.getNamespacePassword())
			.setCollection(this.collectionName)
			.setCollectionDataFilter(nativeFilter);
		try {
			DeleteCollectionDataResponse response = this.client.deleteCollectionData(request);
			log.debug("Delete collection data response: {}", response.getBody());
		}
		catch (Exception ex) {
			throw new RuntimeException("Failed to delete collection data by filter: " + ex.getMessage(), ex);
		}
	}

	@Override
	public List<Document> doSimilaritySearch(SearchRequest searchRequest) {
		double scoreThreshold = searchRequest.getSimilarityThreshold();
		boolean includeValues = searchRequest.hasFilterExpression();
		int topK = searchRequest.getTopK();
		String filterExpression = null;
		if (includeValues) {
			filterExpression = (searchRequest.getFilterExpression() != null)
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
			.setContent(searchRequest.getQuery())
			.setTopK((long) topK)
			.setFilter(filterExpression);

		try {
			QueryCollectionDataResponse response = this.client.queryCollectionData(request);
			List<Document> documents = new ArrayList<>();
			List<QueryCollectionDataResponseBody.QueryCollectionDataResponseBodyMatchesMatch> matches = response.getBody()
				.getMatches()
				.getMatch();
			for (QueryCollectionDataResponseBody.QueryCollectionDataResponseBodyMatchesMatch match : matches) {
				if (match.getScore() == null || match.getScore() <= scoreThreshold) {
					continue;
				}
				Map<String, String> metadata = match.getMetadata();
				String pageContent = metadata.get(CONTENT_FIELD_NAME);
				@SuppressWarnings("unchecked")
				Map<String, Object> metadataJson = this.objectMapper.readValue(metadata.get(METADATA_FIELD_NAME),
						new TypeReference<HashMap<String, Object>>() {
						});
				documents.add(new Document(pageContent, metadataJson));
			}
			return documents;
		}
		catch (Exception ex) {
			throw new RuntimeException("Failed to execute similarity search: " + ex.getMessage(), ex);
		}
	}

	@Override
	public List<Document> similaritySearch(String query) {
		return similaritySearch(SearchRequest.builder()
			.query(query)
			.topK(this.defaultTopK)
			.similarityThreshold(this.defaultSimilarityThreshold)
			.build());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initialize();
		log.debug("AnalyticDB vector store client initialised");
	}

	@Override
	public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
		return VectorStoreObservationContext.builder(DATABASE_SYSTEM, operationName)
			.collectionName(this.collectionName)
			.dimensions(this.embeddingModel.dimensions())
			.namespace(this.config.getNamespace())
			.similarityMetric(this.config.getMetrics());
	}

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
			Assert.hasText(collectionName, "Collection name must not be empty");
			this.collectionName = collectionName.toLowerCase();
			this.config = config;
		}

		public Builder defaultTopK(int defaultTopK) {
			Assert.isTrue(defaultTopK >= 0, "The topK should be greater than or equal to 0.");
			this.defaultTopK = defaultTopK;
			return this;
		}

		public Builder defaultSimilarityThreshold(Double defaultSimilarityThreshold) {
			Assert.isTrue(defaultSimilarityThreshold >= 0.0 && defaultSimilarityThreshold <= 1.0,
					"The similarity threshold must be in range [0.0, 1.0].");
			this.defaultSimilarityThreshold = defaultSimilarityThreshold;
			return this;
		}

		public AnalyticDbVectorStore build() {
			try {
				return new AnalyticDbVectorStore(this);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

	}

}
