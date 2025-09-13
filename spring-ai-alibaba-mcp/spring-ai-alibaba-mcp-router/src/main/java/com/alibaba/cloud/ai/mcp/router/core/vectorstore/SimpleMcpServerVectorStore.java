/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 */

package com.alibaba.cloud.ai.mcp.router.core.vectorstore;

import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * 基于 Spring AI SimpleVectorStore 的 MCP 服务向量存储实现
 */
@Component
public class SimpleMcpServerVectorStore implements McpServerVectorStore {

	private static final Logger logger = LoggerFactory.getLogger(SimpleMcpServerVectorStore.class);

	private final EmbeddingModel embeddingModel;

	private final SimpleVectorStore vectorStore;

	// 初始化状态标识，用于控制是否使用全量读取策略
	private volatile boolean isInitialized = false;

	// 存储所有文档的副本，用于初始化阶段的全量读取
	private final List<Document> documentCache = new ArrayList<>();

	@Autowired(required = false)
	public SimpleMcpServerVectorStore(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
		if (embeddingModel != null) {
			this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
			logger.info("SimpleMcpServerVectorStore initialized with EmbeddingModel: {}",
					embeddingModel.getClass().getSimpleName());
		}
		else {
			// 如果没有 EmbeddingModel，创建一个空的 SimpleVectorStore
			this.vectorStore = null;
			logger
				.warn("SimpleMcpServerVectorStore initialized without EmbeddingModel - vector store will be disabled");
		}
	}

	public SimpleMcpServerVectorStore(EmbeddingModel embeddingModel, SimpleVectorStore vectorStore) {
		this.embeddingModel = embeddingModel;
		this.vectorStore = vectorStore;
	}

	@Override
	public boolean addServer(McpServerInfo serverInfo) {
		if (serverInfo == null || serverInfo.getName() == null) {
			return false;
		}

		try {
			Document document = convertToDocument(serverInfo);

			synchronized (documentCache) {
				documentCache.removeIf(doc -> serverInfo.getName().equals(doc.getMetadata().get("serviceName")));
				documentCache.add(document);
			}

			if (isInitialized && vectorStore != null) {
				vectorStore.add(List.of(document));
			}

			return true;
		}
		catch (Exception e) {
			logger.error("Failed to add server to vector store: {}", serverInfo.getName(), e);
			return false;
		}
	}

	@Override
	public boolean removeServer(String serviceName) {
		if (serviceName == null) {
			return false;
		}

		try {
			boolean removed = false;

			synchronized (documentCache) {
				removed = documentCache.removeIf(doc -> serviceName.equals(doc.getMetadata().get("serviceName")));
			}

			if (vectorStore != null) {
				SearchRequest searchRequest = SearchRequest.builder().query(serviceName).topK(1).build();
				List<Document> documents = vectorStore.similaritySearch(searchRequest);
				if (!documents.isEmpty()) {
					Document doc = documents.get(0);
					if (serviceName.equals(doc.getMetadata().get("serviceName"))) {
						vectorStore.delete(List.of(doc.getId()));
						removed = true;
					}
				}
			}

			return removed;
		}
		catch (Exception e) {
			logger.error("Failed to remove server from vector store: {}", serviceName, e);
			return false;
		}
	}

	@Override
	public McpServerInfo getServer(String serviceName) {
		if (serviceName == null) {
			return null;
		}

		try {
			if (!isInitialized) {
				synchronized (documentCache) {
					return documentCache.stream()
						.filter(doc -> serviceName.equals(doc.getMetadata().get("serviceName")))
						.map(this::convertFromDocument)
						.filter(Objects::nonNull)
						.findFirst()
						.orElse(null);
				}
			}

			if (vectorStore != null) {
				SearchRequest searchRequest = SearchRequest.builder().query(serviceName).topK(1).build();
				List<Document> documents = vectorStore.similaritySearch(searchRequest);
				if (!documents.isEmpty()) {
					Document doc = documents.get(0);
					if (serviceName.equals(doc.getMetadata().get("serviceName"))) {
						return convertFromDocument(doc);
					}
				}
			}

			return null;
		}
		catch (Exception e) {
			logger.error("Failed to get server from vector store: {}", serviceName, e);
			return null;
		}
	}

