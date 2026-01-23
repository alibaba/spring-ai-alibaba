/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.rag.bailian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.bailian20231229.Client;
import com.aliyun.bailian20231229.models.RetrieveRequest;
import com.aliyun.bailian20231229.models.RetrieveResponse;
import com.aliyun.tea.utils.StringUtils;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * Client for interacting with Alibaba Cloud Bailian Knowledge Base API.
 *
 * <p>This class wraps the Bailian SDK and provides reactive API methods
 * for knowledge base operations. It handles client initialization, request
 * construction, and error handling.
 *
 * <p>Example usage:
 * <pre>{@code
 * BailianConfig config = BailianConfig.builder()
 *     .accessKeyId(...)
 *     .accessKeySecret(...)
 *     .workspaceId(...)
 *     .build();
 *
 * BailianClient client = new BailianClient(config);
 * RetrieveResponse response = client.retrieve("indexId", "query").block();
 * }</pre>
 */
public class BailianClient {

	private static final Logger log = LoggerFactory.getLogger(BailianClient.class);

	private final Client sdkClient;
	private final String workspaceId;
	private final BailianConfig config;

	/**
	 * Creates a new BailianClient instance.
	 *
	 * @param config the Bailian configuration
	 * @throws Exception if client initialization fails
	 */
	public BailianClient(BailianConfig config) throws Exception {
		if (config == null) {
			throw new IllegalArgumentException("BailianConfig cannot be null");
		}

		this.config = config;
		this.workspaceId = config.getWorkspaceId();
		this.sdkClient = createSdkClient(config);

		log.info(
				"BailianClient initialized for workspace: {} at endpoint: {}",
				workspaceId,
				config.getEndpoint());
	}

	/**
	 * Package-private constructor for testing purposes.
	 *
	 * <p>This constructor allows injecting a mock SDK client for unit testing
	 * without requiring actual Alibaba Cloud credentials.
	 *
	 * @param sdkClient the Bailian SDK client
	 * @param workspaceId the workspace ID
	 * @param config the Bailian configuration (can be null for testing)
	 */
	BailianClient(Client sdkClient, String workspaceId, BailianConfig config) {
		if (sdkClient == null) {
			throw new IllegalArgumentException("SDK Client cannot be null");
		}
		if (workspaceId == null || workspaceId.trim().isEmpty()) {
			throw new IllegalArgumentException("WorkspaceId cannot be null or empty");
		}

		this.sdkClient = sdkClient;
		this.workspaceId = workspaceId;
		this.config = config;

		log.info("BailianClient initialized for testing with workspace: {}", workspaceId);
	}

	/**
	 * Retrieves relevant documents from a knowledge base with a limit.
	 *
	 * <p>This method searches the specified knowledge base (index) for documents
	 * relevant to the given query. Results are returned sorted by relevance score,
	 * limited to the specified number of documents.
	 *
	 * @param indexId the knowledge base ID to search in
	 * @param query the search query text
	 * @param limit the maximum number of documents to retrieve (null for default)
	 * @return a Mono emitting the retrieve response
	 */
	public Mono<RetrieveResponse> retrieve(String indexId, String query, Integer limit) {
		return retrieve(indexId, query, limit, null);
	}

