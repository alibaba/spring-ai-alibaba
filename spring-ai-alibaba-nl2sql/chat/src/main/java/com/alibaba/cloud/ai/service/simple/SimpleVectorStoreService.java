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
package com.alibaba.cloud.ai.service.simple;

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
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.google.gson.Gson;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
public class SimpleVectorStoreService extends BaseVectorStoreService {

	private final SimpleVectorStore vectorStore;

	private final Gson gson;

	private final DbAccessor dbAccessor;

	private final DbConfig dbConfig;

	private DashScopeEmbeddingModel embeddingModel;

	@Autowired
	public SimpleVectorStoreService(@Value("${spring.ai.dashscope.api-key:default_api_key}") String apiKey, Gson gson,
			DbAccessor dbAccessor, DbConfig dbConfig) {
		this.gson = gson;
		this.dbAccessor = dbAccessor;
		this.dbConfig = dbConfig;

		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
		embeddingModel = new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED,
				DashScopeEmbeddingOptions.builder().withModel("text-embedding-v2").build());
		this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
	}

	@Override
	protected EmbeddingModel getEmbeddingModel() {
		return embeddingModel;
	}

	/**
	 * 初始化数据库 schema 到向量库
	 * @param schemaInitRequest schema 初始化请求
	 * @throws Exception 如果发生错误
	 */
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
			processTable(tableInfoBO, dqp, dbConfig, foreignKeyMap);
		}

		List<Document> columnDocuments = tableInfoBOS.stream().flatMap(table -> {
			try {
				dqp.setTable(table.getName());
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

	private void processTable(TableInfoBO tableInfoBO, DbQueryParameter dqp, DbConfig dbConfig,
			Map<String, List<String>> foreignKeyMap) throws Exception {
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
		tableInfoBO
			.setForeignKey(String.join("、", foreignKeyMap.getOrDefault(tableInfoBO.getName(), new ArrayList<>())));
	}

	public Document convertToDocument(TableInfoBO tableInfoBO, ColumnInfoBO columnInfoBO) {
		String text = Optional.ofNullable(columnInfoBO.getDescription()).orElse(columnInfoBO.getName());
		String id = tableInfoBO.getName() + "." + columnInfoBO.getName();
		Map<String, Object> metadata = Map.of("id", id, "name", columnInfoBO.getName(), "tableName",
				tableInfoBO.getName(), "description", Optional.ofNullable(columnInfoBO.getDescription()).orElse(""),
				"type", columnInfoBO.getType(), "primary", columnInfoBO.isPrimary(), "notnull",
				columnInfoBO.isNotnull(), "vectorType", "column");
		if (columnInfoBO.getSamples() != null) {
			metadata.put("samples", columnInfoBO.getSamples());
		}
		// 多表重复字段数据会被去重，采用表名+字段名作为唯一标识
		return new Document(id, text, metadata);
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
	public Boolean deleteDocuments(DeleteRequest deleteRequest) throws Exception {
		try {
			if (deleteRequest.getId() != null && !deleteRequest.getId().isEmpty()) {
				vectorStore.delete(Arrays.asList("comment_count"));
			}
			else if (deleteRequest.getVectorType() != null && !deleteRequest.getVectorType().isEmpty()) {
				FilterExpressionBuilder b = new FilterExpressionBuilder();
				Filter.Expression expression = b.eq("vectorType", "column").build();
				List<Document> documents = vectorStore
					.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder()
						.topK(Integer.MAX_VALUE)
						.filterExpression(expression)
						.build());
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
	 * 默认 filter 的搜索接口
	 */
	@Override
	public List<Document> searchWithVectorType(SearchRequest searchRequestDTO) {
		FilterExpressionBuilder b = new FilterExpressionBuilder();
		Filter.Expression expression = b.eq("vectorType", searchRequestDTO.getVectorType()).build();

		return vectorStore.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder()
			.query(searchRequestDTO.getQuery())
			.topK(searchRequestDTO.getTopK())
			.filterExpression(expression)
			.build());
	}

	/**
	 * 自定义 filter 的搜索接口
	 */
	@Override
	public List<Document> searchWithFilter(SearchRequest searchRequestDTO) {
		// 这里需要根据实际情况解析 filterFormatted 字段，转换为 FilterExpressionBuilder 的表达式
		// 简化实现，仅作示例
		FilterExpressionBuilder b = new FilterExpressionBuilder();
		Filter.Expression expression = b.eq("vectorType", searchRequestDTO.getVectorType()).build();

		return vectorStore.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder()
			.query(searchRequestDTO.getQuery())
			.topK(searchRequestDTO.getTopK())
			.filterExpression(expression)
			.build());
	}

	@Override
	public List<Document> searchTableByNameAndVectorType(SearchRequest searchRequestDTO) {
		FilterExpressionBuilder b = new FilterExpressionBuilder();
		Filter.Expression expression = b
			.and(b.eq("vectorType", searchRequestDTO.getVectorType()), b.eq("id", searchRequestDTO.getName()))
			.build();
		return vectorStore.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder()
			.topK(searchRequestDTO.getTopK())
			.filterExpression(expression)
			.build());
	}

}
