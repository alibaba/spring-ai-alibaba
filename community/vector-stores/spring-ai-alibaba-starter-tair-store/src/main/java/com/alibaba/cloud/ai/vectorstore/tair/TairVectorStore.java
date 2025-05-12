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
package com.alibaba.cloud.ai.vectorstore.tair;

import com.aliyun.tair.tairvector.factory.VectorBuilderFactory;
import com.aliyun.tair.tairvector.factory.VectorBuilderFactory.KnnItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Provides an API for interacting with Tair Vector, extending the functionality of the
 * {@link AbstractObservationVectorStore} class. This class manages vector operations
 * using a TairVectorApi and an EmbeddingModel.
 *
 * @author fuyou.lxm
 * @since 1.0.0-M3
 */
public class TairVectorStore extends AbstractObservationVectorStore {

	private static final Logger logger = LoggerFactory.getLogger(TairVectorStore.class);

	/**
	 * The field name for the document ID.
	 */
	protected static final String ID_FIELD_NAME = "id";

	/**
	 * The field name for the document content.
	 */
	protected static final String CONTENT_FIELD_NAME = "content";

	/**
	 * The field name for the document metadata.
	 */
	protected static final String METADATA_FIELD_NAME = "metadata";

	/**
	 * The API client used to interact with Tair.
	 */
	protected final TairVectorApi tairVectorApi;

	/**
	 * The embedding model used for vector operations.
	 */
	protected final EmbeddingModel embeddingModel;

	/**
	 * Configuration options for the Tair vector store.
	 */
	protected TairVectorStoreOptions options;

	protected final BatchingStrategy batchingStrategy;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Constructs a new instance of TairVectorStore with the specified parameters.
	 * @param tairVectorApi The API client used to interact with Tair.
	 * @param embeddingModel The embedding model used for vector operations.
	 * @param observationRegistry The observation registry for metrics.
	 * @param customObservationConvention Custom observation convention for metrics.
	 * @param batchingStrategy The batching strategy used for processing documents.
	 */
	public TairVectorStore(TairVectorApi tairVectorApi, EmbeddingModel embeddingModel,
			ObservationRegistry observationRegistry, VectorStoreObservationConvention customObservationConvention,
			BatchingStrategy batchingStrategy) {
		this(builder(tairVectorApi, embeddingModel).observationRegistry(observationRegistry)
			.customObservationConvention(customObservationConvention)
			.batchingStrategy(batchingStrategy));
	}

	/**
	 * Constructs a new instance of TairVectorStore with the specified parameters.
	 * @param tairVectorApi The API client used to interact with Tair.
	 * @param embeddingModel The embedding model used for vector operations.
	 */
	public TairVectorStore(TairVectorApi tairVectorApi, EmbeddingModel embeddingModel) {
		this(builder(tairVectorApi, embeddingModel));
	}

	/**
	 * Initializes a new instance of the {@link TairVectorStore} class.
	 * @param builder The builder containing the necessary configuration for the
	 * TairVectorStore.
	 */
	protected TairVectorStore(Builder builder) {
		super(builder);

		Assert.notNull(builder.tairVectorApi, "The tairVectorClient cannot be null");

		this.options = builder.options;
		this.tairVectorApi = builder.tairVectorApi;
		this.embeddingModel = builder.getEmbeddingModel();
		this.batchingStrategy = builder.batchingStrategy;
	}

	/**
	 * Creates a new builder for the {@link TairVectorStore} class.
	 * @param tairVectorApi The TairVectorApi instance to be used.
	 * @param embeddingModel The EmbeddingModel instance to be used.
	 * @return A new builder instance.
	 */
	public static Builder builder(TairVectorApi tairVectorApi, EmbeddingModel embeddingModel) {
		return new Builder(tairVectorApi, embeddingModel);
	}

