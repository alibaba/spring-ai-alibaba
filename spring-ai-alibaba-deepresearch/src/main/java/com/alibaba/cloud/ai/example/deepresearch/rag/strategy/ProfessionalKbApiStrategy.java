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
package com.alibaba.cloud.ai.example.deepresearch.rag.strategy;

import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;
import com.alibaba.cloud.ai.example.deepresearch.rag.SourceTypeEnum;
import com.alibaba.cloud.ai.example.deepresearch.rag.core.HybridRagProcessor;
import com.alibaba.cloud.ai.example.deepresearch.rag.kb.ProfessionalKbApiClient;
import com.alibaba.cloud.ai.example.deepresearch.rag.kb.ProfessionalKbApiClientFactory;
import com.alibaba.cloud.ai.example.deepresearch.rag.kb.model.KbSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 专业知识库API策略，支持多种API提供商 使用封装的SDK进行API调用，支持扩展更多专业知识库
 *
 * @author hupei
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.alibaba.deepresearch.rag", name = "enabled", havingValue = "true")
public class ProfessionalKbApiStrategy implements RetrievalStrategy {

	private static final Logger logger = LoggerFactory.getLogger(ProfessionalKbApiStrategy.class);

	private final HybridRagProcessor hybridRagProcessor;

	private final ProfessionalKbApiClientFactory clientFactory;

	private final RagProperties ragProperties;

	private final Map<String, ProfessionalKbApiClient> apiClients;

	public ProfessionalKbApiStrategy(HybridRagProcessor hybridRagProcessor,
			ProfessionalKbApiClientFactory clientFactory, RagProperties ragProperties) {
		this.hybridRagProcessor = hybridRagProcessor;
		this.clientFactory = clientFactory;
		this.ragProperties = ragProperties;

		// 初始化API客户端
		this.apiClients = initializeApiClients();
	}

	@Override
	public String getStrategyName() {
		return "professionalKbApi";
	}

	@Override
	public List<Document> retrieve(String query, Map<String, Object> options) {
		try {
			// 1. 查询前处理（查询扩展、翻译等）
			Query ragQuery = new Query(query);
			List<Query> processedQueries = hybridRagProcessor.preProcess(ragQuery, options);

			List<Document> allDocuments = new ArrayList<>();

			// 2. 获取选中的知识库列表
			List<String> selectedKbIds = getSelectedKnowledgeBaseIds(options);

			// 3. 对每个处理后的查询和选中的知识库进行搜索
			for (Query processedQuery : processedQueries) {
				for (String kbId : selectedKbIds) {
					List<Document> kbDocuments = searchKnowledgeBase(kbId, processedQuery.text(), options);
					allDocuments.addAll(kbDocuments);
				}
			}

			// 4. 文档后处理（去重、排序等）
			return hybridRagProcessor.postProcess(allDocuments, options);

		}
		catch (Exception e) {
			logger.error("Error in professional KB API strategy", e);
			return Collections.emptyList();
		}
	}

	/**
	 * 初始化所有API客户端
	 */
	private Map<String, ProfessionalKbApiClient> initializeApiClients() {
		List<RagProperties.ProfessionalKnowledgeBases.KnowledgeBase> knowledgeBases = ragProperties
			.getProfessionalKnowledgeBases()
			.getKnowledgeBases();

		return clientFactory.createClients(knowledgeBases);
	}

	/**
	 * 获取选中的知识库ID列表
	 */
	@SuppressWarnings("unchecked")
	private List<String> getSelectedKnowledgeBaseIds(Map<String, Object> options) {
		Object selectedKbs = options.get("selected_knowledge_bases");

		if (selectedKbs instanceof List) {
			return ((List<?>) selectedKbs).stream()
				.filter(Objects::nonNull)
				.map(Object::toString)
				.filter(id -> apiClients.containsKey(id))
				.collect(Collectors.toList());
		}

		// 如果没有指定，使用所有可用的API类型知识库
		return apiClients.keySet().stream().sorted().collect(Collectors.toList());
	}

	/**
	 * 搜索指定的知识库
	 */
	private List<Document> searchKnowledgeBase(String kbId, String query, Map<String, Object> options) {
		ProfessionalKbApiClient client = apiClients.get(kbId);
		if (client == null || !client.isAvailable()) {
			logger.warn("API client not available for knowledge base: {}", kbId);
			return Collections.emptyList();
		}

		try {
			// 准备搜索选项
			Map<String, Object> searchOptions = new HashMap<>(options);

			// 从知识库配置中获取最大结果数
			RagProperties.ProfessionalKnowledgeBases.KnowledgeBase kbConfig = findKnowledgeBaseConfig(kbId);
			if (kbConfig != null && kbConfig.getApi() != null) {
				searchOptions.put("maxResults", kbConfig.getApi().getMaxResults());
			}

			// 调用API进行搜索
			List<KbSearchResult> searchResults = client.search(query, searchOptions);

			// 转换为Document格式
			return convertToDocuments(searchResults, kbId, kbConfig);

		}
		catch (Exception e) {
			logger.error("Error searching knowledge base: {}", kbId, e);
			return Collections.emptyList();
		}
	}

	/**
	 * 查找知识库配置
	 */
	private RagProperties.ProfessionalKnowledgeBases.KnowledgeBase findKnowledgeBaseConfig(String kbId) {
		return ragProperties.getProfessionalKnowledgeBases()
			.getKnowledgeBases()
			.stream()
			.filter(kb -> kbId.equals(kb.getId()))
			.findFirst()
			.orElse(null);
	}

	/**
	 * 将API搜索结果转换为Document格式，保持与VectorStoreDataIngestionService元数据逻辑一致
	 */
	private List<Document> convertToDocuments(List<KbSearchResult> searchResults, String kbId,
			RagProperties.ProfessionalKnowledgeBases.KnowledgeBase kbConfig) {
		return searchResults.stream().map(result -> {
			// 构建文档内容
			StringBuilder contentBuilder = new StringBuilder();
			if (result.title() != null && !result.title().trim().isEmpty()) {
				contentBuilder.append("Title: ").append(result.title()).append("\n\n");
			}
			if (result.content() != null) {
				contentBuilder.append(result.content());
			}

			// 构建元数据，与VectorStoreDataIngestionService的逻辑保持一致
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("source_type", SourceTypeEnum.PROFESSIONAL_KB_API.getValue());
			metadata.put("kb_id", kbId);

			if (kbConfig != null) {
				metadata.put("kb_name", kbConfig.getName());
				metadata.put("kb_description", kbConfig.getDescription());
			}

			metadata.put("original_filename", result.title());
			metadata.put("source_url", result.url());
			metadata.put("title", result.title());
			metadata.put("api_provider", apiClients.get(kbId).getProvider());

			// 添加搜索相关的元数据
			if (result.score() != null) {
				metadata.put("search_score", result.score());
			}

			// 包含原始元数据
			if (result.metadata() != null) {
				metadata.putAll(result.metadata());
			}

			return new Document(result.id(), contentBuilder.toString(), metadata);
		}).collect(Collectors.toList());
	}

	/**
	 * 获取可用的知识库列表（用于监控和调试）
	 */
	public Set<String> getAvailableKnowledgeBases() {
		return apiClients.keySet();
	}

	/**
	 * 重新初始化API客户端（用于配置更新后的重新加载）
	 */
	public void reinitializeClients() {
		logger.info("Reinitializing professional KB API clients");
		this.apiClients.clear();
		this.apiClients.putAll(initializeApiClients());
	}

}
