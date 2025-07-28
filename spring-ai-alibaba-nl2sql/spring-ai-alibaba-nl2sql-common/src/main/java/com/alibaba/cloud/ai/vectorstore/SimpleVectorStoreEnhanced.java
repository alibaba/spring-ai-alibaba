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
package com.alibaba.cloud.ai.vectorstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhanced SimpleVectorStore with additional delete functionality. Extends Spring AI's
 * SimpleVectorStore to provide doDelete method with filter support.
 *
 * @author Maki Ma
 */
public class SimpleVectorStoreEnhanced implements VectorStore {

	private static final Logger logger = LoggerFactory.getLogger(SimpleVectorStoreEnhanced.class);

	private final SimpleVectorStore simpleVectorStore;

	private final EmbeddingModel embeddingModel;

	/**
	 * Constructor with embedding model
	 * @param embeddingModel the embedding model to use
	 */
	public SimpleVectorStoreEnhanced(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
		this.simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
		logger.info("SimpleVectorStoreEnhanced initialized with EmbeddingModel: {}",
				embeddingModel.getClass().getSimpleName());
	}

	/**
	 * Builder method for creating SimpleVectorStoreEnhanced instance
	 * @param embeddingModel the embedding model to use
	 * @return new SimpleVectorStoreEnhanced instance
	 */
	public static SimpleVectorStoreEnhanced builder(EmbeddingModel embeddingModel) {
		return new SimpleVectorStoreEnhanced(embeddingModel);
	}

	@Override
	public void add(List<Document> documents) {
		logger.debug("Adding {} documents to vector store", documents.size());
		simpleVectorStore.add(documents);
		logger.info("Successfully added {} documents", documents.size());
	}

	@Override
	public void delete(List<String> idList) {
		logger.debug("Deleting documents by IDs: {}", idList);
		simpleVectorStore.delete(idList);
		logger.info("Delete by IDs completed successfully");
	}

	@Override
	public void delete(Filter.Expression filterExpression) {
		doDelete(filterExpression);
	}

	@Override
	public List<Document> similaritySearch(SearchRequest request) {
		logger.debug("Performing similarity search with query: {}, topK: {}", request.getQuery(), request.getTopK());
		List<Document> results = simpleVectorStore.similaritySearch(request);
		logger.debug("Similarity search completed. Found {} documents", results.size());
		return results;
	}

	/**
	 * Delete documents based on filter expression. This method implements the user's
	 * specified approach: 1. Create a special search request to get all documents
	 * matching the filter 2. Extract document IDs from search results 3. Delete documents
	 * by IDs
	 * @param filterExpression the filter expression to match documents for deletion
	 */
	public void doDelete(Filter.Expression filterExpression) {
		logger.info("Starting doDelete operation with filter expression: {}", filterExpression);

		try {
			// 1. Create a special search request to get all documents matching the
			// filter.
			SearchRequest request = SearchRequest.builder()
				// Use a wildcard or any non-empty string to initiate the search.
				.query("*")
				// Apply the filter for the documents we want to delete.
				.filterExpression(filterExpression)
				// Set topK to the maximum value to ensure all matching documents are
				// returned.
				.topK(Integer.MAX_VALUE)
				// Accept results with any similarity score (0.0 means accept all).
				.similarityThreshold(0.0)
				.build();

			logger.debug("Created search request for deletion: query='*', topK={}, threshold={}", Integer.MAX_VALUE,
					0.0);

			// 2. Execute search to find all matching documents
			List<Document> documentsToDelete = similaritySearch(request);
			logger.info("Found {} documents matching filter for deletion", documentsToDelete.size());

			if (documentsToDelete.isEmpty()) {
				logger.info("No documents found matching the filter expression");
				return;
			}

			// 3. Extract document IDs
			List<String> idsToDelete = documentsToDelete.stream().map(Document::getId).collect(Collectors.toList());

			logger.debug("Document IDs to delete: {}", idsToDelete);

			// 4. Delete documents by IDs
			delete(idsToDelete);
			logger.info("Successfully deleted {} documents using doDelete", idsToDelete.size());

		}
		catch (Exception e) {
			logger.error("Failed to execute doDelete operation: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to delete documents by filter expression: " + e.getMessage(), e);
		}
	}

	/**
	 * Get the total number of documents in the vector store
	 * @return the total document count
	 */
	public int getDocumentCount() {
		try {
			// Use a broad search to get all documents
			SearchRequest request = SearchRequest.builder()
				.query("*")
				.topK(Integer.MAX_VALUE)
				.similarityThreshold(0.0)
				.build();
			List<Document> allDocuments = similaritySearch(request);
			int count = allDocuments.size();
			logger.debug("Total document count: {}", count);
			return count;
		}
		catch (Exception e) {
			logger.error("Failed to get document count: {}", e.getMessage(), e);
			return 0;
		}
	}

	/**
	 * Delete all documents in the vector store
	 */
	public void deleteAll() {
		logger.info("Starting deleteAll operation");
		try {
			// Get all documents
			SearchRequest request = SearchRequest.builder()
				.query("*")
				.topK(Integer.MAX_VALUE)
				.similarityThreshold(0.0)
				.build();
			List<Document> allDocuments = similaritySearch(request);

			if (allDocuments.isEmpty()) {
				logger.info("No documents to delete");
				return;
			}

			// Extract all IDs
			List<String> allIds = allDocuments.stream().map(Document::getId).collect(Collectors.toList());

			// Delete all documents
			delete(allIds);
			logger.info("DeleteAll operation completed. Deleted {} documents successfully", allIds.size());

		}
		catch (Exception e) {
			logger.error("Failed to execute deleteAll operation: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to delete all documents: " + e.getMessage(), e);
		}
	}

	/**
	 * Search documents by metadata field value
	 * @param fieldName the metadata field name
	 * @param fieldValue the field value to match
	 * @param topK maximum number of results
	 * @return list of matching documents
	 */
	public List<Document> searchByMetadata(String fieldName, Object fieldValue, int topK) {
		logger.debug("Searching documents by metadata: {}={}, topK={}", fieldName, fieldValue, topK);

		try {
			// Create filter for metadata field
			org.springframework.ai.vectorstore.filter.FilterExpressionBuilder builder = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder();
			Filter.Expression filter = builder.eq(fieldName, fieldValue).build();

			SearchRequest request = SearchRequest.builder()
				.query("*")
				.filterExpression(filter)
				.topK(topK)
				.similarityThreshold(0.0)
				.build();

			List<Document> results = similaritySearch(request);
			logger.info("Found {} documents with metadata {}={}", results.size(), fieldName, fieldValue);
			return results;

		}
		catch (Exception e) {
			logger.error("Failed to search by metadata: {}", e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Get the underlying SimpleVectorStore instance
	 * @return the wrapped SimpleVectorStore
	 */
	public SimpleVectorStore getSimpleVectorStore() {
		return simpleVectorStore;
	}

	/**
	 * Get the embedding model used by this vector store
	 * @return the embedding model
	 */
	public EmbeddingModel getEmbeddingModel() {
		return embeddingModel;
	}

}
