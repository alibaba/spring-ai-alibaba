/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.rag.bailian;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.Assert;

import com.aliyun.bailian20231229.models.RetrieveResponse;

/**
 * Document retriever implementation using Bailian knowledge base.
 *
 * <p>This class implements the Spring AI DocumentRetriever interface,
 * using BailianClient to retrieve documents from Alibaba Cloud Bailian
 * knowledge base.
 */
public class BailianDocumentRetriever implements DocumentRetriever {

	private final BailianDocumentRetrieverOptions options;

	private final BailianClient bailianClient;

	private final String indexId;

	/**
	 * Creates a BailianDocumentRetriever using BailianClient.
	 *
	 * @param bailianClient the BailianClient instance
	 * @param indexId the knowledge base index ID
	 * @param options the retrieval options
	 */
	public BailianDocumentRetriever(BailianClient bailianClient, String indexId,
			BailianDocumentRetrieverOptions options) {
		Assert.notNull(options, "RetrieverOptions must not be null");
		Assert.notNull(bailianClient, "BailianClient must not be null");
		Assert.notNull(indexId, "IndexId must not be null");
		this.options = options;
		this.bailianClient = bailianClient;
		this.indexId = indexId;
	}

	@Override
	public List<Document> retrieve(Query query) {
		// Determine limit
		Integer limit = options.getLimit();
		if (limit == null) {
			limit = 5; // Default limit
		}

		// Convert query history from Spring AI Query to QueryHistoryEntry format
		List<QueryHistoryEntry> conversationHistory = null;
		if (!query.history().isEmpty()) {
			conversationHistory = new ArrayList<>();
			for (Message message : query.history()) {
				// Only convert USER and ASSISTANT messages, skip others
				MessageType messageType = message.getMessageType();
				if (messageType == MessageType.USER
						|| messageType == MessageType.ASSISTANT) {
					String role = messageType == MessageType.USER ? "user"
							: "assistant";
					QueryHistoryEntry entry = new QueryHistoryEntry(role, message.getText());
					conversationHistory.add(entry);
				}
			}
		}

		// Retrieve documents using BailianClient
		RetrieveResponse response = bailianClient
			.retrieve(indexId, query.text(), limit, conversationHistory)
			.block();

		// Convert Bailian response to Spring AI Documents
		List<Document> documents = BailianSpringAiDocumentConverter.fromBailianResponse(response);

		// Apply score threshold filtering if specified
		Double scoreThreshold = options.getScoreThreshold();
		if (scoreThreshold != null && scoreThreshold > 0) {
			documents = documents.stream()
				.filter(doc -> {
					Object scoreObj = doc.getMetadata().get("score");
					if (scoreObj instanceof Number) {
						return ((Number) scoreObj).doubleValue() >= scoreThreshold;
					}
					return true; // If no score, include the document
				})
				.collect(Collectors.toList());
		}

		// Sort by score (descending) and limit
		documents = documents.stream()
			.sorted(Comparator.comparing((Document doc) -> {
				Object scoreObj = doc.getMetadata().get("score");
				if (scoreObj instanceof Number) {
					return ((Number) scoreObj).doubleValue();
				}
				return 0.0;
			}, Comparator.reverseOrder()))
			.limit(limit)
			.collect(Collectors.toList());

		return documents;
	}
}