	/**
	 * Retrieves relevant documents from a knowledge base with conversation history.
	 *
	 * <p>This method searches the specified knowledge base (index) for documents
	 * relevant to the given query. If conversation history is provided and query rewrite
	 * is enabled in the configuration, the query will be rewritten based on the
	 * conversation context.
	 *
	 * <p>Retrieval parameters (denseSimilarityTopK, sparseSimilarityTopK, reranking, etc.)
	 * are read from the BailianConfig provided during client construction.
	 *
	 * @param indexId the knowledge base ID to search in
	 * @param query the search query text
	 * @param limit the maximum number of documents to retrieve (null for default)
	 * @param conversationHistory the conversation history for multi-turn rewrite (can be null)
	 * @return a Mono emitting the retrieve response
	 */
	public Mono<RetrieveResponse> retrieve(
			String indexId,
			String query,
			Integer limit,
			List<QueryHistoryEntry> conversationHistory) {
		if (indexId == null || indexId.trim().isEmpty()) {
			return Mono.error(new IllegalArgumentException("IndexId cannot be null or empty"));
		}
		if (query == null || query.trim().isEmpty()) {
			return Mono.error(new IllegalArgumentException("Query cannot be null or empty"));
		}

		return Mono.fromCallable(
				() -> {
					log.debug(
							"Retrieving from index: {} with query: {} (limit: {}, historySize: {})",
							indexId,
							query,
							limit,
							conversationHistory != null ? conversationHistory.size() : 0);

					// Build retrieve request with all parameters from config
					RetrieveRequest request = new RetrieveRequest();
					request.setIndexId(indexId);
					request.setQuery(query);

					// Set dense and sparse similarity top K from config or limit parameter
					if (config.getDenseSimilarityTopK() != null) {
						request.setDenseSimilarityTopK(config.getDenseSimilarityTopK());
					}
					else if (limit != null && limit > 0) {
						request.setDenseSimilarityTopK(limit);
					}

					if (config.getSparseSimilarityTopK() != null) {
						request.setSparseSimilarityTopK(config.getSparseSimilarityTopK());
					}

					// Set reranking from config
					if (config.getEnableReranking() != null) {
						request.setEnableReranking(config.getEnableReranking());
					}
					if (config.getRerankConfig() != null) {
						RerankConfig rerankConfig = config.getRerankConfig();
						List<RetrieveRequest.RetrieveRequestRerank> rerankList = new ArrayList<>();
						RetrieveRequest.RetrieveRequestRerank rerank =
								new RetrieveRequest.RetrieveRequestRerank();

						if (!StringUtils.isEmpty(rerankConfig.getModelName())) {
							rerank.setModelName(rerankConfig.getModelName());
						}

						if (rerankConfig.getRerankMinScore() != null) {
							request.setRerankMinScore(rerankConfig.getRerankMinScore());
						}

						if (rerankConfig.getRerankTopN() != null) {
							request.setRerankTopN(rerankConfig.getRerankTopN());
						}

						rerankList.add(rerank);
						request.setRerank(rerankList);
					}

					// Set multi-turn dialogue rewrite from config
					if (config.getEnableRewrite() != null) {
						request.setEnableRewrite(config.getEnableRewrite());
					}
					if (config.getRewriteConfig() != null) {
						RewriteConfig rewriteConfig = config.getRewriteConfig();
						List<RetrieveRequest.RetrieveRequestRewrite> rewriteList =
								new ArrayList<>();
						RetrieveRequest.RetrieveRequestRewrite rewrite =
								new RetrieveRequest.RetrieveRequestRewrite();

						if (!StringUtils.isEmpty(rewriteConfig.getModelName())) {
							rewrite.setModelName(rewriteConfig.getModelName());
						}

						rewriteList.add(rewrite);
						request.setRewrite(rewriteList);
					}

					// Set query history for multi-turn rewrite (from parameter)
					if (conversationHistory != null && !conversationHistory.isEmpty()) {
						List<RetrieveRequest.RetrieveRequestQueryHistory> historyList =
								new ArrayList<>();
						for (QueryHistoryEntry entry : conversationHistory) {
							RetrieveRequest.RetrieveRequestQueryHistory historyEntry =
									new RetrieveRequest.RetrieveRequestQueryHistory();
							historyEntry.setRole(entry.getRole());
							historyEntry.setContent(entry.getContent());
							historyList.add(historyEntry);
						}
						request.setQueryHistory(historyList);
					}

					// Set search filters from config
					if (config.getSearchFilters() != null && !config.getSearchFilters().isEmpty()) {
						List<Map<String, String>> filters = config.getSearchFilters();
						List<Map<String, String>> requestFilters = new ArrayList<>(filters);
						request.setSearchFilters(requestFilters);
					}

					// Set save retriever history from config
					if (config.getSaveRetrieverHistory() != null) {
						request.setSaveRetrieverHistory(config.getSaveRetrieverHistory());
					}

					// Execute retrieve request
					RuntimeOptions runtime = new RuntimeOptions();
					RetrieveResponse response =
							sdkClient.retrieveWithOptions(
									workspaceId, request, new HashMap<>(), runtime);

					if (response == null || response.getBody() == null) {
						throw new RuntimeException("Bailian API returned null response");
					}

					if (response.getBody().getData() == null) {
						log.warn(
								"Bailian API returned no data for query: {}. requestId={},statu={},"
										+ " code={}, message={}",
								query,
								response.getBody().getRequestId(),
								response.getBody().getStatus(),
								response.getBody().getCode(),
								response.getBody().getMessage());
					}

					log.debug(
							"Retrieved {} documents from index: {}",
							response.getBody().getData() != null
									&& response.getBody().getData().getNodes() != null
									? response.getBody().getData().getNodes().size()
									: 0,
							indexId);

					return response;
				});
	}

	/**
	 * Gets the workspace ID associated with this client.
	 *
	 * @return the workspace ID
	 */
	public String getWorkspaceId() {
		return workspaceId;
	}

	/**
	 * Creates and configures the Bailian SDK client.
	 *
	 * @param config the Bailian configuration
	 * @return a configured Client instance
	 * @throws Exception if client creation fails due to invalid credentials (invalid access key
	 *     ID or secret), network connectivity issues, SDK initialization errors, or invalid
	 *     endpoint configuration
	 */
	private Client createSdkClient(BailianConfig config) throws Exception {
		Config sdkConfig = new Config();
		sdkConfig.setAccessKeyId(config.getAccessKeyId());
		sdkConfig.setAccessKeySecret(config.getAccessKeySecret());
		sdkConfig.setEndpoint(config.getEndpoint());

		return new Client(sdkConfig);
	}
}