	@Override
	public List<McpServerInfo> getAllServers() {
		try {
			// 如果还在初始化阶段，直接从文档缓存返回全部数据，避免调用嵌入模型
			if (!isInitialized) {
				synchronized (documentCache) {
					return documentCache.stream()
						.map(this::convertFromDocument)
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
				}
			}

			// 正常阶段，使用向量存储（但仍然是全量获取，只是会经过嵌入模型处理）
			if (vectorStore != null) {
				SearchRequest searchRequest = SearchRequest.builder()
					.query("") // 空查询获取所有
					.topK(Integer.MAX_VALUE)
					.build();

				List<Document> documents = vectorStore.similaritySearch(searchRequest);
				return documents.stream()
					.map(this::convertFromDocument)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			}

			return new ArrayList<>();
		}
		catch (Exception e) {
			logger.error("Failed to get all servers from vector store", e);
			return new ArrayList<>();
		}
	}

	@Override
	public List<McpServerInfo> search(String query, int limit) {
		try {
			// 如果还在初始化阶段，使用简单的文本匹配而不是向量搜索
			if (!isInitialized) {
				synchronized (documentCache) {
					String lowerCaseQuery = query != null ? query.toLowerCase() : "";
					return documentCache.stream().filter(doc -> {
						if (query == null || query.isEmpty()) {
							return true; // 空查询返回所有
						}
						String serviceName = (String) doc.getMetadata().get("serviceName");
						String description = (String) doc.getMetadata().get("description");
						@SuppressWarnings("unchecked")
						List<String> tags = (List<String>) doc.getMetadata().get("tags");

						// 简单的文本匹配
						return (serviceName != null && serviceName.toLowerCase().contains(lowerCaseQuery))
								|| (description != null && description.toLowerCase().contains(lowerCaseQuery))
								|| (tags != null
										&& tags.stream().anyMatch(tag -> tag.toLowerCase().contains(lowerCaseQuery)));
					})
						.limit(limit)
						.map(this::convertFromDocument)
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
				}
			}

			// 正常搜索阶段，使用嵌入模型进行向量相似度搜索
			if (vectorStore != null) {
				SearchRequest searchRequest = SearchRequest.builder().query(query).topK(limit).build();
				List<Document> documents = vectorStore.similaritySearch(searchRequest);

				return documents.stream()
					.filter(doc -> doc.getScore() > 0.2) // 过滤低分结果
					.map(this::convertFromDocument)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			}

			return new ArrayList<>();
		}
		catch (Exception e) {
			logger.error("Failed to search vector store with query: '{}'", query, e);
			return new ArrayList<>();
		}
	}

