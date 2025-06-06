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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.vectorstore.analyticdb.AnalyticDbVectorStoreProperties;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.aliyun.gpdb20160503.Client;
import com.aliyun.gpdb20160503.models.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class VectorStoreService {

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

	/**
	 * 将文本转换为 Double 类型的向量
	 */
	public List<Double> embedDouble(String text) {
		return convertToDoubleList(embeddingModel.embed(text));
	}

	/**
	 * 将文本转换为 Float 类型的向量
	 */
	public List<Float> embedFloat(String text) {
		return convertToFloatList(embeddingModel.embed(text));
	}

	/**
	 * 获取向量库中的文档
	 */
	public List<Document> getDocuments(String query, String vectorType) {
		SearchRequest request = new SearchRequest();
		request.setQuery(query);
		request.setVectorType(vectorType);
		request.setTopK(100);
		return searchWithVectorType(request);
	}

	/**
	 * 默认 filter 的搜索接口
	 */
	public List<Document> searchWithVectorType(SearchRequest searchRequestDTO) {
		String filter = String.format("jsonb_extract_path_text(metadata, 'vectorType') = '%s'",
				searchRequestDTO.getVectorType());

		QueryCollectionDataRequest request = buildBaseRequest(searchRequestDTO).setFilter(filter);

		return executeQuery(request);
	}

	/**
	 * 自定义 filter 的搜索接口
	 */
	public List<Document> searchWithFilter(SearchRequest searchRequestDTO) {
		QueryCollectionDataRequest request = buildBaseRequest(searchRequestDTO)
			.setFilter(searchRequestDTO.getFilterFormatted());
		return executeQuery(request);
	}

	/**
	 * 构建基础查询请求对象
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
	 * 执行实际查询并解析结果
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
	 * 将 float[] 转换为 List<Double>
	 */
	private List<Double> convertToDoubleList(float[] array) {
		return IntStream.range(0, array.length)
			.mapToDouble(i -> (double) array[i])
			.boxed()
			.collect(Collectors.toList());
	}

	/**
	 * 将 float[] 转换为 List<Float>
	 */
	private List<Float> convertToFloatList(float[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}

	/**
	 * 解析响应数据为 Document 列表
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
