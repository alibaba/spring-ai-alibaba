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
package com.alibaba.cloud.ai.vectorstore.opensearch;

import com.aliyun.ha3engine.vector.models.QueryRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.observation.conventions.VectorStoreProvider;
import org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the VectorStore interface for Alibaba OpenSearch. This class provides
 * methods to add, delete, and perform similarity searches on documents in an OpenSearch
 * index.
 *
 * @author fuyou.lxm
 * @since 1.0.0-M3
 */
public class OpenSearchVectorStore extends AbstractObservationVectorStore implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(OpenSearchVectorStore.class);

	/**
	 * The field name for the document ID.
	 */
	private static final String ID_FIELD_NAME = "id";

	/**
	 * The field name for the document content.
	 */
	private static final String CONTENT_FIELD_NAME = "content";

	/**
	 * The field name for the document metadata.
	 */
	private static final String METADATA_FIELD_NAME = "metadata";

	/**
	 * The API client used to interact with OpenSearch.
	 */
	private final com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchApi openSearchApi;

	/**
	 * Configuration options for the OpenSearch vector store.
	 */
	private final OpenSearchVectorStoreOptions options;

	/**
	 * The embedding model used for vector operations.
	 */
	private final EmbeddingModel embeddingModel;

	/**
	 * The batching strategy used for processing documents.
	 */
	private final BatchingStrategy batchingStrategy;

	/**
	 * Converter that transforms a JSON object representing a document into a
	 * {@link OpenSearchApi.SimilarityResult} object.
	 */
	private final Converter<JsonNode, com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchApi.SimilarityResult> itemConverter = new SimilarityResultConverter();

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Constructs a new instance of OpenSearchVectorStore with the specified parameters.
	 * @param openSearchApi The API client used to interact with OpenSearch.
	 * @param embeddingModel The embedding model used for vector operations.
	 * @param observationRegistry The observation registry for metrics.
	 * @param customObservationConvention Custom observation convention for metrics.
	 * @param batchingStrategy The batching strategy used for processing documents.
	 */
	public OpenSearchVectorStore(OpenSearchApi openSearchApi, EmbeddingModel embeddingModel,
			ObservationRegistry observationRegistry, VectorStoreObservationConvention customObservationConvention,
			BatchingStrategy batchingStrategy) {
		this(builder(openSearchApi, embeddingModel).observationRegistry(observationRegistry)
			.customObservationConvention(customObservationConvention)
			.batchingStrategy(batchingStrategy));
	}

	/**
	 * Constructs a new instance of OpenSearchVectorStore with the specified parameters.
	 * @param openSearchApi The API client used to interact with OpenSearch.
	 * @param embeddingModel The embedding model used for vector operations.
	 */
	public OpenSearchVectorStore(com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchApi openSearchApi,
			EmbeddingModel embeddingModel) {
		this(builder(openSearchApi, embeddingModel));
	}

	/**
	 * Protected constructor for building instances of OpenSearchVectorStore using the
	 * Builder pattern.
	 * @param builder The builder instance containing configuration options.
	 */
	protected OpenSearchVectorStore(Builder builder) {
		super(builder);

		Assert.notNull(builder.openSearchApi, "The openSearchApi cannot be null");
		Assert.notNull(builder.options.getPrimaryKeyField(), "The primaryKeyField cannot be null");
		Assert.notNull(builder.options.getTableName(), "The tableName cannot be null");

		this.options = builder.options;
		this.openSearchApi = builder.openSearchApi;
		this.embeddingModel = builder.getEmbeddingModel();
		this.batchingStrategy = builder.batchingStrategy;
	}

	/**
	 * Creates a new Builder instance for constructing OpenSearchVectorStore objects.
	 * @param openSearchApi The API client used to interact with OpenSearch.
	 * @param embeddingModel The embedding model used for vector operations.
	 * @return A new Builder instance.
	 */
	public static Builder builder(OpenSearchApi openSearchApi, EmbeddingModel embeddingModel) {
		return new Builder(openSearchApi, embeddingModel);
	}

	@Override
	public void doAdd(List<Document> documents) {
		for (Document document : documents) {
			/*
			 * Document push outer structure, can add document operation structures. The
			 * structure supports one or more document operations.
			 */
			List<Map<String, ?>> documentToAdd = new ArrayList<>();
			Map<String, Object> documentMap = new HashMap<>();
			Map<String, Object> documentFields = new HashMap<>();

			// Insert document content information, key-value pairs matching.
			// The field_pk field must be consistent with the pkField configuration.
			documentFields.put(ID_FIELD_NAME, document.getId());
			documentFields.put(CONTENT_FIELD_NAME, document.getText());
			// Convert metadata to JSON
			try {
				documentFields.put(METADATA_FIELD_NAME, objectMapper.writeValueAsString(document.getMetadata()));
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException("Failed to serialize JSON", e);
			}

			// Add document content to documentEntry structure.
			documentMap.put("fields", documentFields);
			// New document command: add
			documentMap.put("cmd", "add");
			documentToAdd.add(documentMap);

			openSearchApi.uploadDocument(this.options.getTableName(), this.options.getPrimaryKeyField(), documentToAdd);
		}
	}

	/**
	 * Delete documents from the vector store.
	 */
	@Override
	public void doDelete(List<String> idList) {
		for (String id : idList) {
			List<Map<String, ?>> documentToDelete = new ArrayList<>();
			Map<String, Object> documentMap = new HashMap<>();
			Map<String, Object> documentFields = new HashMap<>();

			documentFields.put(this.options.getPrimaryKeyField(), id);
			documentMap.put("fields", documentFields);
			documentMap.put("cmd", "delete");
			documentToDelete.add(documentMap);

			openSearchApi.deleteDocument(this.options.getTableName(), this.options.getPrimaryKeyField(),
					documentToDelete);
		}
	}

	/**
	 * Perform a similarity search in the vector store.
	 * @param request The search request containing the query and parameters.
	 * @return list of documents
	 */
	@Override
	public List<Document> doSimilaritySearch(SearchRequest request) {
		Assert.notNull(request, "The search request must not be null.");

		// set similarityThreshold
		var similarityThreshold = request.getSimilarityThreshold();

		QueryRequest queryRequest = new QueryRequest();
		queryRequest.setTableName(this.options.getTableName()); // Required, the name of
		// the table to
		// query
		queryRequest.setContent(request.getQuery());
		queryRequest.setModal("text"); // Required, used for vectorizing the query term
		queryRequest.setTopK(request.getTopK()); // number of results to return
		queryRequest.setOutputFields(this.options.getOutputFields());

		try {
			List<com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchApi.SimilarityResult> similarityResults = openSearchApi
				.search(queryRequest, itemConverter);

			return similarityResults.stream()
				.filter(result -> result.score() >= similarityThreshold)
				.map(result -> new Document(result.id(), result.content(), result.metadata()))
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create an ObservationContextBuilder for OpenSearchVectorStore.
	 * @return VectorStoreObservationContext.Builder
	 */
	@Override
	public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
		return VectorStoreObservationContext.builder(VectorStoreProvider.OPENSEARCH.value(), operationName)
			.collectionName(this.options.getIndex())
			.dimensions(this.embeddingModel.dimensions())
			.similarityMetric(getSimilarityFunction());
	}

	private String getSimilarityFunction() {
		if ("cosinesimil".equalsIgnoreCase(this.options.getSimilarityFunction())) {
			return VectorStoreSimilarityMetric.COSINE.value();
		}
		else if ("l2".equalsIgnoreCase(this.options.getSimilarityFunction())) {
			return VectorStoreSimilarityMetric.EUCLIDEAN.value();
		}

		return this.options.getSimilarityFunction();
	}

	public boolean exists(String tableName) throws Exception {
		List<Object> indexList = openSearchApi.getIndexList(tableName);
		return indexList.contains("saa_default_index");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.options.isInitializeSchema() && !exists(this.options.getIndex())) {
			openSearchApi.createCollectionAndIndex(this.options.getMappingJson());
		}
	}

	/**
	 * Get the native client.
	 * @return Client
	 */
	@Override
	public <T> Optional<T> getNativeClient() {
		@SuppressWarnings("unchecked")
		T client = (T) this.openSearchApi;
		return Optional.of(client);
	}

	/**
	 * A builder class for constructing instances of {@link OpenSearchVectorStore}. This
	 * class extends {@link AbstractVectorStoreBuilder} and provides methods to configure
	 * the options and batching strategy for the OpenSearch vector store.
	 */
	public static class Builder extends AbstractVectorStoreBuilder<Builder> {

		private final com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchApi openSearchApi;

		private OpenSearchVectorStoreOptions options = new OpenSearchVectorStoreOptions();

		private BatchingStrategy batchingStrategy = new TokenCountBatchingStrategy();

		/**
		 * Constructs a new instance of the Builder.
		 * @param openSearchApi The API client used to interact with OpenSearch.
		 * @param embeddingModel The embedding model used for vector operations.
		 */
		public Builder(com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchApi openSearchApi,
				EmbeddingModel embeddingModel) {
			super(embeddingModel);
			this.openSearchApi = openSearchApi;
		}

		/**
		 * Sets the configuration options for the OpenSearch vector store.
		 * @param options The configuration options to set.
		 * @return The current Builder instance.
		 * @throws IllegalArgumentException if options is null.
		 */
		public Builder options(OpenSearchVectorStoreOptions options) {
			Assert.notNull(options, "options must not be null");
			this.options = options;
			return this;
		}

		/**
		 * Sets the batching strategy for the OpenSearch vector store.
		 * @param batchingStrategy The batching strategy to set.
		 * @return The current Builder instance.
		 * @throws IllegalArgumentException if batchingStrategy is null.
		 */
		public Builder batchingStrategy(BatchingStrategy batchingStrategy) {
			Assert.notNull(batchingStrategy, "BatchingStrategy must not be null");
			this.batchingStrategy = batchingStrategy;
			return this;
		}

		/**
		 * Builds and returns a new instance of {@link OpenSearchVectorStore} configured
		 * with the current settings.
		 * @return A new instance of OpenSearchVectorStore.
		 */
		@Override
		public OpenSearchVectorStore build() {
			return OpenSearchVectorStore.builder(this.openSearchApi, this.embeddingModel).build();
		}

	}

	/**
	 * A converter that transforms a JSON object representing a document into a
	 * {@link OpenSearchApi.SimilarityResult} object. This class is used to parse the
	 * response from an OpenSearch similarity search query and convert it into a more
	 * usable format.
	 */
	public static class SimilarityResultConverter
			implements Converter<JsonNode, com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchApi.SimilarityResult> {

		private static final String EMPTY_TEXT = "";

		private static final String FIELDS_KEY = "fields";

		private static final String SCORE_KEY = "score";

		/**
		 * Extracts the content from the JSON document.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return The content of the document, or empty string if not found or empty.
		 */
		private static String extractContent(JsonNode jsonDocument) {
			if (jsonDocument.has(FIELDS_KEY)) {
				JsonNode fields = jsonDocument.get(FIELDS_KEY);
				String content = fields.path(CONTENT_FIELD_NAME).asText();
				if (content == null || content.isEmpty()) {
					return EMPTY_TEXT;
				}
				return content;
			}

			return EMPTY_TEXT;
		}

		/**
		 * Extracts the ID from the JSON document.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return The ID of the document, or empty string if not found or empty.
		 */
		private static String extractId(JsonNode jsonDocument) {
			String id = jsonDocument.path(ID_FIELD_NAME).asText();
			if (id == null || id.isEmpty()) {
				return EMPTY_TEXT;
			}
			return id;
		}

		/**
		 * Extracts the score from the JSON document.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return The score of the document.
		 */
		private static double extractScore(JsonNode jsonDocument) {
			return jsonDocument.path(SCORE_KEY).asDouble();
		}

		/**
		 * Extracts the metadata from the JSON document.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return A map of metadata extracted from the document, or an empty map if not
		 * found.
		 */
		@SuppressWarnings("unchecked")
		private static Map<String, Object> extractMetadata(JsonNode jsonDocument) {
			if (jsonDocument.has(FIELDS_KEY)) {
				JsonNode fields = jsonDocument.get(FIELDS_KEY);
				String metadataStr = fields.path(METADATA_FIELD_NAME).asText();
				try {
					return new ObjectMapper().readValue(metadataStr, HashMap.class);
				}
				catch (JsonProcessingException e) {
					return new HashMap<>();
				}
			}
			else {
				return new HashMap<>();
			}
		}

		/**
		 * Parses a single JSON object representing a document into a
		 * {@link OpenSearchApi.SimilarityResult} object.
		 * @param jsonDocument The JSON object containing the document details.
		 * @return A {@link OpenSearchApi.SimilarityResult} object extracted from the JSON
		 * document.
		 */
		@Override
		public com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchApi.SimilarityResult convert(
				JsonNode jsonDocument) {
			String id = extractId(jsonDocument);
			String content = extractContent(jsonDocument);
			double score = extractScore(jsonDocument);
			Map<String, Object> metadata = extractMetadata(jsonDocument);

			return new com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchApi.SimilarityResult(id, score, content,
					metadata);
		}

	}

}