	/**
	 * 关键词匹配搜索
	 */
	private List<Document> searchByKeywords(String query, int limit) {
		try {
			// 获取所有文档进行关键词匹配
			SearchRequest searchRequest = SearchRequest.builder().query("").topK(Integer.MAX_VALUE).build();
			List<Document> allDocuments = vectorStore.similaritySearch(searchRequest);

			logger.debug("Keyword search: found {} total documents to search in", allDocuments.size());

			// 如果查询为空，返回所有文档
			if (query == null || query.trim().isEmpty()) {
				return allDocuments.stream().map(doc -> {
					Map<String, Object> newMetadata = new HashMap<>(doc.getMetadata());
					newMetadata.put("keywordScore", 0.5);
					return new Document(doc.getId(), doc.getText(), newMetadata);
				}).limit(limit).collect(Collectors.toList());
			}

			String lowerQuery = query.toLowerCase().trim();

			return allDocuments.stream().filter(doc -> {
				// 检查服务名称
				String serviceName = (String) doc.getMetadata().get("serviceName");
				if (serviceName != null && serviceName.toLowerCase().contains(lowerQuery)) {
					logger.debug("Keyword match found in serviceName: {}", serviceName);
					return true;
				}

				// 检查描述信息
				String description = (String) doc.getMetadata().get("description");
				if (description != null && description.toLowerCase().contains(lowerQuery)) {
					logger.debug("Keyword match found in description: {}", description);
					return true;
				}

				// 检查标签
				@SuppressWarnings("unchecked")
				List<String> tags = (List<String>) doc.getMetadata().get("tags");
				if (tags != null && tags.stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery))) {
					logger.debug("Keyword match found in tags: {}", tags);
					return true;
				}

				// 检查协议
				String protocol = (String) doc.getMetadata().get("protocol");
				if (protocol != null && protocol.toLowerCase().contains(lowerQuery)) {
					logger.debug("Keyword match found in protocol: {}", protocol);
					return true;
				}

				// 检查版本
				String version = (String) doc.getMetadata().get("version");
				if (version != null && version.toLowerCase().contains(lowerQuery)) {
					logger.debug("Keyword match found in version: {}", version);
					return true;
				}

				// 检查端点
				String endpoint = (String) doc.getMetadata().get("endpoint");
				if (endpoint != null && endpoint.toLowerCase().contains(lowerQuery)) {
					logger.debug("Keyword match found in endpoint: {}", endpoint);
					return true;
				}

				return false;
			}).map(doc -> {
				// 为关键词匹配的结果创建新的Document对象并设置分数
				Map<String, Object> newMetadata = new HashMap<>(doc.getMetadata());
				newMetadata.put("keywordScore", 0.5);
				return new Document(doc.getId(), doc.getText(), newMetadata);
			}).limit(limit).collect(Collectors.toList());
		}
		catch (Exception e) {
			logger.error("Failed to search by keywords with query: '{}'", query, e);
			return new ArrayList<>();
		}
	}

	@Override
	public int size() {
		try {
			// 如果还在初始化阶段，直接返回文档缓存的大小
			if (!isInitialized) {
				synchronized (documentCache) {
					return documentCache.size();
				}
			}

			// 正常阶段，从向量存储获取大小
			if (vectorStore != null) {
				SearchRequest searchRequest = SearchRequest.builder().query("").topK(Integer.MAX_VALUE).build();
				List<Document> documents = vectorStore.similaritySearch(searchRequest);
				return documents.size();
			}

			return 0;
		}
		catch (Exception e) {
			logger.error("Failed to get vector store size", e);
			return 0;
		}
	}

	@Override
	public void clear() {
		try {
			synchronized (documentCache) {
				documentCache.clear();
			}

			if (vectorStore != null) {
				SearchRequest searchRequest = SearchRequest.builder().query("").topK(Integer.MAX_VALUE).build();
				List<Document> documents = vectorStore.similaritySearch(searchRequest);
				List<String> ids = documents.stream().map(Document::getId).collect(Collectors.toList());

				if (!ids.isEmpty()) {
					vectorStore.delete(ids);
				}
			}
		}
		catch (Exception e) {
			logger.error("Failed to clear vector store", e);
		}
	}

	/**
	 * 标记初始化完成，后续将使用嵌入模型进行向量搜索
	 */
	public void markInitializationComplete() {
		this.isInitialized = true;

		if (vectorStore != null) {
			synchronized (documentCache) {
				if (!documentCache.isEmpty()) {
					try {
						vectorStore.add(new ArrayList<>(documentCache));
					}
					catch (Exception e) {
						// 同步失败时保持初始化状态为false，继续使用缓存模式
						this.isInitialized = false;
						throw new RuntimeException("Failed to sync documents to vector store", e);
					}
				}
			}
		}
	}

	/**
	 * 检查是否已完成初始化
	 * @return 是否已完成初始化
	 */
	public boolean isInitializationComplete() {
		return this.isInitialized;
	}

	/**
	 * 重置为初始化状态，用于重新初始化
	 */
	public void resetInitializationState() {
		this.isInitialized = false;
	}

	/**
	 * 将 McpServerInfo 转换为 Document
	 */
	private Document convertToDocument(McpServerInfo serverInfo) {
		// 构建更丰富的搜索文本，增加描述信息的权重
		StringBuilder textBuilder = new StringBuilder();

		// 服务名称（高权重）
		textBuilder.append(serverInfo.getName()).append(" ");

		// 描述信息（高权重，重复添加以增加权重）
		if (serverInfo.getDescription() != null && !serverInfo.getDescription().trim().isEmpty()) {
			textBuilder.append(serverInfo.getDescription()).append(" ");
			// 再次添加描述信息以增加权重
			textBuilder.append(serverInfo.getDescription()).append(" ");
		}

		// 协议信息
		if (serverInfo.getProtocol() != null && !serverInfo.getProtocol().trim().isEmpty()) {
			textBuilder.append(serverInfo.getProtocol()).append(" ");
		}

		// 版本信息
		if (serverInfo.getVersion() != null && !serverInfo.getVersion().trim().isEmpty()) {
			textBuilder.append(serverInfo.getVersion()).append(" ");
		}

		// 端点信息
		if (serverInfo.getEndpoint() != null && !serverInfo.getEndpoint().trim().isEmpty()) {
			textBuilder.append(serverInfo.getEndpoint()).append(" ");
		}

		// 标签信息（中权重）
		if (serverInfo.getTags() != null && !serverInfo.getTags().isEmpty()) {
			textBuilder.append(String.join(" ", serverInfo.getTags())).append(" ");
		}

		Map<String, Object> metadata = Map.of("serviceName", serverInfo.getName(), "description",
				Optional.ofNullable(serverInfo.getDescription()).orElse(""), "protocol",
				Optional.ofNullable(serverInfo.getProtocol()).orElse(""), "version",
				Optional.ofNullable(serverInfo.getVersion()).orElse(""), "endpoint",
				Optional.ofNullable(serverInfo.getEndpoint()).orElse(""), "enabled",
				Optional.ofNullable(serverInfo.getEnabled()).orElse(true), "tags",
				Optional.ofNullable(serverInfo.getTags()).orElse(List.of()), "vectorType", "mcp_service");

		return new Document(serverInfo.getName(), textBuilder.toString().trim(), metadata);
	}

	/**
	 * 将 Document 转换为 McpServerInfo
	 */
	private McpServerInfo convertFromDocument(Document document) {
		try {
			String serviceName = (String) document.getMetadata().get("serviceName");
			String description = (String) document.getMetadata().get("description");
			String protocol = (String) document.getMetadata().get("protocol");
			String version = (String) document.getMetadata().get("version");
			String endpoint = (String) document.getMetadata().get("endpoint");
			Boolean enabled = (Boolean) document.getMetadata().get("enabled");
			@SuppressWarnings("unchecked")
			List<String> tags = (List<String>) document.getMetadata().get("tags");

			McpServerInfo serverInfo = new McpServerInfo(serviceName, description, protocol, version, endpoint, enabled,
					tags);

			Double score = document.getScore();
			if (score != null) {
				serverInfo.setScore(score);
			}

			return serverInfo;
		}
		catch (Exception e) {
			logger.error("Failed to convert document to McpServerInfo", e);
			return null;
		}
	}

	/**
	 * 调试方法：获取向量存储的详细信息
	 */
	public void debugVectorStore() {
		if (vectorStore == null) {
			logger.warn("Vector store is null - no EmbeddingModel available");
			return;
		}

		try {
			// 获取所有文档
			SearchRequest searchRequest = SearchRequest.builder().query("").topK(Integer.MAX_VALUE).build();
			List<Document> allDocuments = vectorStore.similaritySearch(searchRequest);

			logger.info("=== Vector Store Debug Information ===");
			logger.info("Total documents in vector store: {}", allDocuments.size());

			for (int i = 0; i < allDocuments.size(); i++) {
				Document doc = allDocuments.get(i);
				logger.info("Document {}: ID={}, Score={}", i + 1, doc.getId(), doc.getScore());
				logger.info("  ServiceName: {}", doc.getMetadata().get("serviceName"));
				logger.info("  Description: {}", doc.getMetadata().get("description"));
				logger.info("  Protocol: {}", doc.getMetadata().get("protocol"));
				logger.info("  Version: {}", doc.getMetadata().get("version"));
				logger.info("  Endpoint: {}", doc.getMetadata().get("endpoint"));
				logger.info("  Tags: {}", doc.getMetadata().get("tags"));
				logger.info("  Text content: {}",
						doc.getText().substring(0, Math.min(100, doc.getText().length())) + "...");
			}
			logger.info("=== End Debug Information ===");
		}
		catch (Exception e) {
			logger.error("Failed to debug vector store", e);
		}
	}

	/**
	 * 调试方法：测试特定查询的搜索
	 */
	public void debugSearch(String query, int limit) {
		logger.info("=== Search Debug for query: '{}' ===", query);

		if (vectorStore == null) {
			logger.warn("Vector store is null");
			return;
		}

		try {
			// 测试向量搜索
			SearchRequest vectorRequest = SearchRequest.builder().query(query).topK(limit * 2).build();
			List<Document> vectorResults = vectorStore.similaritySearch(vectorRequest);
			logger.info("Vector search results: {}", vectorResults.size());
			for (Document doc : vectorResults) {
				logger.info("  Vector result: {} (score: {})", doc.getMetadata().get("serviceName"), doc.getScore());
			}

			// 测试关键词搜索
			List<Document> keywordResults = searchByKeywords(query, limit);
			logger.info("Keyword search results: {}", keywordResults.size());
			for (Document doc : keywordResults) {
				logger.info("  Keyword result: {} (score: {})", doc.getMetadata().get("serviceName"), doc.getScore());
			}

			// 测试完整搜索
			List<McpServerInfo> fullResults = search(query, limit);
			logger.info("Full search results: {}", fullResults.size());
			for (McpServerInfo info : fullResults) {
				logger.info("  Full result: {} (score: {})", info.getName(), info.getScore());
			}

			logger.info("=== End Search Debug ===");
		}
		catch (Exception e) {
			logger.error("Failed to debug search", e);
		}
	}

}
