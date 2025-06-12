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

import com.alibaba.cloud.ai.analyticdb.AnalyticDbVectorStoreProperties;
import com.alibaba.cloud.ai.dbconnector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.request.*;
import com.aliyun.gpdb20160503.Client;
import com.aliyun.gpdb20160503.models.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.dbconnector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.dbconnector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.dbconnector.bo.TableInfoBO;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 核心向量数据库操作服务，提供向量写入、查询、删除、Schema 初始化等功能。
 */
@Service
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.analytic", name = "enabled", havingValue = "true",
		matchIfMissing = true)
public class AnalyticDbVectorStoreManagementService implements VectorStoreManagementService {

	private static final String CONTENT_FIELD_NAME = "content";

	private static final String METADATA_FIELD_NAME = "metadata";

	@Autowired
	@Qualifier("dashscopeEmbeddingModel")
	private EmbeddingModel embeddingModel;

	@Autowired
	private VectorStore vectorStore;

	@Autowired
	private DbAccessor dbAccessor;

	@Autowired
	private AnalyticDbVectorStoreProperties analyticDbVectorStoreProperties;

	@Autowired
	private Client client;

	@Autowired
	private Gson gson;

	/**
	 * 将证据内容添加到向量库中
	 * @param evidenceRequests 证据请求列表
	 * @return 是否成功
	 */
	@Override
	public Boolean addEvidence(List<EvidenceRequest> evidenceRequests) {
		List<Document> evidences = new ArrayList<>();
		for (EvidenceRequest req : evidenceRequests) {
			Document doc = new Document(UUID.randomUUID().toString(), req.getContent(),
					Map.of("evidenceType", req.getType(), "vectorType", "evidence"));
			evidences.add(doc);
		}
		vectorStore.add(evidences);
		return true;
	}

	/**
	 * 将文本嵌入为向量
	 * @param text 输入文本
	 * @return 向量化结果
	 */
	public List<Double> embed(String text) {
		float[] embedded = embeddingModel.embed(text);
		List<Double> result = new ArrayList<>();
		for (float value : embedded) {
			result.add((double) value);
		}
		return result;
	}

	/**
	 * 向量搜索
	 * @param searchRequest 查询请求
	 * @return 匹配的文档列表
	 */
	public List<Document> search(SearchRequest searchRequest) throws Exception {
		String filterTemplate = "jsonb_extract_path_text(metadata, 'vectorType') = '%s'";
		String filterFormatted = String.format(filterTemplate, searchRequest.getVectorType());

		QueryCollectionDataRequest request = new QueryCollectionDataRequest()
			.setDBInstanceId(analyticDbVectorStoreProperties.getDbInstanceId())
			.setRegionId(analyticDbVectorStoreProperties.getRegionId())
			.setNamespace(analyticDbVectorStoreProperties.getNamespace())
			.setNamespacePassword(analyticDbVectorStoreProperties.getNamespacePassword())
			.setCollection(analyticDbVectorStoreProperties.getCollectName())
			.setIncludeValues(false)
			.setMetrics(analyticDbVectorStoreProperties.getMetrics())
			.setVector(embed(searchRequest.getQuery()))
			.setContent(searchRequest.getQuery())
			.setTopK((long) searchRequest.getTopK())
			.setFilter(filterFormatted);

		try {
			QueryCollectionDataResponse response = this.client.queryCollectionData(request);
			List<Document> documents = new ArrayList<>();

			if (response.getBody() != null && response.getBody().getMatches() != null) {
				for (QueryCollectionDataResponseBody.QueryCollectionDataResponseBodyMatchesMatch match : response
					.getBody()
					.getMatches()
					.getMatch()) {
					if (match.getScore() != null && match.getScore() > 0.2) {
						Map<String, String> metadata = match.getMetadata();
						String pageContent = metadata.get(CONTENT_FIELD_NAME);
						Map<String, Object> metadataJson = new ObjectMapper()
							.readValue(metadata.get(METADATA_FIELD_NAME), new TypeReference<HashMap<String, Object>>() {
							});

						Document doc = new Document(match.getId(), pageContent, metadataJson);
						documents.add(doc);
					}
				}
			}

			return documents;
		}
		catch (Exception e) {
			throw new Exception("Failed to perform vector search: " + e.getMessage(), e);
		}
	}

	/**
	 * 删除指定条件的向量数据
	 * @param deleteRequest 删除请求
	 * @return 是否删除成功
	 */
	@Override
	public Boolean deleteDocuments(DeleteRequest deleteRequest) throws Exception {
		try {
			String filterExpression;
			if (deleteRequest.getId() != null && !deleteRequest.getId().isEmpty()) {
				filterExpression = String.format("id = '%s'", deleteRequest.getId());
			}
			else if (deleteRequest.getVectorType() != null && !deleteRequest.getVectorType().isEmpty()) {
				filterExpression = String.format("jsonb_extract_path_text(metadata, 'vectorType') = '%s'",
						deleteRequest.getVectorType());
			}
			else {
				throw new IllegalArgumentException("Either id or vectorType must be specified.");
			}

			DeleteCollectionDataRequest request = getDeleteCollectionDataRequest(filterExpression);
			DeleteCollectionDataResponse deleteCollectionDataResponse = this.client.deleteCollectionData(request);
			return true;
		}
		catch (Exception e) {
			throw new Exception("Failed to delete collection data by filterExpression: " + e.getMessage(), e);
		}
	}

