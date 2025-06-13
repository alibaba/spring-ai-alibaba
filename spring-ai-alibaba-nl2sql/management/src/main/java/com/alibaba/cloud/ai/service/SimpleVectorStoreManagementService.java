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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.dbconnector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.dbconnector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.dbconnector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.dbconnector.bo.TableInfoBO;
import com.alibaba.cloud.ai.request.DeleteRequest;
import com.alibaba.cloud.ai.request.EvidenceRequest;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.google.gson.Gson;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimpleVectorStoreManagementService implements VectorStoreManagementService {

	private final SimpleVectorStore vectorStore;

	private final Gson gson;

	private final DbAccessor dbAccessor;

	private final DbConfig dbConfig;

	@Autowired
	public SimpleVectorStoreManagementService(@Value("${spring.ai.dashscope.api-key:default_api_key}") String apiKey,
			Gson gson, DbAccessor dbAccessor, DbConfig dbConfig) {
		this.gson = gson;
		this.dbAccessor = dbAccessor;
		this.dbConfig = dbConfig;

		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
		DashScopeEmbeddingModel dashScopeEmbeddingModel = new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED,
				DashScopeEmbeddingOptions.builder().withModel("text-embedding-v2").build());
		this.vectorStore = SimpleVectorStore.builder(dashScopeEmbeddingModel).build();
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

	private List<String> buildForeignKeyList(String tableName) {
		return new ArrayList<>();
	}

	public Document convertTableToDocument(TableInfoBO tableInfoBO) {
		String text = Optional.ofNullable(tableInfoBO.getDescription()).orElse(tableInfoBO.getName());
		Map<String, Object> metadata = Map.of("schema", Optional.ofNullable(tableInfoBO.getSchema()).orElse(""), "name",
				tableInfoBO.getName(), "description", Optional.ofNullable(tableInfoBO.getDescription()).orElse(""),
				"foreignKey", tableInfoBO.getForeignKey(), "primaryKey", tableInfoBO.getPrimaryKey(), "vectorType",
				"table");
		return new Document(tableInfoBO.getName(), text, metadata);
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

	/**
	 * 删除指定条件的向量数据
	 * @param deleteRequest 删除请求
	 * @return 是否删除成功
	 */
	@Override
	public Boolean deleteDocuments(DeleteRequest deleteRequest) throws Exception {
		try {
			if (deleteRequest.getId() != null && !deleteRequest.getId().isEmpty()) {
				vectorStore.delete(Arrays.asList(deleteRequest.getId()));
			}
			else if (deleteRequest.getVectorType() != null && !deleteRequest.getVectorType().isEmpty()) {
				FilterExpressionBuilder b = new FilterExpressionBuilder();
				Filter.Expression expression = b.eq("vectorType", "column").build();
				List<Document> documents = vectorStore.similaritySearch(
						SearchRequest.builder().topK(Integer.MAX_VALUE).filterExpression(expression).build());
				vectorStore.delete(documents.stream().map(Document::getId).toList());
			}
			else {
				throw new IllegalArgumentException("Either id or vectorType must be specified.");
			}
			return true;
		}
		catch (Exception e) {
			throw new Exception("Failed to delete collection data by filterExpression: " + e.getMessage(), e);
		}
	}

	/**
	 * 根据搜索请求在向量库中检索文档
	 * @param searchRequest 搜索请求
	 * @return 匹配的文档列表
	 * @throws Exception 检索异常
	 */
	public List<Document> search(SearchRequest searchRequest) throws Exception {
		try {
			return vectorStore.similaritySearch(searchRequest)
				.stream()
				.filter(document -> document.getScore() > 0.2)
				.toList();
		}
		catch (Exception e) {
			throw new Exception("Failed to search vector store: " + e.getMessage(), e);
		}
	}

}
