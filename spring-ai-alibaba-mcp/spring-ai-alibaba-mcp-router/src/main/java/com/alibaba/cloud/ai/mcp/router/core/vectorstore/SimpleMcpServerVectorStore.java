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

/**
 * 基于 Spring AI SimpleVectorStore 的 MCP 服务向量存储实现
 */
@Component
public class SimpleMcpServerVectorStore implements McpServerVectorStore {

	private final EmbeddingModel embeddingModel;

	private final SimpleVectorStore vectorStore;

	@Autowired(required = false)
	public SimpleMcpServerVectorStore(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
		if (embeddingModel != null) {
			this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
		}
		else {
			// 如果没有 EmbeddingModel，创建一个空的 SimpleVectorStore
			this.vectorStore = null;
		}
	}

	@Override
	public boolean addServer(McpServerInfo serverInfo) {
		if (serverInfo == null || serverInfo.getName() == null || vectorStore == null) {
			return false;
		}

		try {
			// 转换为 Document
			Document document = convertToDocument(serverInfo);
			vectorStore.add(List.of(document));
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean removeServer(String serviceName) {
		if (vectorStore == null) {
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
					return true;
				}
			}
			return false;
		}
		catch (Exception e) {
			return false;
		}
	}

	@Override
	public McpServerInfo getServer(String serviceName) {
		if (vectorStore == null) {
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
			return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<McpServerInfo> getAllServers() {
		if (vectorStore == null) {
			return new ArrayList<>();
		}

		try {
			// 获取所有文档
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
		catch (Exception e) {
			return new ArrayList<>();
		}
	}

	@Override
	public List<McpServerInfo> search(String query, int limit) {
		if (vectorStore == null) {
			return new ArrayList<>();
		}

		try {
			SearchRequest searchRequest = SearchRequest.builder().query(query).topK(limit).build();

			List<Document> documents = vectorStore.similaritySearch(searchRequest);

			return documents.stream()
				.filter(doc -> doc.getScore() > 0.2) // 过滤低分结果
				.map(this::convertFromDocument)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		}
		catch (Exception e) {
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
			return documents.size();
		}
		catch (Exception e) {
			return 0;
		}
	}

	@Override
	public void clear() {
		if (vectorStore == null) {
			return;
		}

		try {
			// 获取所有文档并删除
			SearchRequest searchRequest = SearchRequest.builder().query("").topK(Integer.MAX_VALUE).build();

			List<Document> documents = vectorStore.similaritySearch(searchRequest);
			List<String> ids = documents.stream().map(Document::getId).collect(Collectors.toList());

			if (!ids.isEmpty()) {
				vectorStore.delete(ids);
			}
		}
		catch (Exception e) {
			// 忽略异常
		}
	}

	/**
	 * 将 McpServerInfo 转换为 Document
	 */
	private Document convertToDocument(McpServerInfo serverInfo) {
		String text = serverInfo.getName() + " "
				+ (serverInfo.getDescription() != null ? serverInfo.getDescription() : "") + " "
				+ (serverInfo.getTags() != null ? String.join(" ", serverInfo.getTags()) : "");

		Map<String, Object> metadata = Map.of("serviceName", serverInfo.getName(), "description",
				Optional.ofNullable(serverInfo.getDescription()).orElse(""), "protocol",
				Optional.ofNullable(serverInfo.getProtocol()).orElse(""), "version",
				Optional.ofNullable(serverInfo.getVersion()).orElse(""), "endpoint",
				Optional.ofNullable(serverInfo.getEndpoint()).orElse(""), "enabled",
				Optional.ofNullable(serverInfo.getEnabled()).orElse(true), "tags",
				Optional.ofNullable(serverInfo.getTags()).orElse(List.of()), "vectorType", "mcp_service");

		return new Document(serverInfo.getName(), text, metadata);
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
			serverInfo.setScore(document.getScore());

			return serverInfo;
		}
		catch (Exception e) {
			return null;
		}
	}

}