	@Override
	public void doAdd(List<Document> documents) {
		Objects.requireNonNull(documents, "Documents list cannot be null");
		if (documents.isEmpty()) {
			throw new IllegalArgumentException("Documents list cannot be empty");
		}

		for (Document document : documents) {
			logger.info("Calling EmbeddingModel for document id = {}", document.getId());
			float[] embedding = this.embeddingModel.embed(document);
			String embeddingString = null;
			try {
				embeddingString = objectMapper.writeValueAsString(embedding);

				List<String> params = new ArrayList<>();
				params.add(ID_FIELD_NAME);
				params.add(document.getId());
				params.add(CONTENT_FIELD_NAME);
				params.add(document.getText());
				params.add(METADATA_FIELD_NAME);
				params.add(objectMapper.writeValueAsString(document.getMetadata()));
				this.tairVectorApi.tvshset(options.getIndexName(), document.getId(), embeddingString,
						params.toArray(new String[0]));
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException("Error serializing message", e);
			}
		}
	}

	@Override
	public void doDelete(List<String> idList) {
		throw new UnsupportedOperationException("delete is not supported");
	}

	@Override
	public List<Document> doSimilaritySearch(SearchRequest request) {
		float[] userQueryEmbedding = getUserQueryEmbedding(request.getQuery());
		String embeddingString = null;
		try {
			embeddingString = objectMapper.writeValueAsString(userQueryEmbedding);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to serialize JSON", e);
		}
		VectorBuilderFactory.Knn<String> result = this.tairVectorApi.tvsknnsearch(options.getIndexName(),
				(long) request.getTopK(), embeddingString);

		return result.getKnnResults()
			.stream()
			.filter(item -> item.getScore() >= request.getSimilarityThreshold())
			.map(this::mapToDocument)
			.limit(request.getTopK())
			.toList();
	}

	/**
	 * Retrieves a document from the vector store based on a KnnItem.
	 * @param item The KnnItem containing the document ID.
	 * @return The document corresponding to the KnnItem.
	 */
	protected Document mapToDocument(KnnItem<String> item) {
		List<String> detail = this.tairVectorApi.tvshmget(options.getIndexName(), item.getId(), ID_FIELD_NAME,
				CONTENT_FIELD_NAME, METADATA_FIELD_NAME);
		String id = detail.get(0);
		String content = detail.get(1);
		String metadataStr = detail.get(2);
		Map<String, Object> metaData = null;
		try {
			metaData = objectMapper.readValue(metadataStr, new TypeReference<Map<String, Object>>() {
			});
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to parse JSON", e);
		}
		return new Document(id, content, metaData);
	}

	/**
	 * Generates an embedding for a user query.
	 * @param query The user query string.
	 * @return The embedding for the user query.
	 */
	protected float[] getUserQueryEmbedding(String query) {
		return this.embeddingModel.embed(query);
	}

	@Override
	public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
		return null;
	}

	/**
	 * Builder class for constructing {@link TairVectorStore} instances.
	 */
	public static class Builder extends AbstractVectorStoreBuilder<Builder> {

		protected TairVectorStoreOptions options = new TairVectorStoreOptions();

		protected final TairVectorApi tairVectorApi;

		protected BatchingStrategy batchingStrategy = new TokenCountBatchingStrategy();

		/**
		 * Initializes a new instance of the {@link Builder} class.
		 * @param tairVectorApi The TairVectorApi instance to be used.
		 * @param embeddingModel The EmbeddingModel instance to be used.
		 */
		public Builder(TairVectorApi tairVectorApi, EmbeddingModel embeddingModel) {
			super(embeddingModel);
			this.tairVectorApi = tairVectorApi;
		}

		/**
		 * Sets the Tair vector store options.
		 * @param options The vector store options to use.
		 * @return The builder instance.
		 * @throws IllegalArgumentException if options is null.
		 */
		public Builder options(TairVectorStoreOptions options) {
			Assert.notNull(options, "options must not be null");
			this.options = options;
			return this;
		}

		/**
		 * Sets the batching strategy.
		 * @param batchingStrategy The strategy to use.
		 * @return The builder instance.
		 */
		public Builder batchingStrategy(BatchingStrategy batchingStrategy) {
			Assert.notNull(batchingStrategy, "BatchingStrategy must not be null");
			this.batchingStrategy = batchingStrategy;
			return this;
		}

		/**
		 * Builds the TairVectorStore instance.
		 * @return A new TairVectorStore instance.
		 * @throws IllegalStateException if the builder is in an invalid state.
		 */
		@Override
		public TairVectorStore build() {
			return new TairVectorStore(this);
		}

	}

}
