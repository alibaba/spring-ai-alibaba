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
package com.alibaba.cloud.ai.example.deepresearch.rag.strategy;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of RRF (Reciprocal Rank Fusion) fusion strategy in RAG.
 * This strategy calculates the RRF score of documents based on their rankings
 * across multiple result lists and returns a fused document list.
 * Also implements the DocumentPostProcessor interface to support reranking
 * functionality in post-processing.
 *
 * @author hupei
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.alibaba.deepresearch.rag", name = "enabled", havingValue = "true")
public class RrfFusionStrategy implements FusionStrategy, DocumentPostProcessor {

	private final int k;

	private final int defaultTopK;

	private final double defaultThreshold;

	public RrfFusionStrategy(@Value("${rag.fusion.rrf.k-constant:60}") int k,
			@Value("${rag.pipeline.rerankTopK:10}") int defaultTopK,
			@Value("${rag.pipeline.rerankThreshold:0.1}") double defaultThreshold) {
		this.k = k;
		this.defaultTopK = defaultTopK;
		this.defaultThreshold = defaultThreshold;
	}

	@Override
	public String getStrategyName() {
		return "rrf";
	}

	@Override
	public List<Document> fuse(List<List<Document>> results) {
		if (results == null || results.isEmpty()) {
			return List.of();
		}
		if (results.size() == 1) {
			return results.get(0); // If there is only one result list, no fusion is needed
		}

		return fuseInternal(results, defaultTopK, defaultThreshold);
	}

	@Override
	public List<Document> process(Query query, List<Document> documents) {
		// Treats a single document list as results from multiple sources for reranking
		// Groups by source_type, then uses RRF for fusion
		Map<String, List<Document>> documentsBySource = groupDocumentsBySource(documents);

		// Convert to List<List<Document>> format
		List<List<Document>> results = new ArrayList<>(documentsBySource.values());

		// Use RRF fusion and apply topK and threshold constraints
		return fuseInternal(results, defaultTopK, defaultThreshold);
	}

	/**
	 * Internal fusion method with support for topK and threshold constraints
	 */
	private List<Document> fuseInternal(List<List<Document>> results, int topK, double threshold) {
		if (results == null || results.isEmpty()) {
			return List.of();
		}
		if (results.size() == 1) {
			List<Document> singleResult = results.get(0);
			return singleResult.stream().limit(topK).collect(Collectors.toList());
		}

		// Use a Map to store RRF scores for each document, keyed by document ID
		Map<String, Double> rrfScores = new HashMap<>();
		// Use a Map to store document ID to Document object mapping to avoid duplicate storage
		Map<String, Document> documentMap = new HashMap<>();

		for (List<Document> resultList : results) {
			for (int i = 0; i < resultList.size(); i++) {
				Document doc = resultList.get(i);
				int rank = i + 1; // Ranking starts from 1
				String docId = getDocumentId(doc);

				// Update the document's RRF score
				rrfScores.merge(docId, 1.0 / (k + rank), Double::sum);
				// If encountering this document for the first time, store it in the map
				documentMap.putIfAbsent(docId, doc);
			}
		}

		// Sort document IDs in descending order based on RRF scores and apply filtering conditions
		return rrfScores.entrySet()
			.stream()
			.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
			.filter(entry -> entry.getValue() >= threshold)
			.limit(topK)
			.map(entry -> documentMap.get(entry.getKey()))
			.collect(Collectors.toList());
	}

	/**
	 * Groups documents by source
	 */
	private Map<String, List<Document>> groupDocumentsBySource(List<Document> documents) {
		Map<String, List<Document>> groups = new LinkedHashMap<>();

		for (Document doc : documents) {
			String source = getDocumentSource(doc);
			groups.computeIfAbsent(source, k -> new ArrayList<>()).add(doc);
		}

		return groups;
	}

	/**
	 * Retrieves the document ID, prioritizing the document's own ID;
	 * otherwise uses a content hash.
	 */
	private String getDocumentId(Document document) {
		if (document.getId() != null && !document.getId().isEmpty()) {
			return document.getId();
		}

		// Use content hash as ID
		return String.valueOf(document.getText().hashCode());
	}

	/**
	 * Retrieves the document source by reading source_type from metadata
	 */
	private String getDocumentSource(Document document) {
		Object sourceType = document.getMetadata().get("source_type");
		if (sourceType != null) {
			return sourceType.toString();
		}

		// Default source
		return "default";
	}

}
