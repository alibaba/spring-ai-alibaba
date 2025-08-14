/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.rag.vectorstore.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.DocumentChunk;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.IndexConfig;
import com.alibaba.cloud.ai.studio.core.model.embedding.DefaultBatchingStrategy;
import com.alibaba.cloud.ai.studio.core.model.embedding.EmbeddingModelDimension;
import com.alibaba.cloud.ai.studio.core.model.llm.ModelFactory;
import com.alibaba.cloud.ai.studio.core.rag.RagConstants;
import com.alibaba.cloud.ai.studio.core.rag.vectorstore.VectorStoreService;
import com.alibaba.cloud.ai.studio.core.rag.DocumentChunkConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchAiSearchFilterExpressionConverter;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.*;

/**
 * Elasticsearch vector store service implementation. Provides functionality for managing
 * vector indices and document chunks in Elasticsearch. Note: Spring AI does not support
 * index management, so we implement it ourselves.
 *
 * @since 1.0.0.3
 */
@Service
@Slf4j
@Qualifier("elasticSearchVectorStoreService")
public class ElasticSearchVectorStoreService implements VectorStoreService {

	/** Factory for creating embedding models */
	private final ModelFactory modelFactory;

	/** Elasticsearch client for index operations */
	private final ElasticsearchClient elasticsearchClient;

	/** REST client for vector store operations */
	private final RestClient restClient;

	/** Converter for filter expressions to Elasticsearch queries */
	private final FilterExpressionConverter filterExpressionConverter = new ElasticsearchAiSearchFilterExpressionConverter();

	public ElasticSearchVectorStoreService(ModelFactory modelFactory, ElasticsearchClient elasticsearchClient,
			RestClient restClient) {
		this.modelFactory = modelFactory;
		this.elasticsearchClient = elasticsearchClient;
		this.restClient = restClient;
	}

