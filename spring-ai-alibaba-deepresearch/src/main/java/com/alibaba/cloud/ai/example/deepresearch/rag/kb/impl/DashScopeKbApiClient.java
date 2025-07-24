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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DashScope专业知识库API客户端实现
 *
 * @author hupei
 */
public class DashScopeKbApiClient implements ProfessionalKbApiClient {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeKbApiClient.class);

	private final RestClient restClient;

	private final RagProperties.ProfessionalKnowledgeBases.KnowledgeBase.Api apiConfig;

	public DashScopeKbApiClient(RestClient restClient,
			RagProperties.ProfessionalKnowledgeBases.KnowledgeBase.Api apiConfig) {
		this.restClient = restClient;
		this.apiConfig = apiConfig;
	}

	@Override
	public List<KbSearchResult> search(String query, Map<String, Object> options) {
		if (!isAvailable()) {
			logger.warn("DashScope KB API client is not properly configured");
			return Collections.emptyList();
		}

		try {
			// 构建请求体
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("model", apiConfig.getModel() != null ? apiConfig.getModel() : "text-embedding-v1");
			requestBody.put("input", Map.of("query", query));

			// 从options中获取maxResults，如果没有则使用配置的默认值
			int maxResults = (int) options.getOrDefault("maxResults", apiConfig.getMaxResults());
			requestBody.put("parameters", Map.of("size", maxResults));

			// 构建请求URL
			String url = apiConfig.getUrl();
			if (url == null || url.isEmpty()) {
				// DashScope默认API端点
				url = "https://dashscope.aliyuncs.com/api/v1/services/knowledge-base/text-search";
			}

			// 发送请求
			ResponseEntity<Map> response = restClient.post()
				.uri(url)
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiConfig.getApiKey())
				.body(requestBody)
				.retrieve()
				.toEntity(Map.class);

			// 解析响应
			return parseResponse(response.getBody());

		}
		catch (Exception e) {
			logger.error("Error calling DashScope KB API", e);
			return Collections.emptyList();
		}
	}

	@Override
	public String getProvider() {
		return "dashscope";
	}

	@Override
	public boolean isAvailable() {
		return apiConfig != null && apiConfig.getApiKey() != null && !apiConfig.getApiKey().trim().isEmpty();
	}

	/**
	 * 解析DashScope API响应
	 */
	private List<KbSearchResult> parseResponse(Map<String, Object> responseBody) {
		List<KbSearchResult> results = new ArrayList<>();

		if (responseBody == null) {
			return results;
		}

		try {
			// DashScope响应格式解析
			Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
			if (output != null) {
				List<Map<String, Object>> nodes = (List<Map<String, Object>>) output.get("nodes");
				if (nodes != null) {
					for (Map<String, Object> node : nodes) {
						// 解析各个字段
						String id = (String) node.get("id");
						String title = (String) node.get("title");
						String content = (String) node.get("content");
						String url = (String) node.get("url");

						// 解析score
						Double score = null;
						Object scoreObj = node.get("score");
						if (scoreObj instanceof Number) {
							score = ((Number) scoreObj).doubleValue();
						}

						// 添加额外的元数据
						Map<String, Object> metadata = new HashMap<>();
						metadata.put("source", "dashscope");
						metadata.put("provider", getProvider());
						if (node.containsKey("metadata")) {
							metadata.putAll((Map<String, Object>) node.get("metadata"));
						}

						// 使用record的构造函数创建实例
						KbSearchResult result = new KbSearchResult(id, title, content, url, score, metadata);
						results.add(result);
					}
				}
			}
		}
		catch (Exception e) {
			logger.error("Error parsing DashScope response", e);
		}

		return results;
	}

}
