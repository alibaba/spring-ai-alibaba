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
 * Professional Knowledge Base API strategy, supporting multiple API providers.
 * Utilizes encapsulated SDKs for API calls and allows for expansion to additional professional knowledge bases.
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

		// Initializing the API client
		this.apiClients = initializeApiClients();
	}

	@Override
	public String getStrategyName() {
		return "professionalKbApi";
	}

	@Override
	public List<Document> retrieve(String query, Map<String, Object> options) {
		try {
			// 1. Performing query pre-processing (query expansion, translation, etc.)
			Query ragQuery = new Query(query);
			List<Query> processedQueries = hybridRagProcessor.preProcess(ragQuery, options);

			List<Document> allDocuments = new ArrayList<>();

			// 2. Retrieving the list of selected knowledge bases
			List<String> selectedKbIds = getSelectedKnowledgeBaseIds(options);

			// 3. Searching for each processed query and selected knowledge base
			for (Query processedQuery : processedQueries) {
				for (String kbId : selectedKbIds) {
					List<Document> kbDocuments = searchKnowledgeBase(kbId, processedQuery.text(), options);
					allDocuments.addAll(kbDocuments);
				}
			}

			// 4. Performing document post-processing (deduplication, sorting, etc.)
			return hybridRagProcessor.postProcess(allDocuments, options);

		}
		catch (Exception e) {
			logger.error("Error in professional KB API strategy", e);
			return Collections.emptyList();
		}
	}

	/**
	 * Initializes all API clients
	 */
	private Map<String, ProfessionalKbApiClient> initializeApiClients() {
		List<RagProperties.ProfessionalKnowledgeBases.KnowledgeBase> knowledgeBases = ragProperties
				.getProfessionalKnowledgeBases()
				.getKnowledgeBases();

		return clientFactory.createClients(knowledgeBases);
	}

	/**
	 * Retrieves the list of selected knowledge base IDs
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

		// If none specified, use all available API-type knowledge bases
		return apiClients.keySet().stream().sorted().collect(Collectors.toList());
	}

	/**
	 * Searches the specified knowledge base
	 */
	private List<Document> searchKnowledgeBase(String kbId, String query, Map<String, Object> options) {
		ProfessionalKbApiClient client = apiClients.get(kbId);
		if (client == null || !client.isAvailable()) {
			logger.warn("API client not available for knowledge base: {}", kbId);
			return Collections.emptyList();
		}

		try {
			// Prepare search options
			Map<String, Object> searchOptions = new HashMap<>(options);

			// Retrieve max results from knowledge base configuration
			RagProperties.ProfessionalKnowledgeBases.KnowledgeBase kbConfig = findKnowledgeBaseConfig(kbId);
			if (kbConfig != null && kbConfig.getApi() != null) {
				searchOptions.put("maxResults", kbConfig.getApi().getMaxResults());
			}

			// Call API to perform search
			List<KbSearchResult> searchResults = client.search(query, searchOptions);

			// Convert to Document format
			return convertToDocuments(searchResults, kbId, kbConfig);
		}
		catch (Exception e) {
			logger.error("Error searching knowledge base: {}", kbId, e);
			return Collections.emptyList();
		}
	}

	/**
	 * Finds the knowledge base configuration
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
	 * Converts API search results to Document format, maintaining consistency with VectorStoreDataIngestionService metadata logic
	 */
	private List<Document> convertToDocuments(List<KbSearchResult> searchResults, String kbId,
											  RagProperties.ProfessionalKnowledgeBases.KnowledgeBase kbConfig) {
		return searchResults.stream().map(result -> {
			// Build document content
			StringBuilder contentBuilder = new StringBuilder();
			if (result.title() != null && !result.title().trim().isEmpty()) {
				contentBuilder.append("Title: ").append(result.title()).append("\n\n");
			}
			if (result.content() != null) {
				contentBuilder.append(result.content());
			}

			// Build metadata consistent with VectorStoreDataIngestionService logic
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

			// Add search-related metadata
			if (result.score() != null) {
				metadata.put("search_score", result.score());
			}

			// Include original metadata
			if (result.metadata() != null) {
				metadata.putAll(result.metadata());
			}

			return new Document(result.id(), contentBuilder.toString(), metadata);
		}).collect(Collectors.toList());
	}

	/**
	 * Retrieves the list of available knowledge bases (for monitoring and debugging)
	 */
	public Set<String> getAvailableKnowledgeBases() {
		return apiClients.keySet();
	}

	/**
	 * Reinitializes API clients (for reloading after configuration updates)
	 */
	public void reinitializeClients() {
		logger.info("Reinitializing professional KB API clients");
		this.apiClients.clear();
		this.apiClients.putAll(initializeApiClients());
	}

}
