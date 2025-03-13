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
package com.alibaba.cloud.ai.reader.gitbook;

import com.alibaba.cloud.ai.reader.gitbook.model.GitbookPage;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A document reader implementation that reads content from Gitbook. This reader connects
 * to the Gitbook API to fetch documents and their metadata, then converts them into
 * Spring AI Document objects.
 *
 * <p>
 * The reader supports customization of:
 * <ul>
 * <li>API Token - Required for authentication with Gitbook API</li>
 * <li>Space ID - The Gitbook space to read documents from</li>
 * <li>API URL - Optional custom API endpoint</li>
 * <li>Metadata Fields - Optional list of fields to include in document metadata</li>
 * </ul>
 *
 * @author brianxiadong
 */
public class GitbookDocumentReader implements DocumentReader {

	private final String apiToken;

	private final String spaceId;

	private final String apiUrl;

	private final List<String> metadataFields;

	/**
	 * Creates a new GitbookDocumentReader with the minimum required parameters.
	 * @param apiToken The Gitbook API token for authentication
	 * @param spaceId The ID of the Gitbook space to read from
	 */
	public GitbookDocumentReader(String apiToken, String spaceId) {
		this(apiToken, spaceId, null, null);
	}

	/**
	 * Creates a new GitbookDocumentReader with all configurable parameters.
	 * @param apiToken The Gitbook API token for authentication
	 * @param spaceId The ID of the Gitbook space to read from
	 * @param apiUrl Optional custom API URL (if null, uses default Gitbook API endpoint)
	 * @param metadataFields Optional list of metadata fields to include in documents
	 */
	public GitbookDocumentReader(String apiToken, String spaceId, String apiUrl, List<String> metadataFields) {
		Assert.hasText(apiToken, "API Token must not be empty");
		Assert.hasText(spaceId, "Space ID must not be empty");

		this.apiToken = apiToken;
		this.spaceId = spaceId;
		this.apiUrl = apiUrl;
		this.metadataFields = metadataFields;
	}

	/**
	 * Retrieves all documents from the configured Gitbook space. Each page in the Gitbook
	 * space is converted to a Document object. The document's content is the page's
	 * markdown content, and its metadata includes the configured metadata fields from the
	 * page's properties.
	 * @return A list of Document objects representing the Gitbook pages
	 * @throws RuntimeException if there's an error fetching or processing the documents
	 */
	@Override
	public List<Document> get() {
		GitbookClient client = new GitbookClient(apiToken, apiUrl);
		List<Document> documents = new ArrayList<>();

		try {
			// Fetch all pages from the space
			List<GitbookPage> pages = client.listPages(spaceId);

			// Convert each page to a Document
			for (GitbookPage page : pages) {
				String content = client.getPageMarkdown(spaceId, page.getId());

				// Skip pages with no content
				if (content == null || content.isEmpty()) {
					continue;
				}

				// Build metadata map
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("path", page.getPath());

				// Add additional metadata fields if configured
				if (metadataFields != null) {
					for (String field : metadataFields) {
						switch (field) {
							case "title" -> metadata.put(field, page.getTitle());
							case "description" -> metadata.put(field, page.getDescription());
							case "parent" -> metadata.put(field, page.getParent());
							case "type" -> metadata.put(field, page.getType());
						}
					}
				}

				// Create and add the document
				Document document = new Document(page.getId(), content, metadata);
				documents.add(document);
			}

			return documents;
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to load documents from Gitbook", e);
		}
	}

}
