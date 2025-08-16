/*
 * Copyright 2023-2025 the original author or authors.
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

package org.springframework.ai.vectorstore.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.Version;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.model.EmbeddingUtils;
import org.springframework.ai.observation.conventions.VectorStoreProvider;
import org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SearchType;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.SEARCH_TIMEOUT;
import static com.alibaba.cloud.ai.studio.core.utils.concurrent.ThreadPoolUtils.DEFAULT_TASK_EXECUTOR;

/**
 * Elasticsearch-based vector store implementation using the dense_vector field type.
 *
 * <p>
 * The store uses an Elasticsearch index to persist vector embeddings along with their
 * associated document content and metadata. The implementation leverages Elasticsearch's
 * k-NN search capabilities for efficient similarity search operations.
 * </p>
 *
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>Automatic schema initialization with configurable index creation</li>
 * <li>Support for multiple similarity functions: Cosine, L2 Norm, and Dot Product</li>
 * <li>Metadata filtering using Elasticsearch query strings</li>
 * <li>Configurable similarity thresholds for search results</li>
 * <li>Batch processing support with configurable strategies</li>
 * <li>Observation and metrics support through Micrometer</li>
 * </ul>
 *
 * <p>
 * Basic usage example:
 * </p>
 * <pre>{@code
 * ElasticsearchVectorStore vectorStore = ElasticsearchVectorStore.builder(restClient, embeddingModel)
 *     .initializeSchema(true)
 *     .build();
 *
 * // Add documents
 * vectorStore.add(List.of(
 *     new Document("content1", Map.of("key1", "value1")),
 *     new Document("content2", Map.of("key2", "value2"))
 * ));
 *
 * // Search with filters
 * List<Document> results = vectorStore.similaritySearch(
 *     SearchRequest.query("search text")
 *         .withTopK(5)
 *         .withSimilarityThreshold(0.7)
 *         .withFilterExpression("key1 == 'value1'")
 * );
 * }</pre>
 *
 * <p>
 * Advanced configuration example:
 * </p>
 * <pre>{@code
 * ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
 * options.setIndexName("custom_vectors");
 * options.setSimilarity(SimilarityFunction.dot_product);
 * options.setDimensions(1536);
 *
 * ElasticsearchVectorStore vectorStore = ElasticsearchVectorStore.builder(restClient, embeddingModel)
 *     .options(options)
 *     .initializeSchema(true)
 *     .batchingStrategy(new TokenCountBatchingStrategy())
 *     .build();
 * }</pre>
 *
 * <p>
 * Requirements:
 * </p>
 * <ul>
 * <li>Elasticsearch 8.0 or later</li>
 * <li>Index mapping with id (string), content (text), metadata (object), and embedding
 * (dense_vector) fields</li>
 * </ul>
 *
 * <p>
 * Similarity Functions:
 * </p>
 * <ul>
 * <li>cosine: Default, suitable for most use cases. Measures cosine similarity between
 * vectors.</li>
 * <li>l2_norm: Euclidean distance between vectors. Lower values indicate higher
 * similarity.</li>
 * <li>dot_product: Best performance for normalized vectors (e.g., OpenAI
 * embeddings).</li>
 * </ul>
 *
 * @author Jemin Huh
 * @author Wei Jiang
 * @author Laura Trotta
 * @author Soby Chacko
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @author Ilayaperumal Gopinathan
 * @since 1.0.0
 */
public class ElasticsearchVectorStore extends AbstractObservationVectorStore implements InitializingBean {

	private static final Map<SimilarityFunction, VectorStoreSimilarityMetric> SIMILARITY_TYPE_MAPPING = Map.of(
			SimilarityFunction.cosine, VectorStoreSimilarityMetric.COSINE, SimilarityFunction.l2_norm,
			VectorStoreSimilarityMetric.EUCLIDEAN, SimilarityFunction.dot_product, VectorStoreSimilarityMetric.DOT);

	private final ElasticsearchClient elasticsearchClient;

	private final ElasticsearchVectorStoreOptions options;

	private final FilterExpressionConverter filterExpressionConverter;

	private final boolean initializeSchema;

