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
import org.apache.commons.collections.CollectionUtils;
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

	@Override
	public boolean addServer(McpServerInfo serverInfo) {
		if (serverInfo == null || serverInfo.getName() == null) {
			logger.warn("Cannot add server: serverInfo is null or name is null");
			return false;
		}

		if (vectorStore == null) {
			logger.warn("Cannot add server '{}': vectorStore is null (no EmbeddingModel available)",
					serverInfo.getName());
			return false;
		}

		try {
			// 转换为 Document
			Document document = convertToDocument(serverInfo);
			logger.debug("Adding server to vector store: {}", serverInfo.getName());
			vectorStore.add(List.of(document));
			logger.info("Successfully added server to vector store: {}", serverInfo.getName());
			return true;
		}
		catch (Exception e) {
			logger.error("Failed to add server to vector store: {}", serverInfo.getName(), e);
			return false;
		}
	}

	@Override
	public boolean removeServer(String serviceName) {
		if (vectorStore == null) {
			logger.warn("Cannot remove server '{}': vectorStore is null", serviceName);
			return false;
		}

		try {
			// 查找对应的文档
			SearchRequest searchRequest = SearchRequest.builder().query(serviceName).topK(1).build();

			List<Document> documents = vectorStore.similaritySearch(searchRequest);
			if (!documents.isEmpty()) {
				Document doc = documents.get(0);
				if (serviceName.equals(doc.getMetadata().get("serviceName"))) {
					vectorStore.delete(List.of(doc.getId()));
					logger.info("Successfully removed server from vector store: {}", serviceName);
					return true;
				}
			}
			logger.warn("Server not found in vector store: {}", serviceName);
			return false;
		}
		catch (Exception e) {
			logger.error("Failed to remove server from vector store: {}", serviceName, e);
			return false;
		}
	}

	@Override
	public McpServerInfo getServer(String serviceName) {
		if (vectorStore == null) {
			logger.warn("Cannot get server '{}': vectorStore is null", serviceName);
			return null;
		}

		try {
			// 通过服务名搜索
			SearchRequest searchRequest = SearchRequest.builder().query(serviceName).topK(1).build();

			List<Document> documents = vectorStore.similaritySearch(searchRequest);
			if (!documents.isEmpty()) {
				Document doc = documents.get(0);
				if (serviceName.equals(doc.getMetadata().get("serviceName"))) {
					return convertFromDocument(doc);
				}
			}
			logger.debug("Server not found in vector store: {}", serviceName);
			return null;
		}
		catch (Exception e) {
			logger.error("Failed to get server from vector store: {}", serviceName, e);
			return null;
		}
	}

	@Override
	public List<McpServerInfo> getAllServers() {
		if (vectorStore == null) {
			logger.warn("Cannot get all servers: vectorStore is null");
			return new ArrayList<>();
		}

		try {
			// 获取所有文档
			SearchRequest searchRequest = SearchRequest.builder()
				.query("") // 空查询获取所有
				.topK(Integer.MAX_VALUE)
				.build();

			List<Document> documents = vectorStore.similaritySearch(searchRequest);
			logger.debug("Found {} documents in vector store", documents.size());
			return documents.stream()
				.map(this::convertFromDocument)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		}
		catch (Exception e) {
			logger.error("Failed to get all servers from vector store", e);
			return new ArrayList<>();
		}
	}

	@Override
	public List<McpServerInfo> search(String query, int limit) {
		if (vectorStore == null) {
			logger.warn("Cannot search servers: vectorStore is null");
			return new ArrayList<>();
		}

		try {
			logger.debug("Searching vector store with query: '{}', limit: {}", query, limit);

			List<Document> documents = new ArrayList<>();

			// 策略1：向量相似度搜索（如果查询不为空）
			if (query != null && !query.trim().isEmpty()) {
				SearchRequest searchRequest = SearchRequest.builder().query(query).topK(limit * 2).build();
				List<Document> vectorResults = vectorStore.similaritySearch(searchRequest);
				logger.debug("Found {} documents in vector search results", vectorResults.size());
				documents.addAll(vectorResults);
			}

			// 策略2：关键词匹配搜索（总是执行，确保能找到结果）
			List<Document> keywordResults = searchByKeywords(query, limit);
			if (CollectionUtils.isNotEmpty(keywordResults)) {
				documents.addAll(keywordResults);
				logger.debug("Added {} documents from keyword search", keywordResults.size());
			}

			// 如果仍然没有结果，尝试获取所有服务器
			if (documents.isEmpty()) {
				logger.debug("No results found, trying to get all servers");
				SearchRequest allRequest = SearchRequest.builder().query("").topK(Integer.MAX_VALUE).build();
				List<Document> allDocuments = vectorStore.similaritySearch(allRequest);
				documents.addAll(allDocuments);
				logger.debug("Found {} total documents in vector store", allDocuments.size());
			}

			// 去重并排序
			return documents.stream().filter(doc -> {
				// 降低分数阈值，或者对于关键词匹配的结果不进行分数过滤
				double score = doc.getScore();
				Object keywordScore = doc.getMetadata().get("keywordScore");
				if (keywordScore != null) {
					// 关键词匹配的结果，使用关键词分数
					return ((Number) keywordScore).doubleValue() > 0.0;
				}
				// 向量搜索的结果，使用较低的阈值
				return score > 0.05; // 进一步降低阈值
			})
				.map(this::convertFromDocument)
				.filter(Objects::nonNull)
				.distinct() // 去重
				.sorted((a, b) -> Double.compare(b.getScore(), a.getScore())) // 按分数排序
				.limit(limit)
				.collect(Collectors.toList());
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
		if (vectorStore == null) {
			return 0;
		}

		try {
			SearchRequest searchRequest = SearchRequest.builder().query("").topK(Integer.MAX_VALUE).build();

			List<Document> documents = vectorStore.similaritySearch(searchRequest);
			logger.debug("Vector store size: {}", documents.size());
			return documents.size();
		}
		catch (Exception e) {
			logger.error("Failed to get vector store size", e);
			return 0;
		}
	}

	@Override
	public void clear() {
		if (vectorStore == null) {
			logger.warn("Cannot clear vector store: vectorStore is null");
			return;
		}

		try {
			// 获取所有文档并删除
			SearchRequest searchRequest = SearchRequest.builder().query("").topK(Integer.MAX_VALUE).build();

			List<Document> documents = vectorStore.similaritySearch(searchRequest);
			List<String> ids = documents.stream().map(Document::getId).collect(Collectors.toList());

			if (!ids.isEmpty()) {
				vectorStore.delete(ids);
				logger.info("Cleared {} documents from vector store", ids.size());
			}
		}
		catch (Exception e) {
			logger.error("Failed to clear vector store", e);
		}
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

			// 优先使用关键词搜索的分数，如果没有则使用向量搜索的分数
			Object keywordScore = document.getMetadata().get("keywordScore");
			if (keywordScore != null) {
				serverInfo.setScore(((Number) keywordScore).doubleValue());
			}
			else {
				serverInfo.setScore(document.getScore());
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
