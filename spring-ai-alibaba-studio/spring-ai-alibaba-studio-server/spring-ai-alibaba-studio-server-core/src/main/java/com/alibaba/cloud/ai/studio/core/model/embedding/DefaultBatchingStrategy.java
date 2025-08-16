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

package com.alibaba.cloud.ai.studio.core.model.embedding;

import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of document batching strategy for embedding processing. Splits
 * documents into batches of specified size for efficient processing.
 *
 * @since 1.0.0.3
 */
public class DefaultBatchingStrategy implements BatchingStrategy {

	/** Maximum batch size for document processing */
	private static final int MAX_BATCH_SIZE = 20;

	/** Configured batch size for document batching */
	private final Integer batchSize;

	/**
	 * Creates a batching strategy with default maximum batch size.
	 */
	public DefaultBatchingStrategy() {
		this.batchSize = MAX_BATCH_SIZE;
	}

	/**
	 * Creates a batching strategy with specified batch size.
	 * @param batchSize the size of each batch
	 */
	public DefaultBatchingStrategy(Integer batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Splits the input documents into batches.
	 * @param documents list of documents to be batched
	 * @return list of document batches
	 */
	@NotNull
	@Override
	public List<List<Document>> batch(@NotNull List<Document> documents) {
		if (CollectionUtils.isEmpty(documents)) {
			return List.of();
		}

		if (documents.size() <= batchSize) {
			return List.of(documents);
		}

		List<List<Document>> batches = new ArrayList<>();
		for (int i = 0; i < documents.size(); i += batchSize) {
			int end = Math.min(i + batchSize, documents.size());
			List<Document> batch = documents.subList(i, end);
			batches.add(batch);
		}

		return batches;
	}

}
