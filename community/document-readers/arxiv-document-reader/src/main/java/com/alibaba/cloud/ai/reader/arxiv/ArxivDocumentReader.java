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
 * arXiv文档阅读器，用于从arXiv获取和解析论文
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
	 * 创建arXiv文档阅读器
	 * @param queryString arXiv查询字符串
	 * @param maxSize 最大获取数量
	 */
	public ArxivDocumentReader(String queryString, int maxSize) {
		Assert.hasText(queryString, "Query string must not be empty");
		Assert.isTrue(maxSize > 0, "Max size must be greater than 0");

		this.queryString = queryString;
		this.maxSize = maxSize;
		this.parser = new PagePdfDocumentParser();
		this.arxivClient = new ArxivClient();
		this.arxivResource = new ArxivResource(queryString, maxSize);
	}

	/**
	 * 从ArxivResult创建元数据Map
	 * @param result arXiv搜索结果
	 * @return 包含所有非空字段的元数据Map
	 */
	private Map<String, Object> createMetadata(ArxivResult result) {
		Map<String, Object> metadata = new HashMap<>();

		// 使用函数式方法添加非空字段
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

		// 处理作者列表
		if (result.getAuthors() != null && !result.getAuthors().isEmpty()) {
			List<String> authorNames = result.getAuthors()
				.stream()
				.map(ArxivResult.ArxivAuthor::getName)
				.filter(name -> name != null && !name.trim().isEmpty())
				.toList();
			addIfNotEmpty(metadata, ArxivResource.AUTHORS, authorNames);
		}

		// 处理分类列表
		addIfNotEmpty(metadata, ArxivResource.CATEGORIES, result.getCategories());

		return metadata;
	}

	/**
	 * 添加非空值到元数据Map
	 */
	private void addIfNotNull(Map<String, Object> metadata, String key, Object value) {
		if (value != null) {
			metadata.put(key, value);
		}
	}

	/**
	 * 添加非空集合到元数据Map
	 */
	private void addIfNotEmpty(Map<String, Object> metadata, String key, List<?> value) {
		if (value != null && !value.isEmpty()) {
			metadata.put(key, value);
		}
	}

	/**
	 * 获取文档摘要列表，每个文档只包含元数据，不包含PDF内容
	 * @return 文档列表，只包含元数据
	 */
	public List<Document> getSummaries() {
		List<Document> documents = new ArrayList<>();
		try {
			ArxivSearch search = new ArxivSearch();
			search.setQuery(queryString);
			search.setMaxResults(maxSize);

			arxivClient.results(search, 0).forEachRemaining(result -> {
				// 检查是否已达到最大数量限制
				if (documents.size() >= maxSize) {
					return;
				}

				// 创建Document实例，使用摘要作为内容
				Map<String, Object> metadata = createMetadata(result);
				documents.add(new Document(result.getSummary(), metadata));
			});
		}
		catch (IOException e) {
			logger.error("Failed to get summaries from arXiv", e);
		}
		return documents;
	}

	@Override
	public List<Document> get() {
		List<Document> documents = new ArrayList<>();
		try {
			// 1. 创建搜索
			ArxivSearch search = new ArxivSearch();
			search.setQuery(arxivResource.getQueryString());
			search.setMaxResults(maxSize);

			// 2. 执行搜索并处理结果
			arxivClient.results(search, 0).forEachRemaining(result -> {
				// 检查是否已达到最大数量限制
				if (documents.size() >= maxSize) {
					return;
				}

				try {
					// 3. 下载PDF到临时文件
					Path tempDir = Files.createTempDirectory("arxiv-");
					Path pdfPath = arxivClient.downloadPdf(result, tempDir.toString());
					arxivResource.setTempFilePath(pdfPath);

					// 4. 解析PDF文档
					List<Document> parsedDocuments = parser.parse(arxivResource.getInputStream());

					// 5. 为每个文档添加元数据
					for (Document doc : parsedDocuments) {
						Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
						metadata.putAll(createMetadata(result));

						// 创建新的Document实例，包含完整的元数据
						documents.add(new Document(doc.getContent(), metadata));
					}

					// 6. 清理临时文件
					arxivResource.cleanup();
					Files.delete(tempDir);

				}
				catch (IOException e) {
					logger.error("Failed to process arXiv paper: " + result.getEntryId(), e);
				}
			});

		}
		catch (IOException e) {
			throw new RuntimeException("Failed to read documents from arXiv", e);
		}

		return documents;
	}

}