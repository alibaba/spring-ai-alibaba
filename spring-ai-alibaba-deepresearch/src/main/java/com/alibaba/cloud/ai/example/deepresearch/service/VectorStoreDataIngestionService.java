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

import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;
import com.alibaba.cloud.ai.example.deepresearch.rag.SourceTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
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

	private final RagProperties ragProperties;

	public VectorStoreDataIngestionService(@Qualifier("ragVectorStore") VectorStore vectorStore,
			RagProperties ragProperties) {
		this.vectorStore = vectorStore;
		this.ragProperties = ragProperties;

		// 使用配置化的文本分割器
		RagProperties.TextSplitter splitterConfig = ragProperties.getTextSplitter();
		this.textSplitter = new TokenTextSplitter(splitterConfig.getDefaultChunkSize(), splitterConfig.getOverlap(),
				splitterConfig.getMinChunkSizeToSplit(), splitterConfig.getMaxChunkSize(),
				splitterConfig.isKeepSeparator());

		logger.info(
				"Initialized VectorStoreDataIngestionService with text splitter config: "
						+ "chunkSize={}, overlap={}, minChunkSize={}, maxChunkSize={}, keepSeparator={}",
				splitterConfig.getDefaultChunkSize(), splitterConfig.getOverlap(),
				splitterConfig.getMinChunkSizeToSplit(), splitterConfig.getMaxChunkSize(),
				splitterConfig.isKeepSeparator());
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
		batchProcessAndStore(List.of(file), sessionId, userId);
	}

	/**
	 * 批量处理并存储上传的文件
	 * @param files 上传的文件列表
	 * @param sessionId 会话ID
	 * @param userId 用户ID
	 * @return 成功处理的文档片段数量
	 */
	public int batchProcessAndStore(List<MultipartFile> files, String sessionId, String userId) {
		if (files == null || files.isEmpty()) {
			logger.warn("No files provided for user upload");
			return 0;
		}

		logger.info("Starting batch upload for user: sessionId={}, userId={}, fileCount={}", sessionId, userId,
				files.size());

		int totalChunks = 0;
		String uploadTimestamp = Instant.now().toString();

		for (MultipartFile file : files) {
			try {
				if (file.isEmpty()) {
					logger.warn("Skipping empty file: {}", file.getOriginalFilename());
					continue;
				}

				// 1. 解析文档
				TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
				List<Document> documents = reader.get();

				// 2. 分块
				List<Document> chunks = textSplitter.apply(documents);

				// 3. 元数据富化
				AtomicInteger chunkCounter = new AtomicInteger(0);
				List<Document> enrichedChunks = chunks.stream()
					.map(chunk -> enrichUserUploadMetadata(chunk, file.getOriginalFilename(), sessionId, userId,
							uploadTimestamp, chunkCounter.getAndIncrement(), file.getSize(), file.getContentType()))
					.collect(Collectors.toList());

				// 4. 存储
				vectorStore.add(enrichedChunks);
				totalChunks += enrichedChunks.size();

				logger.info("Successfully uploaded user file {} to vector store: {} chunks", file.getOriginalFilename(),
						enrichedChunks.size());

			}
			catch (Exception e) {
				logger.error("Failed to upload user file {} to vector store", file.getOriginalFilename(), e);
			}
		}

		logger.info("Batch upload for user completed: sessionId={}, userId={}, totalChunks={}", sessionId, userId,
				totalChunks);
		return totalChunks;
	}

	/**
	 * 从Resource批量处理并存储用户文件
	 * @param resources 资源列表
	 * @param sessionId 会话ID
	 * @param userId 用户ID
	 * @return 成功处理的文档片段数量
	 */
	public int batchProcessAndStoreResources(List<Resource> resources, String sessionId, String userId) {
		if (resources == null || resources.isEmpty()) {
			logger.warn("No resources provided for user upload");
			return 0;
		}

		logger.info("Starting batch upload resources for user: sessionId={}, userId={}, resourceCount={}", sessionId,
				userId, resources.size());

		int totalChunks = 0;
		String uploadTimestamp = Instant.now().toString();

		for (Resource resource : resources) {
			try {
				if (!resource.exists() || resource.contentLength() == 0) {
					logger.warn("Skipping empty or non-existent resource: {}", resource.getFilename());
					continue;
				}

				// 1. 解析文档
				TikaDocumentReader reader = new TikaDocumentReader(resource);
				List<Document> documents = reader.get();

				// 2. 分块
				List<Document> chunks = textSplitter.apply(documents);

				// 3. 元数据富化
				AtomicInteger chunkCounter = new AtomicInteger(0);
				List<Document> enrichedChunks = chunks.stream()
					.map(chunk -> enrichUserResourceUploadMetadata(chunk, resource.getFilename(), sessionId, userId,
							uploadTimestamp, chunkCounter.getAndIncrement()))
					.collect(Collectors.toList());

				// 4. 存储
				vectorStore.add(enrichedChunks);
				totalChunks += enrichedChunks.size();

				logger.info("Successfully uploaded user resource {} to vector store: {} chunks", resource.getFilename(),
						enrichedChunks.size());

			}
			catch (Exception e) {
				logger.error("Failed to upload user resource {} to vector store", resource.getFilename(), e);
			}
		}

		logger.info("Batch upload resources for user completed: sessionId={}, userId={}, totalChunks={}", sessionId,
				userId, totalChunks);
		return totalChunks;
	}

	/**
	 * 批量上传文档到专业知识库ES 与ProfessionalKbEsStrategy的元数据保持一致
	 * @param files 上传的文件列表
	 * @param kbId 知识库ID
	 * @param kbName 知识库名称
	 * @param kbDescription 知识库描述
	 * @param category 文档分类（可选）
	 * @return 上传成功的文档数量
	 */
	public int batchUploadToProfessionalKbEs(List<MultipartFile> files, String kbId, String kbName,
			String kbDescription, String category) {
		if (files == null || files.isEmpty()) {
			logger.warn("No files provided for professional KB upload");
			return 0;
		}

		logger.info("Starting batch upload to professional KB ES: kbId={}, kbName={}, fileCount={}", kbId, kbName,
				files.size());

		int totalChunks = 0;
		String uploadTimestamp = Instant.now().toString();

		for (MultipartFile file : files) {
			try {
				if (file.isEmpty()) {
					logger.warn("Skipping empty file: {}", file.getOriginalFilename());
					continue;
				}

				// 1. 解析文档
				TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
				List<Document> documents = reader.get();

				// 2. 分块
				List<Document> chunks = textSplitter.apply(documents);

				// 3. 元数据富化，与ProfessionalKbEsStrategy保持一致
				AtomicInteger chunkCounter = new AtomicInteger(0);
				List<Document> enrichedChunks = chunks.stream()
					.map(chunk -> enrichProfessionalKbEsMetadata(chunk, file.getOriginalFilename(), kbId, kbName,
							kbDescription, category, uploadTimestamp, chunkCounter.getAndIncrement(), file.getSize(),
							file.getContentType()))
					.collect(Collectors.toList());

				// 4. 存储到ES
				vectorStore.add(enrichedChunks);
				totalChunks += enrichedChunks.size();

				logger.info("Successfully uploaded file {} to professional KB ES: {} chunks",
						file.getOriginalFilename(), enrichedChunks.size());

			}
			catch (Exception e) {
				logger.error("Failed to upload file {} to professional KB ES", file.getOriginalFilename(), e);
			}
		}

		logger.info("Batch upload to professional KB ES completed: kbId={}, totalChunks={}", kbId, totalChunks);
		return totalChunks;
	}

	/**
	 * 单个文档上传到专业知识库ES
	 * @param file 上传的文件
	 * @param kbId 知识库ID
	 * @param kbName 知识库名称
	 * @param kbDescription 知识库描述
	 * @param category 文档分类（可选）
	 * @return 上传成功的文档片段数量
	 */
	public int uploadToProfessionalKbEs(MultipartFile file, String kbId, String kbName, String kbDescription,
			String category) {
		return batchUploadToProfessionalKbEs(List.of(file), kbId, kbName, kbDescription, category);
	}

	/**
	 * 从Resource批量上传到专业知识库ES
	 * @param resources 资源列表
	 * @param kbId 知识库ID
	 * @param kbName 知识库名称
	 * @param kbDescription 知识库描述
	 * @param category 文档分类（可选）
	 * @return 上传成功的文档片段数量
	 */
	public int batchUploadResourcesToProfessionalKbEs(List<Resource> resources, String kbId, String kbName,
			String kbDescription, String category) {
		if (resources == null || resources.isEmpty()) {
			logger.warn("No resources provided for professional KB upload");
			return 0;
		}

		logger.info("Starting batch upload resources to professional KB ES: kbId={}, kbName={}, resourceCount={}", kbId,
				kbName, resources.size());

		int totalChunks = 0;
		String uploadTimestamp = Instant.now().toString();

		for (Resource resource : resources) {
			try {
				if (!resource.exists() || resource.contentLength() == 0) {
					logger.info("Skipping empty or non-existent resource: {}", resource.getFilename());
					continue;
				}

				// 1. 解析文档
				TikaDocumentReader reader = new TikaDocumentReader(resource);
				List<Document> documents = reader.get();

				// 2. 分块
				List<Document> chunks = textSplitter.apply(documents);

				// 3. 元数据富化，与ProfessionalKbEsStrategy保持一致
				AtomicInteger chunkCounter = new AtomicInteger(0);
				List<Document> enrichedChunks = chunks.stream()
					.map(chunk -> enrichProfessionalKbEsResourceMetadata(chunk, resource.getFilename(), kbId, kbName,
							kbDescription, category, uploadTimestamp, chunkCounter.getAndIncrement()))
					.collect(Collectors.toList());

				// 4. 存储到ES
				vectorStore.add(enrichedChunks);
				totalChunks += enrichedChunks.size();

				logger.info("Successfully uploaded resource {} to professional KB ES: {} chunks",
						resource.getFilename(), enrichedChunks.size());

			}
			catch (Exception e) {
				logger.error("Failed to upload resource {} to professional KB ES", resource.getFilename(), e);
			}
		}

		logger.info("Batch upload resources to professional KB ES completed: kbId={}, totalChunks={}", kbId,
				totalChunks);
		return totalChunks;
	}

	/**
	 * 从文本内容提取标题
	 * @param text 文本内容
	 * @param filename 文件名（回退标题）
	 * @return 提取的标题
	 */
	private String extractTitle(String text, String filename) {
		if (text == null || text.isBlank()) {
			return filename != null ? filename : "Untitled";
		}

		// 简单的标题提取逻辑：取第一行作为标题，最多50个字符
		String firstLine = text.split("\n")[0].trim();
		if (firstLine.length() > 50) {
			firstLine = firstLine.substring(0, 50) + "...";
		}

		return firstLine.isEmpty() ? (filename != null ? filename : "Untitled") : firstLine;
	}

	/**
	 * 为用户上传的MultipartFile丰富元数据
	 */
	private Document enrichUserUploadMetadata(Document chunk, String originalFilename, String sessionId, String userId,
			String uploadTimestamp, int chunkId, long fileSize, String contentType) {
		Map<String, Object> metadata = createBaseMetadata(chunk, originalFilename, uploadTimestamp, chunkId);

		// 用户上传文件特有元数据
		metadata.put("file_size", fileSize);
		metadata.put("content_type", contentType);

		// 核心元数据
		metadata.put("source_type", SourceTypeEnum.USER_UPLOAD.getValue());

		if (userId != null && !userId.isBlank()) {
			metadata.put("user_id", userId);
		}

		metadata.put("session_id", sessionId);
		return new Document(chunk.getId(), chunk.getText(), metadata);
	}

	/**
	 * 为用户上传的Resource丰富元数据
	 */
	private Document enrichUserResourceUploadMetadata(Document chunk, String originalFilename, String sessionId,
			String userId, String uploadTimestamp, int chunkId) {
		Map<String, Object> metadata = createBaseMetadata(chunk, originalFilename, uploadTimestamp, chunkId);

		// 核心元数据
		metadata.put("source_type", SourceTypeEnum.USER_UPLOAD.getValue());
		metadata.put("session_id", sessionId);
		if (userId != null && !userId.isBlank()) {
			metadata.put("user_id", userId);
		}

		return new Document(chunk.getId(), chunk.getText(), metadata);
	}

	/**
	 * 为专业知识库ES的MultipartFile丰富元数据
	 */
	private Document enrichProfessionalKbEsMetadata(Document chunk, String originalFilename, String kbId, String kbName,
			String kbDescription, String category, String uploadTimestamp, int chunkId, long fileSize,
			String contentType) {
		Map<String, Object> metadata = createBaseMetadata(chunk, originalFilename, uploadTimestamp, chunkId);

		// 核心元数据，与ProfessionalKbEsStrategy一致
		metadata.put("source_type", SourceTypeEnum.PROFESSIONAL_KB_ES.getValue());
		metadata.put("session_id", "professional_kb_es");

		// 专业知识库文件特有元数据
		metadata.put("file_size", fileSize);
		metadata.put("content_type", contentType);

		// 可选分类
		if (category != null && !category.isBlank()) {
			metadata.put("category", category);
		}

		// 专业知识库特有元数据
		metadata.put("kb_id", kbId);
		metadata.put("kb_name", kbName);
		metadata.put("kb_description", kbDescription);

		return new Document(chunk.getId(), chunk.getText(), metadata);
	}

	/**
	 * 为专业知识库ES的Resource丰富元数据
	 */
	private Document enrichProfessionalKbEsResourceMetadata(Document chunk, String originalFilename, String kbId,
			String kbName, String kbDescription, String category, String uploadTimestamp, int chunkId) {
		Map<String, Object> metadata = createBaseMetadata(chunk, originalFilename, uploadTimestamp, chunkId);

		// 核心元数据，与ProfessionalKbEsStrategy一致
		metadata.put("source_type", SourceTypeEnum.PROFESSIONAL_KB_ES.getValue());
		metadata.put("session_id", "professional_kb_es");

		// 专业知识库特有元数据
		metadata.put("kb_id", kbId);
		metadata.put("kb_name", kbName);
		metadata.put("kb_description", kbDescription);

		// 可选分类
		if (category != null && !category.isBlank()) {
			metadata.put("category", category);
		}

		return new Document(chunk.getId(), chunk.getText(), metadata);
	}

	/**
	 * 创建基础元数据
	 */
	private Map<String, Object> createBaseMetadata(Document chunk, String originalFilename, String uploadTimestamp,
			int chunkId) {
		Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());

		// 文档元数据
		metadata.put("original_filename", originalFilename);
		metadata.put("upload_timestamp", uploadTimestamp);
		metadata.put("chunk_id", chunkId);

		// 添加搜索用的标题字段
		String title = extractTitle(chunk.getText(), originalFilename);
		metadata.put("title", title);

		return metadata;
	}

}
