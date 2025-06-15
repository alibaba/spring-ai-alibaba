package com.alibaba.cloud.ai.example.deepresearch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

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
		this.textSplitter = new TokenTextSplitter();
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

}