	/**
	 * Creates a new Elasticsearch index with vector search capabilities
	 * @param indexConfig Configuration for the index including name and embedding model
	 */
	@Override
	public void createIndex(IndexConfig indexConfig) {
		String indexName = indexConfig.getName();

		if (StringUtils.isBlank(indexName)) {
			throw new IllegalArgumentException("Elastic search index name must be provided");
		}

		ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
		options.setIndexName(indexName);
		options.setSimilarity(SimilarityFunction.dot_product);

		String similarityAlgo = SimilarityFunction.cosine.name();
		IndexSettings indexSettings = IndexSettings
			.of(settings -> settings.numberOfShards(String.valueOf(1)).numberOfReplicas(String.valueOf(1)));

		// Maybe using json directly?
		int dimension = EmbeddingModelDimension.getDimension(indexConfig.getEmbeddingModel(), DEFAULT_DIMENSION);
		Map<String, Property> properties = new HashMap<>();
		properties.put(RagConstants.VECTOR_FIELD, Property.of(property -> property.denseVector(
				DenseVectorProperty.of(dense -> dense.index(true).dims(dimension).similarity(similarityAlgo)))));
		properties.put(RagConstants.TEXT_FIELD, Property.of(property -> property.text(TextProperty.of(t -> t))));

		Map<String, Property> metadata = new HashMap<>();
		metadata.put(KEY_WORKSPACE_ID, Property.of(property -> property.keyword(KeywordProperty.of(k -> k))));
		metadata.put(KEY_DOC_ID, Property.of(property -> property.keyword(KeywordProperty.of(k -> k))));
		metadata.put(KEY_ENABLED, Property.of(property -> property.keyword(KeywordProperty.of(k -> k))));
		metadata.put(KEY_CHUNK_INDEX, Property.of(property -> property.keyword(KeywordProperty.of(k -> k))));

		properties.put("metadata",
				Property.of(property -> property.object(ObjectProperty.of(op -> op.properties(metadata)))));

		CreateIndexResponse indexResponse;
		try {
			indexResponse = elasticsearchClient.indices()
				.create(createIndexBuilder -> createIndexBuilder.index(indexName)
					.settings(indexSettings)
					.mappings(TypeMapping.of(mappings -> mappings.properties(properties))));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (!indexResponse.acknowledged()) {
			throw new RuntimeException("failed to create index");
		}

		log.info("create elasticsearch index {} successfully", indexName);
	}

	/**
	 * Deletes an existing Elasticsearch index
	 * @param indexConfig Configuration containing the index name to delete
	 */
	@Override
	public void deleteIndex(IndexConfig indexConfig) {
		String indexName = indexConfig.getName();
		try {
			elasticsearchClient.indices().delete(idx -> idx.index(indexName));
		}
		catch (ElasticsearchException ex) {
			if (ex.response().status() == 404) {
				log.warn("index {} not found", indexName);
			}
			else {
				throw new RuntimeException(ex);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates and returns a vector store instance for the specified index
	 * @param indexConfig Configuration for the index
	 * @return Configured vector store instance
	 */
	@Override
	public VectorStore getVectorStore(IndexConfig indexConfig) {
		EmbeddingModel embeddingModel = modelFactory.getEmbeddingModel(MetadataMode.EMBED, indexConfig);

		int dimension = EmbeddingModelDimension.getDimension(indexConfig.getEmbeddingModel(), DEFAULT_DIMENSION);
		ElasticsearchVectorStoreOptions storeOptions = new ElasticsearchVectorStoreOptions();
		storeOptions.setIndexName(indexConfig.getName());
		storeOptions.setSimilarity(SimilarityFunction.cosine);
		storeOptions.setDimensions(dimension);

		return ElasticsearchVectorStore.builder(restClient, embeddingModel)
			.options(storeOptions)
			.initializeSchema(false)
			.batchingStrategy(new DefaultBatchingStrategy())
			.build();
	}

	/**
	 * Lists document chunks from the index with pagination support
	 * @param indexConfig Index configuration
	 * @param searchRequest Search parameters including pagination and filters
	 * @return Paginated list of document chunks
	 */
	public PagingList<DocumentChunk> listDocumentChunks(IndexConfig indexConfig, SearchRequest searchRequest) {
		try {
			int from = searchRequest.getFrom();
			int size = searchRequest.getTopK();
			String queryString = Objects.isNull(searchRequest.getFilterExpression()) ? "*"
					: this.filterExpressionConverter.convertExpression(searchRequest.getFilterExpression());
			SearchResponse<Document> res = this.elasticsearchClient.search(sr -> sr.index(indexConfig.getName())
				.query(q -> q.queryString(qs -> qs.query(queryString)))
				.from(searchRequest.getFrom())
				.size(searchRequest.getTopK()), Document.class);

			List<DocumentChunk> chunks;
			List<Hit<Document>> hits = res.hits().hits();
			if (CollectionUtils.isEmpty(hits)) {
				chunks = new ArrayList<>();
			}
			else {
				chunks = hits.stream()
					.filter(x -> x.source() != null)
					.map(x -> DocumentChunkConverter.toDocumentChunk(x.source()))
					.toList();
			}

			long total = res.hits().total() == null ? 0 : res.hits().total().value();
			int current = (from / size) + 1;
			return new PagingList<>(current, size, total, chunks);
		}
		catch (IOException e) {
			throw new BizException(ErrorCode.DOCUMENT_RETRIEVAL_ERROR.toError(), e);
		}
	}

	/**
	 * Updates multiple document chunks in the index
	 * @param indexConfig Index configuration
	 * @param chunks List of document chunks to update
	 */
	@Override
	public void updateDocumentChunks(IndexConfig indexConfig, List<DocumentChunk> chunks) {
		try {
			EmbeddingModel embeddingModel = modelFactory.getEmbeddingModel(MetadataMode.EMBED, indexConfig);
			BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

			List<Document> documents = chunks.stream().map(DocumentChunkConverter::toDocument).toList();
			List<float[]> embeddings = embeddingModel.embed(documents, EmbeddingOptionsBuilder.builder().build(),
					new DefaultBatchingStrategy());

			for (Document document : documents) {
				ElasticsearchVectorStore.ElasticSearchDocument doc = new ElasticsearchVectorStore.ElasticSearchDocument(
						document.getId(), document.getText(), document.getMetadata(),
						embeddings.get(documents.indexOf(document)));
				bulkRequestBuilder.operations(op -> op
					.update(idx -> idx.index(indexConfig.getName()).id(document.getId()).action(a -> a.doc(doc))));
			}

			bulkUpdate(bulkRequestBuilder.build());
		}
		catch (IOException e) {
			throw new BizException(ErrorCode.UPDATE_DOCUMENT_CHUNK_ERROR.toError(), e);
		}
	}

	/**
	 * Updates the enabled status of multiple document chunks
	 * @param indexConfig Index configuration
	 * @param chunkIds List of chunk IDs to update
	 * @param enabled New enabled status
	 */
	@Override
	public void updateDocumentChunkStatus(IndexConfig indexConfig, List<String> chunkIds, boolean enabled) {
		try {
			BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
			for (String chunkId : chunkIds) {
				ElasticsearchVectorStore.ElasticSearchDocument doc = new ElasticsearchVectorStore.ElasticSearchDocument(
						chunkId, null, Map.of(KEY_ENABLED, enabled), null);
				bulkRequestBuilder.operations(
						op -> op.update(idx -> idx.index(indexConfig.getName()).id(chunkId).action(a -> a.doc(doc))));
			}

			bulkUpdate(bulkRequestBuilder.build());
		}
		catch (IOException e) {
			throw new BizException(ErrorCode.UPDATE_DOCUMENT_CHUNK_ERROR.toError(), e);
		}
	}

	/**
	 * Performs a bulk update operation and handles any errors
	 * @param request Bulk update request
	 * @throws IOException if the update operation fails
	 */
	private void bulkUpdate(BulkRequest request) throws IOException {
		BulkResponse bulkRequest = this.elasticsearchClient.bulk(request);
		if (bulkRequest.errors()) {
			List<BulkResponseItem> bulkResponseItems = bulkRequest.items();
			for (BulkResponseItem bulkResponseItem : bulkResponseItems) {
				if (bulkResponseItem.error() != null) {
					throw new IllegalStateException(bulkResponseItem.error().reason());
				}
			}
		}
	}

}
