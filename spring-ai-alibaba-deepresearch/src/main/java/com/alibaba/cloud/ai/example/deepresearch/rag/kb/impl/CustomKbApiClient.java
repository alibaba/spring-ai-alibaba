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

package com.alibaba.cloud.ai.example.deepresearch.rag.kb.impl;

import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;
import com.alibaba.cloud.ai.example.deepresearch.rag.kb.ProfessionalKbApiClient;
import com.alibaba.cloud.ai.example.deepresearch.rag.kb.model.KbSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义专业知识库API客户端实现 支持通用的REST API调用
 *
 * @author hupei
 */
public class CustomKbApiClient implements ProfessionalKbApiClient {

	private static final Logger logger = LoggerFactory.getLogger(CustomKbApiClient.class);

	private final RestClient restClient;

	private final RagProperties.ProfessionalKnowledgeBases.KnowledgeBase.Api apiConfig;

	public CustomKbApiClient(RestClient restClient,
			RagProperties.ProfessionalKnowledgeBases.KnowledgeBase.Api apiConfig) {
		this.restClient = restClient;
		this.apiConfig = apiConfig;
	}

	@Override
	public List<KbSearchResult> search(String query, Map<String, Object> options) {
		if (!isAvailable()) {
			logger.warn("Custom KB API client is not properly configured");
			return Collections.emptyList();
		}

		try {
			// 构建请求URL
			int maxResults = (int) options.getOrDefault("maxResults", apiConfig.getMaxResults());
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiConfig.getUrl())
				.queryParam("q", query)
				.queryParam("limit", maxResults);

			// 发送GET请求
			RestClient.RequestHeadersSpec<?> request = restClient.get()
				.uri(builder.toUriString())
				.accept(MediaType.APPLICATION_JSON);

			if (apiConfig.getApiKey() != null && !apiConfig.getApiKey().trim().isEmpty()) {
				request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiConfig.getApiKey());
			}

			ResponseEntity<Map> response = request.retrieve().toEntity(Map.class);

			// 解析响应
			return parseResponse(response.getBody());

		}
		catch (Exception e) {
			logger.error("Error calling custom KB API: {}", apiConfig.getUrl(), e);
			return Collections.emptyList();
		}
	}

	@Override
	public String getProvider() {
		return "custom";
	}

	@Override
	public boolean isAvailable() {
		return apiConfig != null && apiConfig.getUrl() != null && !apiConfig.getUrl().trim().isEmpty();
	}

	/**
	 * 解析自定义API响应 支持多种常见的响应格式
	 */
	private List<KbSearchResult> parseResponse(Map<String, Object> responseBody) {
		List<KbSearchResult> results = new ArrayList<>();

		if (responseBody == null) {
			return results;
		}

		try {
			// 尝试解析不同的响应格式
			List<Map<String, Object>> items = extractItemsFromResponse(responseBody);

			for (Map<String, Object> item : items) {
				// 尝试多种字段名称映射
				String id = getStringValue(item, "id", "doc_id", "document_id");
				String title = getStringValue(item, "title", "name", "heading");
				String content = getStringValue(item, "content", "text", "body", "description");
				String url = getStringValue(item, "url", "link", "source_url");

				// 解析score
				Double score = getDoubleValue(item, "score", "relevance", "confidence");

				// 添加元数据
				Map<String, Object> metadata = new HashMap<>();
				metadata.put("source", "custom_api");
				metadata.put("provider", getProvider());
				metadata.put("api_url", apiConfig.getUrl());

				// 包含原始数据作为元数据
				metadata.put("original_data", item);

				// 使用record构造函数创建实例
				KbSearchResult result = new KbSearchResult(id, title, content, url, score, metadata);
				results.add(result);
			}
		}

		catch (Exception e) {
			logger.error("Error parsing custom API response", e);
		}

		return results;
	}

	/**
	 * 从响应中提取项目列表，支持多种格式
	 */
	private List<Map<String, Object>> extractItemsFromResponse(Map<String, Object> responseBody) {
		// 尝试常见的数组字段名
		String[] arrayFields = { "results", "data", "items", "documents", "docs" };

		for (String field : arrayFields) {
			Object value = responseBody.get(field);
			if (value instanceof List) {
				return (List<Map<String, Object>>) value;
			}
		}

		// 如果响应本身就是数组格式
		if (responseBody.containsKey("content") || responseBody.containsKey("title")) {
			return Collections.singletonList(responseBody);
		}

		return Collections.emptyList();
	}

	/**
	 * 从item中获取字符串值，尝试多个字段名
	 */
	private String getStringValue(Map<String, Object> item, String... fieldNames) {
		for (String fieldName : fieldNames) {
			Object value = item.get(fieldName);
			if (value instanceof String && !((String) value).trim().isEmpty()) {
				return (String) value;
			}
		}
		return null;
	}

	/**
	 * 从item中获取Double值，尝试多个字段名
	 */
	private Double getDoubleValue(Map<String, Object> item, String... fieldNames) {
		for (String fieldName : fieldNames) {
			Object value = item.get(fieldName);
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
			if (value instanceof String) {
				try {
					return Double.parseDouble((String) value);
				}
				catch (NumberFormatException e) {
					// 忽略解析错误
				}
			}
		}
		return null;
	}

}
