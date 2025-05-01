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
package com.alibaba.cloud.ai.reader.arxiv;

import com.alibaba.cloud.ai.document.DocumentParser;
import com.alibaba.cloud.ai.parser.apache.pdfbox.PagePdfDocumentParser;
import com.alibaba.cloud.ai.reader.arxiv.client.ArxivClient;
import com.alibaba.cloud.ai.reader.arxiv.client.ArxivResult;
import com.alibaba.cloud.ai.reader.arxiv.client.ArxivSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ArXiv document reader for retrieving and parsing papers from arXiv
 *
 * @author brianxiadong
 */
public class ArxivDocumentReader implements DocumentReader {

	private static final Logger logger = LoggerFactory.getLogger(ArxivDocumentReader.class);

	private final DocumentParser parser;

	private final String queryString;

	private final int maxSize;

	private final ArxivClient arxivClient;

	private final ArxivResource arxivResource;

	/**
	 * Create an arXiv document reader
	 * @param queryString arXiv query string
	 * @param maxSize maximum number of documents to retrieve
	 */
	public ArxivDocumentReader(String queryString, int maxSize) {
		Assert.hasText(queryString, "Query string must not be empty");
		Assert.isTrue(maxSize > 0, "Max size must be greater than 0");

		this.queryString = queryString;
		this.maxSize = maxSize;
		this.parser = (DocumentParser) new PagePdfDocumentParser();
		this.arxivClient = new ArxivClient();
		this.arxivResource = new ArxivResource(queryString, maxSize);
	}

	/**
	 * Create metadata map from ArxivResult
	 * @param result arXiv search result
	 * @return metadata map containing all non-null fields
	 */
	private Map<String, Object> createMetadata(ArxivResult result) {
		Map<String, Object> metadata = new HashMap<>();

		// Add non-null fields using functional approach
		addIfNotNull(metadata, ArxivResource.ENTRY_ID, result.getEntryId());
		addIfNotNull(metadata, ArxivResource.TITLE, result.getTitle());
		addIfNotNull(metadata, ArxivResource.SUMMARY, result.getSummary());
		addIfNotNull(metadata, ArxivResource.PRIMARY_CATEGORY, result.getPrimaryCategory());
		addIfNotNull(metadata, ArxivResource.PUBLISHED, result.getPublished());
		addIfNotNull(metadata, ArxivResource.UPDATED, result.getUpdated());
		addIfNotNull(metadata, ArxivResource.DOI, result.getDoi());
		addIfNotNull(metadata, ArxivResource.JOURNAL_REF, result.getJournalRef());
		addIfNotNull(metadata, ArxivResource.COMMENT, result.getComment());
		addIfNotNull(metadata, ArxivResource.PDF_URL, result.getPdfUrl());

		// Process author list
		if (result.getAuthors() != null && !result.getAuthors().isEmpty()) {
			List<String> authorNames = result.getAuthors()
				.stream()
				.map(ArxivResult.ArxivAuthor::getName)
				.filter(name -> name != null && !name.trim().isEmpty())
				.toList();
			addIfNotEmpty(metadata, ArxivResource.AUTHORS, authorNames);
		}

		// Process categories list
		addIfNotEmpty(metadata, ArxivResource.CATEGORIES, result.getCategories());

		return metadata;
	}

	/**
	 * Add non-null value to metadata map
	 */
	private void addIfNotNull(Map<String, Object> metadata, String key, Object value) {
		if (value != null) {
			metadata.put(key, value);
		}
	}

	/**
	 * Add non-empty collection to metadata map
	 */
	private void addIfNotEmpty(Map<String, Object> metadata, String key, List<?> value) {
		if (value != null && !value.isEmpty()) {
			metadata.put(key, value);
		}
	}

	/**
	 * Get list of document summaries, each document contains only metadata without PDF
	 * content
	 * @return list of documents with metadata only
	 */
	public List<Document> getSummaries() {
		List<Document> documents = new ArrayList<>();
		try {
			ArxivSearch search = new ArxivSearch();
			search.setQuery(queryString);
			search.setMaxResults(maxSize);

			arxivClient.results(search, 0).forEachRemaining(result -> {
				// Check if maximum limit is reached
				if (documents.size() >= maxSize) {
					return;
				}

				// Create Document instance using summary as content
				Map<String, Object> metadata = createMetadata(result);
				documents.add(new Document(result.getSummary(), metadata));
			});
		}
		catch (IOException e) {
			logger.error("Failed to get summaries from arXiv", e);
			throw new RuntimeException("Failed to get summaries from arXiv", e);
		}
		return documents;
	}

	@Override
	public List<Document> get() {
		List<Document> documents = new ArrayList<>();
		try {
			// 1. Create search
			ArxivSearch search = new ArxivSearch();
			search.setQuery(arxivResource.getQueryString());
			search.setMaxResults(maxSize);

			// 2. Execute search and process results
			arxivClient.results(search, 0).forEachRemaining(result -> {
				// Check if maximum limit is reached
				if (documents.size() >= maxSize) {
					return;
				}

				Path tempDir = null;
				try {
					// 3. Download PDF to temporary file
					tempDir = Files.createTempDirectory("arxiv-");
					Path pdfPath = arxivClient.downloadPdf(result, tempDir.toString());
					arxivResource.setTempFilePath(pdfPath);

					// 4. Parse PDF document
					List<Document> parsedDocuments = parser.parse(arxivResource.getInputStream());

					// 5. Add metadata to each document
					for (Document doc : parsedDocuments) {
						Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
						metadata.putAll(createMetadata(result));

						// Create new Document instance with complete metadata
						documents.add(new Document(doc.getText(), metadata));
					}
				}
				catch (IOException e) {
					logger.error("Failed to process arXiv paper: {}", result.getEntryId(), e);
				}
				finally {
					// Clean up temporary files
					arxivResource.cleanup();
					if (tempDir != null) {
						try {
							Files.delete(tempDir);
						}
						catch (IOException e) {
							logger.error("Failed to delete temporary directory", e);
						}
					}
				}
			});
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to read documents from arXiv", e);
		}

		return documents;
	}

}
