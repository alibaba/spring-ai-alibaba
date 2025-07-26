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
package com.alibaba.cloud.ai.example.deepresearch.rag.retriever;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.EmbeddingUtils;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hybrid Elasticsearch retriever using BM25 and KNN search with Reciprocal Rank Fusion.
 * SimilaritySearch reference
 * {@link org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore}
 *
 * @author hupei
 * @author ViliamSun
 */
public class RrfHybridElasticsearchRetriever implements DocumentRetriever {

	/**
	 * Elasticsearch REST client for executing search requests
	 */
	private final ElasticsearchClient elasticsearchClient;

	/**
	 * Model used for generating embeddings from text queries
	 */
	private final EmbeddingModel embeddingModel;

	/**
	 * Name of the Elasticsearch index to search
	 */
	private final String indexName;

	/**
	 * Maximum number of documents to return in search results
	 */
	private final int windowSize;

	/**
	 * Constant k used in Reciprocal Rank Fusion scoring
	 */
	private final int rrfK;

	/**
	 * Boost factor applied to BM25 text search scores
	 */
	private final float bm25Boost;

	/**
	 * Boost factor applied to KNN vector search scores
	 */
	private final float knnBoost;

	private final boolean hasHybrid;

	public RrfHybridElasticsearchRetriever(RestClient restClient, EmbeddingModel embeddingModel, String indexName,
			RagProperties.Elasticsearch.Hybrid hybrid) {
		this.elasticsearchClient = new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper(
				new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))));
		this.embeddingModel = embeddingModel;
		this.indexName = indexName;
		this.windowSize = hybrid.getRrfWindowSize();
		this.rrfK = hybrid.getRrfRankConstant();
		this.bm25Boost = hybrid.getBm25Boost();
		this.knnBoost = hybrid.getKnnBoost();
		this.hasHybrid = hybrid.isEnabled();
	}

	@NotNull
	@Override
	public List<Document> retrieve(Query query) {
		String text = query.text();
		try {
			return search(text, hasHybrid, null);
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to execute hybrid search", ex);
		}
	}

	/**
	 * 允许传入一个 ES filter query
	 * @param query 用户查询
	 * @param filter ES filter query，用于限定搜索范围
	 * @return 文档列表
	 */
	public List<Document> retrieve(Query query, co.elastic.clients.elasticsearch._types.query_dsl.Query filter) {
		String text = query.text();
		try {
			return search(text, hasHybrid, filter);
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to execute hybrid search", ex);
		}
	}

	/**
	 * Query unified entry, default search based on knn method
	 * @param text the query text to search for
	 * @param hasHybrid whether to enable hybrid search (based on query statement and rank
	 * query)
	 */
	public List<Document> search(String text, boolean hasHybrid,
			co.elastic.clients.elasticsearch._types.query_dsl.Query filter) throws IOException {
		float[] vector = embeddingModel.embed(text);
		SearchResponse<Document> response = elasticsearchClient.search(sr -> {
			Builder knnBuilder = sr.index(indexName)
				.postFilter(filter)
				.knn(knn -> knn.queryVector(EmbeddingUtils.toList(vector))
					.similarity(0.0f)
					.k(windowSize)
					.field("embedding")
					.numCandidates(Math.max(windowSize * 2, 10))
					.boost(knnBoost));
			if (hasHybrid) {
				return buildHybridSearch(text, knnBuilder);
			}
			return knnBuilder;
		}, Document.class);

		return response.hits().hits().stream().map(this::toDocument).collect(Collectors.toList());
	}

	private Document toDocument(Hit<Document> hit) {
		Document document = hit.source();
		Document.Builder documentBuilder = document != null ? document.mutate() : new Document.Builder();
		Double score = hit.score();
		if (score != null) {
			documentBuilder.metadata(DocumentMetadata.DISTANCE.value(), 1 - (2 * score) - 1);
			documentBuilder.score((2 * score) - 1);
		}
		return documentBuilder.build();
	}

	private Builder buildHybridSearch(String text, Builder knnBuilder) {
		// Build the hybrid search request with BM25 and rrf
		return knnBuilder.query(q -> q.match(mq -> mq.field("content").query(escape(text)).boost(bm25Boost)))
			.rank(r -> r.rrf(rrfk -> rrfk.rankConstant((long) rrfK).rankWindowSize((long) windowSize)));
	}

	private static String escape(String text) {
		return text.replace("\"", "\\\"");
	}

}
