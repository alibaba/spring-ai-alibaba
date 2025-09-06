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

import com.alibaba.cloud.ai.annotation.ConditionalOnMilvusEnabled;
import com.alibaba.cloud.ai.connector.accessor.Accessor;
import com.alibaba.cloud.ai.connector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.connector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.connector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.connector.bo.TableInfoBO;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.request.DeleteRequest;
import com.alibaba.cloud.ai.request.EvidenceRequest;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core vector database operation service for Milvus, providing vector writing, querying,
 * deleting, and schema initialization functions.
 */
@Service
@ConditionalOnMilvusEnabled
public class MilvusVectorStoreManagementService implements VectorStoreManagementService {

	private static final Logger log = LoggerFactory.getLogger(MilvusVectorStoreManagementService.class);

	private final EmbeddingModel embeddingModel;

	private final MilvusVectorStore milvusVectorStore;

	private final Accessor dbAccessor;

	private final Gson gson;

	@Autowired
	public MilvusVectorStoreManagementService(EmbeddingModel embeddingModel, MilvusVectorStore milvusVectorStore,
			@Qualifier("mysqlAccessor") Accessor dbAccessor, Gson gson) {
		this.embeddingModel = embeddingModel;
		this.milvusVectorStore = milvusVectorStore;
		this.dbAccessor = dbAccessor;
		this.gson = gson;
	}

	/**
	 * Adds evidence content to the vector store.
	 * @param evidenceRequests List of evidence requests.
	 * @return True if successful.
	 */
	@Override
	public Boolean addEvidence(List<EvidenceRequest> evidenceRequests) {
		List<Document> evidences = new ArrayList<>();
		for (EvidenceRequest req : evidenceRequests) {
			Document doc = new Document(UUID.randomUUID().toString(), req.getContent(),
					Map.of("evidenceType", req.getType(), "vectorType", "evidence"));
			evidences.add(doc);
		}
		milvusVectorStore.add(evidences);
		return true;
	}

	/**
	 * Embeds text into a vector.
	 * @param text Input text.
	 * @return Vector representation.
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
	 * Performs a vector search.
	 * @param searchRequest Search request.
	 * @return List of matching documents.
	 */

	public List<Document> search(SearchRequest searchRequest) {
		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression filterExpression = builder.eq("vectorType", searchRequest.getVectorType()).build();

		org.springframework.ai.vectorstore.SearchRequest request = org.springframework.ai.vectorstore.SearchRequest
			.builder()
			.query(searchRequest.getQuery())
			.topK(searchRequest.getTopK())
			.filterExpression(filterExpression)
			.build();

		return milvusVectorStore.similaritySearch(request);
	}

	/**
	 * Deletes vector data based on specified conditions.
	 * @param deleteRequest Delete request.
	 * @return True if successful.
	 */
	@Override
	public Boolean deleteDocuments(DeleteRequest deleteRequest) throws Exception {
		if ((deleteRequest.getId() == null || deleteRequest.getId().isEmpty())
				&& (deleteRequest.getVectorType() == null || deleteRequest.getVectorType().isEmpty())) {
			throw new IllegalArgumentException("Either id or vectorType must be specified.");
		}

		try {
			if (deleteRequest.getId() != null && !deleteRequest.getId().isEmpty()) {
				milvusVectorStore.delete(Collections.singletonList(deleteRequest.getId()));
			}
			else {
				// At this point vectorType is definitely not null/empty
				FilterExpressionBuilder b = new FilterExpressionBuilder();
				Filter.Expression expression = b.eq("vectorType", deleteRequest.getVectorType()).build();
				milvusVectorStore.delete(expression);
			}

			return true;
		}
		catch (Exception e) {
			throw new Exception("Failed to delete documents: " + e.getMessage(), e);
		}
	}

	/**
	 * Initializes the database schema into the vector store.
	 * @param schemaInitRequest Schema initialization request.
	 * @throws Exception If an error occurs.
	 */
	@Override
	public Boolean schema(SchemaInitRequest schemaInitRequest) throws Exception {
		DbConfig dbConfig = schemaInitRequest.getDbConfig();
		DbQueryParameter dqp = DbQueryParameter.from(dbConfig)
			.setSchema(dbConfig.getSchema())
			.setTables(schemaInitRequest.getTables());

		// Clear existing schema documents
		DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.setVectorType("column");
		deleteDocuments(deleteRequest);
		deleteRequest.setVectorType("table");
		deleteDocuments(deleteRequest);

		List<ForeignKeyInfoBO> foreignKeyInfoBOS = dbAccessor.showForeignKeys(dbConfig, dqp);
		Map<String, List<String>> foreignKeyMap = buildForeignKeyMap(foreignKeyInfoBOS);

		List<TableInfoBO> tableInfoBOS = dbAccessor.fetchTables(dbConfig, dqp);
		for (TableInfoBO tableInfoBO : tableInfoBOS) {
			processTable(tableInfoBO, dqp, dbConfig, foreignKeyMap, dbAccessor);
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

		milvusVectorStore.add(columnDocuments);
		log.info("Added {} column documents to Milvus.", columnDocuments.size());

		List<Document> tableDocuments = tableInfoBOS.stream()
			.map(this::convertTableToDocument)
			.collect(Collectors.toList());

		milvusVectorStore.add(tableDocuments);
		log.info("Added {} table documents to Milvus.", tableDocuments.size());

		return true;
	}

	private void processTable(TableInfoBO tableInfoBO, DbQueryParameter dqp, DbConfig dbConfig,
			Map<String, List<String>> foreignKeyMap, Accessor dbAccessor) throws Exception {
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

	public Document convertToDocument(TableInfoBO tableInfoBO, ColumnInfoBO columnInfoBO) {
		String text = Optional.ofNullable(columnInfoBO.getDescription()).orElse(columnInfoBO.getName());
		// Use a mutable map to allow adding more properties
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("name", columnInfoBO.getName());
		metadata.put("tableName", tableInfoBO.getName());
		metadata.put("description", Optional.ofNullable(columnInfoBO.getDescription()).orElse(""));
		metadata.put("type", Optional.ofNullable(columnInfoBO.getType()).orElse(""));
		metadata.put("primary", columnInfoBO.isPrimary());
		metadata.put("notnull", columnInfoBO.isNotnull());
		metadata.put("vectorType", "column");
		if (columnInfoBO.getSamples() != null) {
			metadata.put("samples", columnInfoBO.getSamples());
		}
		// Use a unique ID for the document to avoid conflicts
		String documentId = tableInfoBO.getName() + "." + columnInfoBO.getName();
		return new Document(documentId, text, metadata);
	}

	public Document convertTableToDocument(TableInfoBO tableInfoBO) {
		String text = Optional.ofNullable(tableInfoBO.getDescription()).orElse(tableInfoBO.getName());
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("schema", Optional.ofNullable(tableInfoBO.getSchema()).orElse(""));
		metadata.put("name", tableInfoBO.getName());
		metadata.put("description", Optional.ofNullable(tableInfoBO.getDescription()).orElse(""));
		metadata.put("foreignKey", Optional.ofNullable(tableInfoBO.getForeignKey()).orElse(""));
		metadata.put("primaryKey", Optional.ofNullable(tableInfoBO.getPrimaryKey()).orElse(""));
		metadata.put("vectorType", "table");
		return new Document(tableInfoBO.getName(), text, metadata);
	}

}
