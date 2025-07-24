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

package com.alibaba.cloud.ai.example.deepresearch.rag.kb;

import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;
import com.alibaba.cloud.ai.example.deepresearch.rag.kb.impl.CustomKbApiClient;
import com.alibaba.cloud.ai.example.deepresearch.rag.kb.impl.DashScopeKbApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 专业知识库API客户端工厂 根据配置创建相应的API客户端
 *
 * @author hupei
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.alibaba.deepresearch.rag", name = "enabled", havingValue = "true")
public class ProfessionalKbApiClientFactory {

	private static final Logger logger = LoggerFactory.getLogger(ProfessionalKbApiClientFactory.class);

	private final RestClient restClient;

	public ProfessionalKbApiClientFactory(RestClient restClient) {
		this.restClient = restClient;
	}

	/**
	 * 根据知识库配置创建API客户端
	 * @param knowledgeBase 知识库配置
	 * @return API客户端，如果配置不支持则返回null
	 */
	public ProfessionalKbApiClient createClient(RagProperties.ProfessionalKnowledgeBases.KnowledgeBase knowledgeBase) {
		if (knowledgeBase == null || !knowledgeBase.isEnabled()) {
			return null;
		}

		// 只处理API类型的知识库
		if (!"api".equalsIgnoreCase(knowledgeBase.getType())) {
			logger.debug("Knowledge base {} is not API type, skipping", knowledgeBase.getId());
			return null;
		}

		RagProperties.ProfessionalKnowledgeBases.KnowledgeBase.Api apiConfig = knowledgeBase.getApi();
		if (apiConfig == null) {
			logger.warn("No API configuration found for knowledge base: {}", knowledgeBase.getId());
			return null;
		}

		String provider = apiConfig.getProvider();
		if (provider == null) {
			provider = "custom"; // 默认使用自定义客户端
		}

		try {
			switch (provider.toLowerCase()) {
				case "dashscope":
					logger.info("Creating DashScope API client for knowledge base: {}", knowledgeBase.getId());
					return new DashScopeKbApiClient(restClient, apiConfig);

				case "custom":
				default:
					logger.info("Creating custom API client for knowledge base: {}", knowledgeBase.getId());
					return new CustomKbApiClient(restClient, apiConfig);
			}
		}
		catch (Exception e) {
			logger.error("Failed to create API client for knowledge base: {}", knowledgeBase.getId(), e);
			return null;
		}
	}

	/**
	 * 批量创建所有启用的知识库的API客户端
	 * @param knowledgeBases 知识库配置列表
	 * @return 知识库ID到API客户端的映射
	 */
	public Map<String, ProfessionalKbApiClient> createClients(
			List<RagProperties.ProfessionalKnowledgeBases.KnowledgeBase> knowledgeBases) {

		java.util.Map<String, ProfessionalKbApiClient> clients = new java.util.HashMap<>();

		if (knowledgeBases == null || knowledgeBases.isEmpty()) {
			return clients;
		}

		for (RagProperties.ProfessionalKnowledgeBases.KnowledgeBase kb : knowledgeBases) {
			if (kb.isEnabled() && "api".equalsIgnoreCase(kb.getType())) {
				ProfessionalKbApiClient client = createClient(kb);
				if (client != null && client.isAvailable()) {
					clients.put(kb.getId(), client);
					logger.info("Successfully created API client for knowledge base: {} ({})", kb.getName(),
							kb.getId());
				}
				else {
					logger.warn("Failed to create or configure API client for knowledge base: {}", kb.getId());
				}
			}
		}

		logger.info("Created {} API clients for professional knowledge bases", clients.size());
		return clients;
	}

}