	private DeleteCollectionDataRequest getDeleteCollectionDataRequest(String query) {
		return new DeleteCollectionDataRequest().setDBInstanceId(analyticDbVectorStoreProperties.getDbInstanceId())
			.setRegionId(analyticDbVectorStoreProperties.getRegionId())
			.setNamespace(analyticDbVectorStoreProperties.getNamespace())
			.setNamespacePassword(analyticDbVectorStoreProperties.getNamespacePassword())
			.setCollection(analyticDbVectorStoreProperties.getCollectName())
			.setCollectionData(null)
			.setCollectionDataFilter(query);
	}

	/**
	 * 初始化数据库 schema 到向量库
	 * @param schemaInitRequest schema 初始化请求
	 * @throws Exception 如果发生错误
	 */
	@Override
	public Boolean schema(SchemaInitRequest schemaInitRequest) throws Exception {
		DbConfig dbConfig = schemaInitRequest.getDbConfig();
		DbQueryParameter dqp = DbQueryParameter.from(dbConfig)
			.setSchema(dbConfig.getSchema())
			.setTables(schemaInitRequest.getTables());

		DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.setVectorType("column");
		deleteDocuments(deleteRequest);
		deleteRequest.setVectorType("table");
		deleteDocuments(deleteRequest);

		List<ForeignKeyInfoBO> foreignKeyInfoBOS = dbAccessor.showForeignKeys(dbConfig, dqp);
		Map<String, List<String>> foreignKeyMap = buildForeignKeyMap(foreignKeyInfoBOS);

		List<TableInfoBO> tableInfoBOS = dbAccessor.fetchTables(dbConfig, dqp);
		for (TableInfoBO tableInfoBO : tableInfoBOS) {
			processTable(tableInfoBO, dqp, dbConfig);
		}

		List<Document> columnDocuments = tableInfoBOS.stream().flatMap(table -> {
			try {
				return dbAccessor.showColumns(dbConfig, dqp).stream().map(column -> convertToDocument(table, column));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());

		vectorStore.add(columnDocuments);

		List<Document> tableDocuments = tableInfoBOS.stream()
			.map(this::convertTableToDocument)
			.collect(Collectors.toList());

		vectorStore.add(tableDocuments);

		return true;
	}

	private void processTable(TableInfoBO tableInfoBO, DbQueryParameter dqp, DbConfig dbConfig) throws Exception {
		dqp.setTable(tableInfoBO.getName());
		List<ColumnInfoBO> columnInfoBOS = dbAccessor.showColumns(dbConfig, dqp);
		for (ColumnInfoBO columnInfoBO : columnInfoBOS) {
			dqp.setColumn(columnInfoBO.getName());
			List<String> sampleColumn = dbAccessor.sampleColumn(dbConfig, dqp);
			sampleColumn = Optional.ofNullable(sampleColumn)
				.orElse(new ArrayList<>())
				.stream()
				.filter(Objects::nonNull)
				.distinct()
				.limit(3)
				.filter(s -> s.length() <= 100)
				.toList();

			columnInfoBO.setTableName(tableInfoBO.getName());
			columnInfoBO.setSamples(gson.toJson(sampleColumn));
		}

		ColumnInfoBO primaryColumnDO = columnInfoBOS.stream()
			.filter(ColumnInfoBO::isPrimary)
			.findFirst()
			.orElse(new ColumnInfoBO());

		tableInfoBO.setPrimaryKey(primaryColumnDO.getName());
		tableInfoBO.setForeignKey(String.join("、", buildForeignKeyList(tableInfoBO.getName())));
	}

	private Map<String, List<String>> buildForeignKeyMap(List<ForeignKeyInfoBO> foreignKeyInfoBOS) {
		Map<String, List<String>> foreignKeyMap = new HashMap<>();
		for (ForeignKeyInfoBO fk : foreignKeyInfoBOS) {
			String key = fk.getTable() + "." + fk.getColumn() + "=" + fk.getReferencedTable() + "."
					+ fk.getReferencedColumn();

			foreignKeyMap.computeIfAbsent(fk.getTable(), k -> new ArrayList<>()).add(key);
			foreignKeyMap.computeIfAbsent(fk.getReferencedTable(), k -> new ArrayList<>()).add(key);
		}
		return foreignKeyMap;
	}

	private List<String> buildForeignKeyList(String tableName) {
		return new ArrayList<>();
	}

	public Document convertToDocument(TableInfoBO tableInfoBO, ColumnInfoBO columnInfoBO) {
		String text = Optional.ofNullable(columnInfoBO.getDescription()).orElse(columnInfoBO.getName());
		Map<String, Object> metadata = Map.of("name", columnInfoBO.getName(), "tableName", tableInfoBO.getName(),
				"description", Optional.ofNullable(columnInfoBO.getDescription()).orElse(""), "type",
				columnInfoBO.getType(), "primary", columnInfoBO.isPrimary(), "notnull", columnInfoBO.isNotnull(),
				"vectorType", "column");
		if (columnInfoBO.getSamples() != null) {
			metadata.put("samples", columnInfoBO.getSamples());
		}
		return new Document(columnInfoBO.getName(), text, metadata);
	}

	public Document convertTableToDocument(TableInfoBO tableInfoBO) {
		String text = Optional.ofNullable(tableInfoBO.getDescription()).orElse(tableInfoBO.getName());
		Map<String, Object> metadata = Map.of("schema", Optional.ofNullable(tableInfoBO.getSchema()).orElse(""), "name",
				tableInfoBO.getName(), "description", Optional.ofNullable(tableInfoBO.getDescription()).orElse(""),
				"foreignKey", tableInfoBO.getForeignKey(), "primaryKey", tableInfoBO.getPrimaryKey(), "vectorType",
				"table");
		return new Document(tableInfoBO.getName(), text, metadata);
	}

}
