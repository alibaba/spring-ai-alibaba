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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
public class SimpleVectorStoreService extends BaseVectorStoreService {

	private static final Logger log = LoggerFactory.getLogger(SimpleVectorStoreService.class);

	private final SimpleVectorStore vectorStore;

	private final Gson gson;

	private final DbAccessor dbAccessor;

	private final DbConfig dbConfig;

	private final EmbeddingModel embeddingModel;

	@Autowired
	public SimpleVectorStoreService(EmbeddingModel embeddingModel, Gson gson, DbAccessor dbAccessor,
			DbConfig dbConfig) {
		log.info("Initializing SimpleVectorStoreService with EmbeddingModel: {}",
				embeddingModel.getClass().getSimpleName());
		this.gson = gson;
		this.dbAccessor = dbAccessor;
		this.dbConfig = dbConfig;
		this.embeddingModel = embeddingModel;
		this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
		log.info("SimpleVectorStoreService initialized successfully");
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
		log.info("Starting schema initialization for database: {}, schema: {}, tables: {}",
				schemaInitRequest.getDbConfig().getUrl(), schemaInitRequest.getDbConfig().getSchema(),
				schemaInitRequest.getTables());

		DbConfig dbConfig = schemaInitRequest.getDbConfig();
		DbQueryParameter dqp = DbQueryParameter.from(dbConfig)
			.setSchema(dbConfig.getSchema())
			.setTables(schemaInitRequest.getTables());

		DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.setVectorType("column");
		// deleteDocuments(deleteRequest);
		deleteRequest.setVectorType("table");
		// deleteDocuments(deleteRequest);

		log.debug("Fetching foreign keys from database");
		List<ForeignKeyInfoBO> foreignKeyInfoBOS = dbAccessor.showForeignKeys(dbConfig, dqp);
		log.debug("Found {} foreign keys", foreignKeyInfoBOS.size());
		Map<String, List<String>> foreignKeyMap = buildForeignKeyMap(foreignKeyInfoBOS);

		log.debug("Fetching tables from database");
		List<TableInfoBO> tableInfoBOS = dbAccessor.fetchTables(dbConfig, dqp);
		log.info("Found {} tables to process", tableInfoBOS.size());

		for (TableInfoBO tableInfoBO : tableInfoBOS) {
			log.debug("Processing table: {}", tableInfoBO.getName());
			processTable(tableInfoBO, dqp, dbConfig, foreignKeyMap);
		}

		log.debug("Converting columns to documents");
		List<Document> columnDocuments = tableInfoBOS.stream().flatMap(table -> {
			try {
				dqp.setTable(table.getName());
				return dbAccessor.showColumns(dbConfig, dqp).stream().map(column -> convertToDocument(table, column));
			}
			catch (Exception e) {
				log.error("Error processing columns for table: {}", table.getName(), e);
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());

		log.info("Adding {} column documents to vector store", columnDocuments.size());
		vectorStore.add(columnDocuments);

		log.debug("Converting tables to documents");
		List<Document> tableDocuments = tableInfoBOS.stream()
			.map(this::convertTableToDocument)
			.collect(Collectors.toList());

		log.info("Adding {} table documents to vector store", tableDocuments.size());
		vectorStore.add(tableDocuments);

		log.info("Schema initialization completed successfully. Total documents added: {}",
				columnDocuments.size() + tableDocuments.size());
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
		log.debug("Converting column to document: table={}, column={}", tableInfoBO.getName(), columnInfoBO.getName());

		String text = Optional.ofNullable(columnInfoBO.getDescription()).orElse(columnInfoBO.getName());
		String id = tableInfoBO.getName() + "." + columnInfoBO.getName();
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("id", id);
		metadata.put("name", columnInfoBO.getName());
		metadata.put("tableName", tableInfoBO.getName());
		metadata.put("description", Optional.ofNullable(columnInfoBO.getDescription()).orElse(""));
		metadata.put("type", columnInfoBO.getType());
		metadata.put("primary", columnInfoBO.isPrimary());
		metadata.put("notnull", columnInfoBO.isNotnull());
		metadata.put("vectorType", "column");
		if (columnInfoBO.getSamples() != null) {
			metadata.put("samples", columnInfoBO.getSamples());
		}
		// 多表重复字段数据会被去重，采用表名+字段名作为唯一标识
		Document document = new Document(id, text, metadata);
		log.debug("Created column document with ID: {}", id);
		return document;
	}

	public Document convertTableToDocument(TableInfoBO tableInfoBO) {
		log.debug("Converting table to document: {}", tableInfoBO.getName());

		String text = Optional.ofNullable(tableInfoBO.getDescription()).orElse(tableInfoBO.getName());
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("schema", Optional.ofNullable(tableInfoBO.getSchema()).orElse(""));
		metadata.put("name", tableInfoBO.getName());
		metadata.put("description", Optional.ofNullable(tableInfoBO.getDescription()).orElse(""));
		metadata.put("foreignKey", Optional.ofNullable(tableInfoBO.getForeignKey()).orElse(""));
		metadata.put("primaryKey", Optional.ofNullable(tableInfoBO.getPrimaryKey()).orElse(""));
		metadata.put("vectorType", "table");
		Document document = new Document(tableInfoBO.getName(), text, metadata);
		log.debug("Created table document with ID: {}", tableInfoBO.getName());
		return document;
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
		log.info("Starting delete operation with request: id={}, vectorType={}", deleteRequest.getId(),
				deleteRequest.getVectorType());

		try {
			if (deleteRequest.getId() != null && !deleteRequest.getId().isEmpty()) {
				log.debug("Deleting documents by ID: {}", deleteRequest.getId());
				vectorStore.delete(Arrays.asList("comment_count"));
				log.info("Successfully deleted documents by ID");
			}
			else if (deleteRequest.getVectorType() != null && !deleteRequest.getVectorType().isEmpty()) {
				log.debug("Deleting documents by vectorType: {}", deleteRequest.getVectorType());
				FilterExpressionBuilder b = new FilterExpressionBuilder();
				Filter.Expression expression = b.eq("vectorType", deleteRequest.getVectorType()).build();
				List<Document> documents = vectorStore
					.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder()
						.topK(Integer.MAX_VALUE)
						.filterExpression(expression)
						.build());
				if (documents != null && !documents.isEmpty()) {
					log.info("Found {} documents to delete with vectorType: {}", documents.size(),
							deleteRequest.getVectorType());
					vectorStore.delete(documents.stream().map(Document::getId).toList());
					log.info("Successfully deleted {} documents", documents.size());
				}
				else {
					log.info("No documents found to delete with vectorType: {}", deleteRequest.getVectorType());
				}
			}
			else {
				log.warn("Invalid delete request: either id or vectorType must be specified");
				throw new IllegalArgumentException("Either id or vectorType must be specified.");
			}
			return true;
		}
		catch (Exception e) {
			log.error("Failed to delete documents: {}", e.getMessage(), e);
			throw new Exception("Failed to delete collection data by filterExpression: " + e.getMessage(), e);
		}
	}

	/**
	 * 默认 filter 的搜索接口
	 */
	@Override
	public List<Document> searchWithVectorType(SearchRequest searchRequestDTO) {
		log.debug("Searching with vectorType: {}, query: {}, topK: {}", searchRequestDTO.getVectorType(),
				searchRequestDTO.getQuery(), searchRequestDTO.getTopK());

		FilterExpressionBuilder b = new FilterExpressionBuilder();
		Filter.Expression expression = b.eq("vectorType", searchRequestDTO.getVectorType()).build();

		List<Document> results = vectorStore.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder()
			.query(searchRequestDTO.getQuery())
			.topK(searchRequestDTO.getTopK())
			.filterExpression(expression)
			.build());

		if (results == null) {
			results = new ArrayList<>();
		}

		log.info("Search completed. Found {} documents for vectorType: {}", results.size(),
				searchRequestDTO.getVectorType());
		return results;
	}

