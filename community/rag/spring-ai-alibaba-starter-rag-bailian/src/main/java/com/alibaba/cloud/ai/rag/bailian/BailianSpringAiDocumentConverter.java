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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.aliyun.bailian20231229.models.RetrieveResponse;
import com.aliyun.bailian20231229.models.RetrieveResponseBody;
import org.springframework.ai.document.Document;

/**
 * Converter for transforming Bailian API responses to Spring AI Document objects.
 *
 * <p>This class handles the conversion between Bailian's retrieve response format
 * and Spring AI's Document format, ensuring seamless integration with Spring AI RAG
 * capabilities.
 */
public class BailianSpringAiDocumentConverter {

	private BailianSpringAiDocumentConverter() {
		// Utility class, prevent instantiation
	}

	/**
	 * Converts Bailian retrieve response to a list of Spring AI Documents.
	 *
	 * @param response the Bailian retrieve response
	 * @return a list of Spring AI Document objects
	 */
	public static List<Document> fromBailianResponse(RetrieveResponse response) {
		if (response == null || response.getBody() == null) {
			return List.of();
		}

		RetrieveResponseBody body = response.getBody();
		if (body.getData() == null || body.getData().getNodes() == null || body.getData().getNodes().isEmpty()) {
			return List.of();
		}

		return body.getData().getNodes().stream()
			.map(BailianSpringAiDocumentConverter::fromBailianNode)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	/**
	 * Converts a single Bailian node to a Spring AI Document.
	 *
	 * @param node the Bailian node from retrieve response
	 * @return a Spring AI Document object, or null if conversion fails
	 */
	public static Document fromBailianNode(RetrieveResponseBody.RetrieveResponseBodyDataNodes node) {
		if (node == null) {
			return null;
		}

		try {
			// Extract text content
			String text = node.getText();
			if (text == null || text.trim().isEmpty()) {
				return null;
			}

			// Build Spring AI Document metadata
			Map<String, Object> metadata = new HashMap<>();

			// Extract metadata (convert Object to Map safely)
			Map<String, Object> nodeMetadata = new HashMap<>();
			Object metadataObj = node.getMetadata();
			if (metadataObj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> metadataMap = (Map<String, Object>) metadataObj;
				nodeMetadata.putAll(metadataMap);
			}

			// Extract doc_id
			String docId = extractStringFromMetadata(nodeMetadata, "doc_id");
			if (docId == null || docId.isEmpty()) {
				docId = "unknown";
			}
			metadata.put("doc_id", docId);
			metadata.put("doc_name", docId); // Use doc_id as doc_name for compatibility

			// Extract chunk_id
			String chunkId = extractStringFromMetadata(nodeMetadata, "_id");
			if (chunkId != null) {
				metadata.put("chunk_id", chunkId);
			}

			// Add score if available
			if (node.getScore() != null) {
				metadata.put("score", node.getScore());
			}

			// Extract title if available in metadata
			Object titleObj = nodeMetadata.get("title");
			if (titleObj != null) {
				metadata.put("title", titleObj.toString());
			}

			// Copy all other metadata fields (excluding reserved fields)
			for (Map.Entry<String, Object> entry : nodeMetadata.entrySet()) {
				String key = entry.getKey();
				// Skip reserved fields that are already added
				if (!"doc_id".equals(key) && !"_id".equals(key) && !"title".equals(key)) {
					metadata.put(key, entry.getValue());
				}
			}

			// Create Spring AI Document
			return new Document(text, metadata);
		}
		catch (Exception e) {
			// Log error and return null to skip this document
			return null;
		}
	}

	/**
	 * Safely extracts a string value from metadata map.
	 *
	 * @param metadata the metadata map
	 * @param key the key to extract
	 * @return the string value, or null if not found or not a string
	 */
	private static String extractStringFromMetadata(Map<String, Object> metadata, String key) {
		if (metadata == null || key == null) {
			return null;
		}

		Object value = metadata.get(key);
		if (value instanceof String) {
			return (String) value;
		}

		return value != null ? value.toString() : null;
	}
}
