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

package com.alibaba.cloud.ai.example.deepresearch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A service for ingesting data into the vector store from various sources. It handles
 * document reading, splitting, and vectorization.
 *
 * @author hupei
 */
@Service
public class VectorStoreDataIngestionService {

	private static final Logger logger = LoggerFactory.getLogger(VectorStoreDataIngestionService.class);

	private final VectorStore vectorStore;

	private final TokenTextSplitter textSplitter;

	public VectorStoreDataIngestionService(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
		//todo:在配置类中进行更灵活的配置
		this.textSplitter = new TokenTextSplitter(800, 100, 5,
				10000, true);
	}

	/**
	 * 从单个资源加载、处理并存入向量数据库
	 * @param resource Spring Resource, e.g., FileSystemResource, ClassPathResource, or
	 * MultipartFile.getResource()
	 */
	public void ingest(Resource resource) {
		try {
			if (!resource.exists() || resource.contentLength() == 0) {
				logger.warn("Skipping ingestion for empty or non-existent resource: {}", resource.getFilename());
				return;
			}

			logger.info("Ingesting data from resource: {}", resource.getFilename());
			// TikaDocumentReader 支持多种文档格式(PDF, DOCX, MD, etc.)
			var documentReader = new TikaDocumentReader(resource);
			List<Document> documents = documentReader.get();
			List<Document> splitDocuments = this.textSplitter.apply(documents);
			this.vectorStore.add(splitDocuments);
			logger.info("Successfully ingested {} splits from {}", splitDocuments.size(), resource.getFilename());

		}
		catch (Exception e) {
			logger.error("Failed to ingest data from resource: " + resource.getFilename(), e);
		}
	}

	/**
	 * 批量处理多个资源
	 * @param resources List of resources to ingest
	 */
	public void ingest(List<Resource> resources) {
		resources.forEach(this::ingest);
	}

	/**
	 * 处理并存储上传的文件
	 * @param file 上传的文件
	 * @param sessionId 会话ID
	 * @param userId 用户ID
	 */
	public void processAndStore(MultipartFile file, String sessionId, String userId) {
		// 1. 解析
		TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
		List<Document> documents = reader.get();

		// 2. 分块
		List<Document> chunks = textSplitter.apply(documents);

		// 3. 元数据富化
		AtomicInteger chunkCounter = new AtomicInteger(0);
		List<Document> enrichedChunks = chunks.stream().map(chunk -> {
			Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
			metadata.put("source_type", "user_upload");
			metadata.put("session_id", sessionId);
			if (userId!= null &&!userId.isBlank()) {
				metadata.put("user_id", userId);
			}
			metadata.put("original_filename", file.getOriginalFilename());
			metadata.put("upload_timestamp", Instant.now().toString());
			metadata.put("chunk_id", chunkCounter.getAndIncrement());

			return new Document(chunk.getId(), chunk.getText(), metadata);
		}).collect(Collectors.toList());

		// 4. 存储
		vectorStore.add(enrichedChunks);
	}
}
