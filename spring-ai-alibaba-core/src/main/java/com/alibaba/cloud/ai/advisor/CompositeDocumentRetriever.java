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
package com.alibaba.cloud.ai.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.Assert;

/**
 * Composite document retriever that combines multiple document retrievers.
 *
 * @author mengnankkkk
 * @since 1.0.0-M2
 */
public class CompositeDocumentRetriever implements DocumentRetriever {

	private static final Logger logger = LoggerFactory.getLogger(CompositeDocumentRetriever.class);

	private final List<DocumentRetriever> retrievers;

	private final Integer maxResultsPerRetriever;

	private final ResultMergeStrategy mergeStrategy;

	public enum ResultMergeStrategy {

		SIMPLE_MERGE, // Simple merge strategy

		SCORE_BASED, // Score-based merge strategy

		ROUND_ROBIN// Round-robin merge strategy

	}

	public CompositeDocumentRetriever(List<DocumentRetriever> retrievers) {
		this(retrievers, 10, ResultMergeStrategy.SCORE_BASED);
	}

	public CompositeDocumentRetriever(List<DocumentRetriever> retrievers, Integer maxResultsPerRetriever) {
		this(retrievers, maxResultsPerRetriever, ResultMergeStrategy.SCORE_BASED);
	}

	public CompositeDocumentRetriever(List<DocumentRetriever> retrievers, Integer maxResultsPerRetriever,
			ResultMergeStrategy mergeStrategy) {
		Assert.notNull(retrievers, "Retrievers list must not be null!");
		Assert.isTrue(!retrievers.isEmpty(), "Retrievers list must not be empty!");
		Assert.isTrue(maxResultsPerRetriever > 0, "MaxResultsPerRetriever must be positive!");
		Assert.notNull(mergeStrategy, "MergeStrategy must not be null!");

		this.retrievers = new ArrayList<>(retrievers);
		this.maxResultsPerRetriever = maxResultsPerRetriever;
		this.mergeStrategy = mergeStrategy;
	}

	@Override
	public List<Document> retrieve(Query query) {
		if (mergeStrategy == ResultMergeStrategy.ROUND_ROBIN) {
			return roundRobinRetrieve(query);
		}

		List<Document> allDocuments = new ArrayList<>();

		for (DocumentRetriever retriever : retrievers) {
			try {
				List<Document> documents = retriever.retrieve(query);
				if (documents != null && !documents.isEmpty()) {
					List<Document> limitedDocuments = documents.stream()
						.limit(maxResultsPerRetriever)
						.collect(Collectors.toList());
					allDocuments.addAll(limitedDocuments);
				}
			}
			catch (Exception e) {
				logger.error("Error retrieving from one of the retrievers: {}", e.getMessage(), e);
			}
		}

		return mergeResults(allDocuments);
	}

	private List<Document> roundRobinRetrieve(Query query) {
		List<List<Document>> allResults = new ArrayList<>();

		for (DocumentRetriever retriever : retrievers) {
			try {
				List<Document> documents = retriever.retrieve(query);
				if (documents != null && !documents.isEmpty()) {

					List<Document> limitedDocuments = documents.stream()
						.limit(maxResultsPerRetriever)
						.collect(Collectors.toList());
					allResults.add(limitedDocuments);
				}
				else {
					allResults.add(new ArrayList<>());
				}
			}
			catch (Exception e) {
				logger.error("Error retrieving from one of the retrievers: {}", e.getMessage(), e);
				allResults.add(new ArrayList<>());
			}
		}

		Integer maxSize = allResults.stream().mapToInt(List::size).max().orElse(0);

		return java.util.stream.IntStream.range(0, maxSize)
			.boxed()
			.flatMap(i -> allResults.stream()
				.filter(documents -> i < documents.size())
				.map(documents -> documents.get(i)))
			.collect(Collectors.toList());
	}

	private List<Document> mergeResults(List<Document> documents) {
		if (documents.isEmpty()) {
			return documents;
		}

		return switch (mergeStrategy) {
			case SIMPLE_MERGE -> documents;
			case SCORE_BASED -> documents.stream().sorted((d1, d2) -> {
				Double score1 = d1.getScore();
				Double score2 = d2.getScore();

				if (score1 == null)
					score1 = 0.0;
				if (score2 == null)
					score2 = 0.0;
				return Double.compare(score2, score1);
			}).collect(Collectors.toList());
			case ROUND_ROBIN -> documents;
			default -> documents;
		};
	}

	public static class Builder {

		private List<DocumentRetriever> retrievers = new ArrayList<>();

		private Integer maxResultsPerRetriever = 10;

		private ResultMergeStrategy mergeStrategy = ResultMergeStrategy.SCORE_BASED;

		private Builder() {
		}

		public Builder addRetriever(DocumentRetriever retriever) {
			if (retriever != null) {
				this.retrievers.add(retriever);
			}
			return this;
		}

		public Builder retrievers(List<DocumentRetriever> retrievers) {
			if (retrievers != null) {
				this.retrievers.addAll(retrievers);
			}
			return this;
		}

		public Builder maxResultsPerRetriever(Integer maxResultsPerRetriever) {
			this.maxResultsPerRetriever = maxResultsPerRetriever;
			return this;
		}

		public Builder mergeStrategy(ResultMergeStrategy mergeStrategy) {
			this.mergeStrategy = mergeStrategy;
			return this;
		}

		public CompositeDocumentRetriever build() {
			return new CompositeDocumentRetriever(retrievers, maxResultsPerRetriever, mergeStrategy);
		}

	}

	public static Builder builder() {
		return new Builder();
	}

}
