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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hybrid Elasticsearch retriever using BM25 and KNN search with Reciprocal Rank Fusion.
 *
 * @author hupei
 */
public class RrfHybridElasticsearchRetriever implements DocumentRetriever {

	/**
	 * Elasticsearch REST client for executing search requests
	 */
	private final RestClient restClient;

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

	/**
	 * JSON object mapper for parsing Elasticsearch responses
	 */
	private final ObjectMapper mapper = new ObjectMapper();

	public RrfHybridElasticsearchRetriever(RestClient restClient, EmbeddingModel embeddingModel, String indexName,
			RagProperties.Elasticsearch.Hybrid hybrid) {
		this.restClient = restClient;
		this.embeddingModel = embeddingModel;
		this.indexName = indexName;
		this.windowSize = hybrid.getRrfWindowSize();
		this.rrfK = hybrid.getRrfRankConstant();
		this.bm25Boost = hybrid.getBm25Boost();
		this.knnBoost = hybrid.getKnnBoost();
	}

	@NotNull
	@Override
	public List<Document> retrieve(Query query) {
		String text = query.text();
		try {
			return searchHybrid(text);
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to execute hybrid search", ex);
		}
	}

	private List<Document> searchHybrid(String text) throws IOException {
		float[] vector = embeddingModel.embed(text);
		StringBuilder builder = new StringBuilder("[");
		for (int i = 0; i < vector.length; i++) {
			if (i > 0) {
				builder.append(',');
			}
			builder.append(vector[i]);
		}
		builder.append(']');
		String vectorStr = builder.toString();

		String body = String.format(Locale.ROOT, """
				{
				  "queries": [
				    {
				      "query": {
				        "match": {
				          "content": "%s"
				        }
				      },
				      "boost": %f
				    },
				    {
				      "knn": {
				        "field": "embedding",
				        "query_vector": %s,
				        "k": %d,
				        "num_candidates": %d
				      },
				      "boost": %f
				    }
				  ],
				  "rank": {
				    "rrf": {
				      "window_size": %d,
				      "rank_constant": %d
				    }
				  }
				}""", escape(text), bm25Boost, vectorStr, windowSize, Math.max(windowSize * 2, 10), knnBoost,
				windowSize, rrfK);

		Request request = new Request("GET", "/" + indexName + "/_search");
		request.setJsonEntity(body);
		Response response = restClient.performRequest(request);
		return parseResults(response.getEntity().getContent());
	}

	private List<Document> parseResults(InputStream content) throws IOException {
		JsonNode hits = mapper.readTree(content).path("hits").path("hits");

		List<Document> results = new ArrayList<>();

		for (JsonNode hit : hits) {
			String id = hit.path("_id").asText();
			String docText = hit.path("_source").path("content").asText();

			Map<String, Object> metadata = mapper.convertValue(hit.path("_source"),
					mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

			results.add(new Document(id, docText, metadata));
		}

		return results;
	}

	private static String escape(String text) {
		return text.replace("\"", "\\\"");
	}

}