	/**
	 * 自定义 filter 的搜索接口
	 */
	@Override
	public List<Document> searchWithFilter(SearchRequest searchRequestDTO) {
		log.debug("Searching with custom filter: vectorType={}, query={}, topK={}", searchRequestDTO.getVectorType(),
				searchRequestDTO.getQuery(), searchRequestDTO.getTopK());

		// 这里需要根据实际情况解析 filterFormatted 字段，转换为 FilterExpressionBuilder 的表达式
		// 简化实现，仅作示例
		FilterExpressionBuilder b = new FilterExpressionBuilder();
		Filter.Expression expression = b.eq("vectorType", searchRequestDTO.getVectorType()).build();

		List<Document> results = vectorStore.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder()
			.query(searchRequestDTO.getQuery())
			.topK(searchRequestDTO.getTopK())
			.filterExpression(expression)
			.build());

		if (results == null) {
			results = new ArrayList<>();
		}

		log.info("Search with filter completed. Found {} documents", results.size());
		return results;
	}

	@Override
	public List<Document> searchTableByNameAndVectorType(SearchRequest searchRequestDTO) {
		log.debug("Searching table by name and vectorType: name={}, vectorType={}, topK={}", searchRequestDTO.getName(),
				searchRequestDTO.getVectorType(), searchRequestDTO.getTopK());

		FilterExpressionBuilder b = new FilterExpressionBuilder();
		Filter.Expression expression = b
			.and(b.eq("vectorType", searchRequestDTO.getVectorType()), b.eq("id", searchRequestDTO.getName()))
			.build();

		List<Document> results = vectorStore.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder()
			.topK(searchRequestDTO.getTopK())
			.filterExpression(expression)
			.build());

		if (results == null) {
			results = new ArrayList<>();
		}

		log.info("Search by name completed. Found {} documents for name: {}", results.size(),
				searchRequestDTO.getName());
		return results;
	}

}