	protected ElasticsearchVectorStore(Builder builder) {
		super(builder);

		Assert.notNull(builder.restClient, "RestClient must not be null");

		this.initializeSchema = builder.initializeSchema;
		this.options = builder.options;
		this.filterExpressionConverter = builder.filterExpressionConverter;

		String version = Version.VERSION == null ? "Unknown" : Version.VERSION.toString();
		this.elasticsearchClient = new ElasticsearchClient(new RestClientTransport(builder.restClient,
				new JacksonJsonpMapper(
						new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))))
			.withTransportOptions(t -> t.addHeader("user-agent", "spring-ai elastic-java/" + version));
	}

	@Override
	public void doAdd(List<Document> documents) {
		// For the index to be present, either it must be pre-created or set the
		// initializeSchema to true.
		if (!indexExists()) {
			throw new IllegalArgumentException("Index not found");
		}
		BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

		List<float[]> embeddings = this.embeddingModel.embed(documents, EmbeddingOptionsBuilder.builder().build(),
				this.batchingStrategy);

		for (Document document : documents) {
			ElasticSearchDocument doc = new ElasticSearchDocument(document.getId(), document.getText(),
					document.getMetadata(), embeddings.get(documents.indexOf(document)));
			bulkRequestBuilder.operations(
					op -> op.index(idx -> idx.index(this.options.getIndexName()).id(document.getId()).document(doc)));
		}
		BulkResponse bulkRequest = bulkRequest(bulkRequestBuilder.build());
		if (bulkRequest.errors()) {
			List<BulkResponseItem> bulkResponseItems = bulkRequest.items();
			for (BulkResponseItem bulkResponseItem : bulkResponseItems) {
				if (bulkResponseItem.error() != null) {
					throw new IllegalStateException(bulkResponseItem.error().reason());
				}
			}
		}
	}

	@Override
	public void doDelete(List<String> idList) {
		BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
		// For the index to be present, either it must be pre-created or set the
		// initializeSchema to true.
		if (!indexExists()) {
			throw new IllegalArgumentException("Index not found");
		}
		for (String id : idList) {
			bulkRequestBuilder.operations(op -> op.delete(idx -> idx.index(this.options.getIndexName()).id(id)));
		}
		if (bulkRequest(bulkRequestBuilder.build()).errors()) {
			throw new IllegalStateException("Delete operation failed");
		}
	}

	@Override
	public void doDelete(Filter.Expression filterExpression) {
		// For the index to be present, either it must be pre-created or set the
		// initializeSchema to true.
		if (!indexExists()) {
			throw new IllegalArgumentException("Index not found");
		}

		try {
			this.elasticsearchClient.deleteByQuery(d -> d.index(this.options.getIndexName())
				.query(q -> q.queryString(qs -> qs.query(getElasticsearchQueryString(filterExpression)))));
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to delete documents by filter", e);
		}
	}

	private BulkResponse bulkRequest(BulkRequest bulkRequest) {
		try {
			return this.elasticsearchClient.bulk(bulkRequest);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Document> doSimilaritySearch(SearchRequest searchRequest) {
		Assert.notNull(searchRequest, "The search request must not be null.");

		return switch (searchRequest.getSearchType()) {
			case SEMANTIC -> searchBySemantic(searchRequest);
			case FULL_TEXT -> searchByFullText(searchRequest);
			case HYBRID -> searchByHybrid(searchRequest);
			default -> throw new IllegalArgumentException("Unsupported search type: " + searchRequest.getSearchType());
		};
	}

	private String getElasticsearchQueryString(Filter.Expression filterExpression) {
		return Objects.isNull(filterExpression) ? "*"
				: this.filterExpressionConverter.convertExpression(filterExpression);

	}

	private Document toDocument(Hit<Document> hit, SearchType searchType) {
		Document document = hit.source();
		Document.Builder documentBuilder = document.mutate();
		if (hit.score() != null) {
			documentBuilder.metadata(DocumentMetadata.DISTANCE.value(), 1 - normalizeSimilarityScore(hit.score()));

			if (searchType == SearchType.FULL_TEXT) {
				documentBuilder.score(1 - hit.score());
			}
			else {
				documentBuilder.score(normalizeSimilarityScore(hit.score()));
			}
		}
		return documentBuilder.build();
	}

	// more info on score/distance calculation
	// https://www.elastic.co/guide/en/elasticsearch/reference/current/knn-search.html#knn-similarity-search
	private double normalizeSimilarityScore(double score) {
		switch (this.options.getSimilarity()) {
			case l2_norm:
				// the returned value of l2_norm is the opposite of the other functions
				// (closest to zero means more accurate), so to make it consistent
				// with the other functions the reverse is returned applying a "1-"
				// to the standard transformation
				return (1 - (java.lang.Math.sqrt((1 / score) - 1)));
			// cosine and dot_product
			default:
				return (2 * score) - 1;
		}
	}

	public boolean indexExists() {
		try {
			return this.elasticsearchClient.indices().exists(ex -> ex.index(this.options.getIndexName())).value();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void createIndexMapping() {
		try {
			this.elasticsearchClient.indices()
				.create(cr -> cr.index(this.options.getIndexName())
					.mappings(map -> map.properties("embedding",
							p -> p.denseVector(dv -> dv.similarity(this.options.getSimilarity().toString())
								.dims(this.options.getDimensions())))));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void afterPropertiesSet() {
		if (!this.initializeSchema) {
			return;
		}
		if (!indexExists()) {
			createIndexMapping();
		}
	}

	@Override
	public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
		int dimensions = this.options.getDimensions();
		if (dimensions <= 0) {
			dimensions = this.embeddingModel.dimensions();
		}

		return VectorStoreObservationContext.builder(VectorStoreProvider.ELASTICSEARCH.value(), operationName)
			.collectionName(this.options.getIndexName())
			.dimensions(dimensions)
			.similarityMetric(getSimilarityMetric());
	}

	private String getSimilarityMetric() {
		if (!SIMILARITY_TYPE_MAPPING.containsKey(this.options.getSimilarity())) {
			return this.options.getSimilarity().name();
		}
		return SIMILARITY_TYPE_MAPPING.get(this.options.getSimilarity()).value();
	}

	@Override
	public <T> Optional<T> getNativeClient() {
		@SuppressWarnings("unchecked")
		T client = (T) this.elasticsearchClient;
		return Optional.of(client);
	}

	/**
	 * Creates a new builder instance for ElasticsearchVectorStore.
	 * @return a new ElasticsearchBuilder instance
	 */
	public static Builder builder(RestClient restClient, EmbeddingModel embeddingModel) {
		return new Builder(restClient, embeddingModel);
	}

	/**
	 * The representation of {@link Document} along with its embedding.
	 *
	 * @param id The id of the document
	 * @param content The content of the document
	 * @param metadata The metadata of the document
	 * @param embedding The vectors representing the content of the document
	 */
	public record ElasticSearchDocument(String id, String content, Map<String, Object> metadata, float[] embedding) {
	}

	public static class Builder extends AbstractVectorStoreBuilder<Builder> {

		private final RestClient restClient;

		private ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();

		private boolean initializeSchema = false;

		private FilterExpressionConverter filterExpressionConverter = new ElasticsearchAiSearchFilterExpressionConverter();

		/**
		 * Sets the Elasticsearch REST client.
		 * @param restClient the Elasticsearch REST client
		 * @param embeddingModel the Embedding Model to be used
		 */
		public Builder(RestClient restClient, EmbeddingModel embeddingModel) {
			super(embeddingModel);
			Assert.notNull(restClient, "RestClient must not be null");
			this.restClient = restClient;
		}

		/**
		 * Sets the Elasticsearch vector store options.
		 * @param options the vector store options to use
		 * @return the builder instance
		 * @throws IllegalArgumentException if options is null
		 */
		public Builder options(ElasticsearchVectorStoreOptions options) {
			Assert.notNull(options, "options must not be null");
			this.options = options;
			return this;
		}

		/**
		 * Sets whether to initialize the schema.
		 * @param initializeSchema true to initialize schema, false otherwise
		 * @return the builder instance
		 */
		public Builder initializeSchema(boolean initializeSchema) {
			this.initializeSchema = initializeSchema;
			return this;
		}

		/**
		 * Sets the filter expression converter.
		 * @param converter the filter expression converter to use
		 * @return the builder instance
		 * @throws IllegalArgumentException if converter is null
		 */
		public Builder filterExpressionConverter(FilterExpressionConverter converter) {
			Assert.notNull(converter, "filterExpressionConverter must not be null");
			this.filterExpressionConverter = converter;
			return this;
		}

		/**
		 * Builds the ElasticsearchVectorStore instance.
		 * @return a new ElasticsearchVectorStore instance
		 * @throws IllegalStateException if the builder is in an invalid state
		 */
		@Override
		public ElasticsearchVectorStore build() {
			return new ElasticsearchVectorStore(this);
		}

	}

	protected List<Document> searchBySemantic(SearchRequest searchRequest) {
		try {
			float threshold = (float) searchRequest.getSimilarityThreshold();
			// reverting l2_norm distance to its original value
			if (this.options.getSimilarity().equals(SimilarityFunction.l2_norm)) {
				threshold = 1 - threshold;
			}
			final float finalThreshold = threshold;
			float[] vectors = this.embeddingModel.embed(searchRequest.getQuery());

			SearchResponse<Document> res = this.elasticsearchClient.search(sr -> sr.index(this.options.getIndexName())
				.knn(knn -> knn.queryVector(EmbeddingUtils.toList(vectors))
					.similarity(finalThreshold)
					.k((long) searchRequest.getTopK())
					.field("embedding")
					.numCandidates((long) (1.5 * searchRequest.getTopK()))
					.filter(fl -> fl
						.queryString(qs -> qs.query(getElasticsearchQueryString(searchRequest.getFilterExpression())))))
				.size(searchRequest.getTopK()), Document.class);

			return res.hits().hits().stream().map(x -> toDocument(x, SearchType.SEMANTIC)).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new BizException(ErrorCode.DOCUMENT_RETRIEVAL_ERROR.toError(), e);
		}
	}

	protected List<Document> searchByFullText(SearchRequest searchRequest) {
		try {
			SearchResponse<Document> res = this.elasticsearchClient.search(
					sr -> sr.index(this.options.getIndexName())
						.query(q -> q.bool(m -> m
							.must(qm -> qm.match(mm -> mm.field("content").query(searchRequest.getQuery())))
							.filter(fl -> fl.queryString(
									qs -> qs.query(getElasticsearchQueryString(searchRequest.getFilterExpression()))))))
						.minScore(searchRequest.getSimilarityThreshold())
						.size((int) (1.5 * searchRequest.getTopK())),
					Document.class);

			return res.hits()
				.hits()
				.stream()
				.map(x -> toDocument(x, SearchType.FULL_TEXT))
				.collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new BizException(ErrorCode.DOCUMENT_RETRIEVAL_ERROR.toError(), e);
		}
	}

	protected List<Document> searchByHybrid(SearchRequest searchRequest) {
		List<CompletableFuture<List<Document>>> futureList = new ArrayList<>();

		if (searchRequest.getHybridWeight() < 0 || searchRequest.getHybridWeight() > 1) {
			throw new IllegalArgumentException("hybrid alpha should be between 0 ~ 1.");
		}

		try {
			CompletableFuture<List<Document>> textFuture = CompletableFuture.supplyAsync(() -> {
				int textTopK = Math.round(searchRequest.getTopK() * (1 - searchRequest.getHybridWeight()));
				return searchByFullText(SearchRequest.builder()
					.query(searchRequest.getQuery())
					.similarityThreshold(searchRequest.getSimilarityThreshold())
					.topK(textTopK)
					.filterExpression(searchRequest.getFilterExpression())
					.build());
			}, DEFAULT_TASK_EXECUTOR);
			futureList.add(textFuture);

			// 基于向量检索召回内容
			CompletableFuture<List<Document>> vectorFuture = CompletableFuture.supplyAsync(() -> {
				int textTopK = Math.round(searchRequest.getTopK() * searchRequest.getHybridWeight());
				return searchBySemantic(SearchRequest.builder()
					.query(searchRequest.getQuery())
					.similarityThreshold(searchRequest.getSimilarityThreshold())
					.topK(textTopK)
					.filterExpression(searchRequest.getFilterExpression())
					.build());
			}, DEFAULT_TASK_EXECUTOR);
			futureList.add(vectorFuture);

			for (CompletableFuture<List<Document>> future : futureList) {
				future.get(SEARCH_TIMEOUT, TimeUnit.SECONDS);
			}

			// 去重处理
			List<Document> vectorList = vectorFuture.get() == null ? new ArrayList<>() : vectorFuture.get();
			List<Document> fullTextList = textFuture.get() == null ? new ArrayList<>() : textFuture.get();

			return Stream.concat(vectorList.stream(), fullTextList.stream())
				.collect(Collectors.toMap(Document::getId, student -> student, (existing, newStudent) -> existing))
				.values()
				.stream()
				.toList();
		}
		catch (InterruptedException | ExecutionException e) {
			throw new BizException(ErrorCode.DOCUMENT_RETRIEVAL_ERROR.toError(), e);
		}
		catch (TimeoutException e) {
			throw new BizException(ErrorCode.DOCUMENT_RETRIEVAL_TIMEOUT.toError(), e);
		}
	}

}
