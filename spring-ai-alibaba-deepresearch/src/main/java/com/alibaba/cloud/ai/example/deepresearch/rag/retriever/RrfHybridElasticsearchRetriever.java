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
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Hybrid Elasticsearch retriever using BM25 and KNN search with Reciprocal Rank Fusion.
 *
 * @author hupei
 */
public class RrfHybridElasticsearchRetriever implements DocumentRetriever {

	private final RestClient restClient;

	private final EmbeddingModel embeddingModel;

	private final String indexName;

	private final int windowSize;

	private final int rrfK;

	private final float bm25Boost;

	private final float knnBoost;

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

	@Override
	public List<Document> retrieve(Query query) {
		String text = query.text();
		try {
			List<ScoredDocument> bm25Docs = searchBm25(text);
			List<ScoredDocument> knnDocs = searchKnn(text);
			List<ScoredDocument> merged = rrfMerge(bm25Docs, knnDocs);
			return merged.stream().limit(windowSize).map(ScoredDocument::doc).collect(Collectors.toList());
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to execute hybrid search", ex);
		}
	}

	private List<ScoredDocument> searchBm25(String text) throws IOException {
		String body = String.format(Locale.ROOT, "{\"size\":%d,\"query\":{\"match\":{\"content\":\"%s\"}}}", windowSize,
				escape(text));
		Request request = new Request("GET", "/" + indexName + "/_search");
		request.setJsonEntity(body);
		Response response = restClient.performRequest(request);
		return parseResults(response.getEntity().getContent());
	}

	private List<ScoredDocument> searchKnn(String text) throws IOException {
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
		String body = String.format(Locale.ROOT,
				"{\"size\":%d,\"knn\":{\"field\":\"embedding\",\"query_vector\":%s,\"k\":%d,\"num_candidates\":%d}}",
				windowSize, vectorStr, windowSize, Math.max(windowSize * 2, 10));
		Request request = new Request("GET", "/" + indexName + "/_search");
		request.setJsonEntity(body);
		Response response = restClient.performRequest(request);
		return parseResults(response.getEntity().getContent());
	}

	private List<ScoredDocument> parseResults(InputStream content) throws IOException {
		JsonNode hits = mapper.readTree(content).path("hits").path("hits");
		List<ScoredDocument> results = new ArrayList<>();
		for (JsonNode hit : hits) {
			String id = hit.path("_id").asText();
			String docText = hit.path("_source").path("content").asText();
			Map<String, Object> metadata = mapper.convertValue(hit.path("_source"), Map.class);
			Document doc = new Document(id, docText, metadata);
			results.add(new ScoredDocument(doc));
		}
		return results;
	}

	private List<ScoredDocument> rrfMerge(List<ScoredDocument> bm25Docs, List<ScoredDocument> knnDocs) {
		Map<String, ScoredDocument> map = new HashMap<>();
		AtomicInteger rank = new AtomicInteger(1);
		for (ScoredDocument d : bm25Docs) {
			map.compute(d.doc().getId(), (k, v) -> accumulate(v, d, rank.getAndIncrement(), bm25Boost));
		}
		rank.set(1);
		for (ScoredDocument d : knnDocs) {
			map.compute(d.doc().getId(), (k, v) -> accumulate(v, d, rank.getAndIncrement(), knnBoost));
		}
		return map.values()
			.stream()
			.sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
			.collect(Collectors.toList());
	}

	private ScoredDocument accumulate(ScoredDocument existing, ScoredDocument incoming, int rank, float boost) {
		double score = boost / (rrfK + rank);
		if (existing == null) {
			incoming.addScore(score);
			return incoming;
		}
		existing.addScore(score);
		return existing;
	}

	private static String escape(String text) {
		return text.replace("\"", "\\\"");
	}

	private static class ScoredDocument {

		private final Document doc;

		private double score;

		ScoredDocument(Document doc) {
			this.doc = doc;
		}

		Document doc() {
			return doc;
		}

		double score() {
			return score;
		}

		void addScore(double s) {
			this.score += s;
		}

	}

}
