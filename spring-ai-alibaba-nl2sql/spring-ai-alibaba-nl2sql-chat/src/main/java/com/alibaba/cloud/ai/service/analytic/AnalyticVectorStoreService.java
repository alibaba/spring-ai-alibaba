/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.service.analytic;

import com.alibaba.cloud.ai.annotation.ConditionalOnADBEnabled;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.alibaba.cloud.ai.vectorstore.analyticdb.AnalyticDbVectorStoreProperties;
import com.aliyun.gpdb20160503.Client;
import com.aliyun.gpdb20160503.models.QueryCollectionDataRequest;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnADBEnabled
public class AnalyticVectorStoreService extends BaseVectorStoreService {

	private static final String CONTENT_FIELD_NAME = "content";

	private static final String METADATA_FIELD_NAME = "metadata";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	@Qualifier("dashscopeEmbeddingModel")
	private EmbeddingModel embeddingModel;

	@Autowired
	private AnalyticDbVectorStoreProperties analyticDbVectorStoreProperties;

	@Autowired
	private Client client;

	@Override
	protected EmbeddingModel getEmbeddingModel() {
		return embeddingModel;
	}

	/**
	 * Search interface with default filter
	 */
	@Override
	public List<Document> searchWithVectorType(SearchRequest searchRequestDTO) {
		String filter = String.format("jsonb_extract_path_text(metadata, 'vectorType') = '%s'",
				searchRequestDTO.getVectorType());

		QueryCollectionDataRequest request = buildBaseRequest(searchRequestDTO).setFilter(filter);

		return executeQuery(request);
	}

	/**
	 * Search interface with custom filter
	 */
	@Override
	public List<Document> searchWithFilter(SearchRequest searchRequestDTO) {
		QueryCollectionDataRequest request = buildBaseRequest(searchRequestDTO)
			.setFilter(searchRequestDTO.getFilterFormatted());
		return executeQuery(request);
	}

	/**
	 * Build basic query request object
	 */
	private QueryCollectionDataRequest buildBaseRequest(SearchRequest searchRequestDTO) {
		QueryCollectionDataRequest queryCollectionDataRequest = new QueryCollectionDataRequest()
			.setDBInstanceId(analyticDbVectorStoreProperties.getDbInstanceId())
			.setRegionId(analyticDbVectorStoreProperties.getRegionId())
			.setNamespace(analyticDbVectorStoreProperties.getNamespace())
			.setNamespacePassword(analyticDbVectorStoreProperties.getNamespacePassword())
			.setCollection(analyticDbVectorStoreProperties.getCollectName())
			.setIncludeValues(false)
			.setMetrics(analyticDbVectorStoreProperties.getMetrics())
			.setTopK((long) searchRequestDTO.getTopK());
		if (searchRequestDTO.getQuery() != null) {
			queryCollectionDataRequest.setVector(embedDouble(searchRequestDTO.getQuery()));
			queryCollectionDataRequest.setContent(searchRequestDTO.getQuery());
		}
		return queryCollectionDataRequest;
	}

	/**
	 * Execute actual query and parse results
	 */
	private List<Document> executeQuery(QueryCollectionDataRequest request) {
		try {
			QueryCollectionDataResponse response = client.queryCollectionData(request);
			return parseDocuments(response);
		}
		catch (Exception e) {
			throw new RuntimeException("向量数据库查询失败: " + e.getMessage(), e);
		}
	}

	/**
	 * Parse response data into Document list
	 */
	private List<Document> parseDocuments(QueryCollectionDataResponse response) throws Exception {
		return response.getBody()
			.getMatches()
			.getMatch()
			.stream()
			.filter(match -> match.getScore() == null || match.getScore() > 0.1 || match.getScore() == 0.0)
			.map(match -> {
				Map<String, String> metadata = match.getMetadata();
				try {
					Map<String, Object> metadataJson = OBJECT_MAPPER.readValue(metadata.get(METADATA_FIELD_NAME),
							new TypeReference<HashMap<String, Object>>() {
							});
					metadataJson.put("score", match.getScore());

					return new Document(match.getId(), metadata.get(CONTENT_FIELD_NAME), metadataJson);
				}
				catch (Exception e) {
					throw new RuntimeException("解析元数据失败: " + e.getMessage(), e);
				}
			})
			.collect(Collectors.toList());
	}

}
