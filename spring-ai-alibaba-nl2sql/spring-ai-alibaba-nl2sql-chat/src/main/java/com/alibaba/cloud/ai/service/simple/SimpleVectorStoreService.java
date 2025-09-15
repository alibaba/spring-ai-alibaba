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

import com.alibaba.cloud.ai.connector.accessor.Accessor;
import com.alibaba.cloud.ai.connector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.connector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.connector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.connector.bo.TableInfoBO;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.request.DeleteRequest;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Primary
public class SimpleVectorStoreService extends BaseVectorStoreService {

	private static final Logger log = LoggerFactory.getLogger(SimpleVectorStoreService.class);

	private final SimpleVectorStore vectorStore; // Keep original global storage for
													// backward compatibility

	private final AgentVectorStoreManager agentVectorStoreManager; // New agent vector
																	// storage manager

	private final Gson gson;

	private final Accessor dbAccessor;

	private final DbConfig dbConfig;

	private final EmbeddingModel embeddingModel;

	@Autowired
	public SimpleVectorStoreService(EmbeddingModel embeddingModel, Gson gson,
			@Qualifier("mysqlAccessor") Accessor dbAccessor, DbConfig dbConfig,
			AgentVectorStoreManager agentVectorStoreManager) {
		log.info("Initializing SimpleVectorStoreService with EmbeddingModel: {}",
				embeddingModel.getClass().getSimpleName());
		this.gson = gson;
		this.dbAccessor = dbAccessor;
		this.dbConfig = dbConfig;
		this.embeddingModel = embeddingModel;
		this.agentVectorStoreManager = agentVectorStoreManager;
		this.vectorStore = SimpleVectorStore.builder(embeddingModel).build(); // Keep
																				// original
																				// implementation
		log.info("SimpleVectorStoreService initialized successfully with AgentVectorStoreManager");
	}

	@Override
	protected EmbeddingModel getEmbeddingModel() {
		return embeddingModel;
	}

	/**
	 * Initialize database schema to vector store
	 * @param schemaInitRequest schema initialization request
	 * @throws Exception if an error occurs
	 */
	@Override
	public Boolean schema(SchemaInitRequest schemaInitRequest) throws Exception {
		log.info("Starting schema initialization for database: {}, schema: {}, tables: {}",
				schemaInitRequest.getDbConfig().getUrl(), schemaInitRequest.getDbConfig().getSchema(),
				schemaInitRequest.getTables());

		DbConfig dbConfig = schemaInitRequest.getDbConfig();
		DbQueryParameter dqp = DbQueryParameter.from(dbConfig)
			.setSchema(dbConfig.getSchema())
			.setTables(schemaInitRequest.getTables());

		// Clean up old schema data
		DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.setVectorType("column");
		deleteDocuments(deleteRequest);
		deleteRequest.setVectorType("table");
		deleteDocuments(deleteRequest);

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

		List<ColumnInfoBO> targetPrimaryList = columnInfoBOS.stream()
			.filter(ColumnInfoBO::isPrimary)
			.collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(targetPrimaryList)) {
			List<String> columnNames = targetPrimaryList.stream()
				.map(ColumnInfoBO::getName)
				.collect(Collectors.toList());
			tableInfoBO.setPrimaryKeys(columnNames);
		}
		else {
			tableInfoBO.setPrimaryKeys(new ArrayList<>());
		}
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
		// Multi-table duplicate field data will be deduplicated, using table name + field
		// name as unique identifier
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
		metadata.put("primaryKey", Optional.ofNullable(tableInfoBO.getPrimaryKeys()).orElse(new ArrayList<>()));
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
	 * Delete vector data with specified conditions
	 * @param deleteRequest delete request
	 * @return whether deletion succeeded
	 */
	public Boolean deleteDocuments(DeleteRequest deleteRequest) throws Exception {
		log.info("Starting delete operation with request: id={}, vectorType={}", deleteRequest.getId(),
				deleteRequest.getVectorType());

		try {
			if (deleteRequest.getId() != null && !deleteRequest.getId().isEmpty()) {
				log.debug("Deleting documents by ID: {}", deleteRequest.getId());
				vectorStore.delete(Arrays.asList(deleteRequest.getId()));
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
	 * Search interface with default filter
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
	 * Search interface with custom filter
	 */
	@Override
	public List<Document> searchWithFilter(SearchRequest searchRequestDTO) {
		log.debug("Searching with custom filter: vectorType={}, query={}, topK={}", searchRequestDTO.getVectorType(),
				searchRequestDTO.getQuery(), searchRequestDTO.getTopK());

		// Need to parse filterFormatted field according to actual situation here, convert
		// to FilterExpressionBuilder expression
		// Simplified implementation, for demonstration only
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

	// ==================== 智能体相关的新方法 ====================

	/**
	 * Initialize database schema to vector store for specified agent
	 * @param agentId agent ID
	 * @param schemaInitRequest schema initialization request
	 * @throws Exception if an error occurs
	 */
	public Boolean schemaForAgent(String agentId, SchemaInitRequest schemaInitRequest) throws Exception {
		log.info("Starting schema initialization for agent: {}, database: {}, schema: {}, tables: {}", agentId,
				schemaInitRequest.getDbConfig().getUrl(), schemaInitRequest.getDbConfig().getSchema(),
				schemaInitRequest.getTables());

		DbConfig dbConfig = schemaInitRequest.getDbConfig();
		DbQueryParameter dqp = DbQueryParameter.from(dbConfig)
			.setSchema(dbConfig.getSchema())
			.setTables(schemaInitRequest.getTables());

		// Clean up agent's old data
		agentVectorStoreManager.deleteDocumentsByType(agentId, "column");
		agentVectorStoreManager.deleteDocumentsByType(agentId, "table");

		log.debug("Fetching foreign keys from database for agent: {}", agentId);
		List<ForeignKeyInfoBO> foreignKeyInfoBOS = dbAccessor.showForeignKeys(dbConfig, dqp);
		log.debug("Found {} foreign keys for agent: {}", foreignKeyInfoBOS.size(), agentId);
		Map<String, List<String>> foreignKeyMap = buildForeignKeyMap(foreignKeyInfoBOS);

		log.debug("Fetching tables from database for agent: {}", agentId);
		List<TableInfoBO> tableInfoBOS = dbAccessor.fetchTables(dbConfig, dqp);
		log.info("Found {} tables to process for agent: {}", tableInfoBOS.size(), agentId);

		for (TableInfoBO tableInfoBO : tableInfoBOS) {
			log.debug("Processing table: {} for agent: {}", tableInfoBO.getName(), agentId);
			processTable(tableInfoBO, dqp, dbConfig, foreignKeyMap);
		}

		log.debug("Converting columns to documents for agent: {}", agentId);
		List<Document> columnDocuments = tableInfoBOS.stream().flatMap(table -> {
			try {
				dqp.setTable(table.getName());
				return dbAccessor.showColumns(dbConfig, dqp)
					.stream()
					.map(column -> convertToDocumentForAgent(agentId, table, column));
			}
			catch (Exception e) {
				log.error("Error processing columns for table: {} and agent: {}", table.getName(), agentId, e);
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());

		log.info("Adding {} column documents to vector store for agent: {}", columnDocuments.size(), agentId);
		agentVectorStoreManager.addDocuments(agentId, columnDocuments);

		log.debug("Converting tables to documents for agent: {}", agentId);
		List<Document> tableDocuments = tableInfoBOS.stream()
			.map(table -> convertTableToDocumentForAgent(agentId, table))
			.collect(Collectors.toList());

		log.info("Adding {} table documents to vector store for agent: {}", tableDocuments.size(), agentId);
		agentVectorStoreManager.addDocuments(agentId, tableDocuments);

		log.info("Schema initialization completed successfully for agent: {}. Total documents added: {}", agentId,
				columnDocuments.size() + tableDocuments.size());
		return true;
	}

	/**
	 * Convert column information to documents for agent
	 */
	private Document convertToDocumentForAgent(String agentId, TableInfoBO tableInfoBO, ColumnInfoBO columnInfoBO) {
		log.debug("Converting column to document for agent: {}, table={}, column={}", agentId, tableInfoBO.getName(),
				columnInfoBO.getName());

		String text = Optional.ofNullable(columnInfoBO.getDescription()).orElse(columnInfoBO.getName());
		String id = agentId + ":" + tableInfoBO.getName() + "." + columnInfoBO.getName();
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("id", id);
		metadata.put("agentId", agentId);
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

		Document document = new Document(id, text, metadata);
		log.debug("Created column document with ID: {} for agent: {}", id, agentId);
		return document;
	}

	/**
	 * Convert table information to documents for agent
	 */
	private Document convertTableToDocumentForAgent(String agentId, TableInfoBO tableInfoBO) {
		log.debug("Converting table to document for agent: {}, table: {}", agentId, tableInfoBO.getName());

		String text = Optional.ofNullable(tableInfoBO.getDescription()).orElse(tableInfoBO.getName());
		String id = agentId + ":" + tableInfoBO.getName();
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("agentId", agentId);
		metadata.put("schema", Optional.ofNullable(tableInfoBO.getSchema()).orElse(""));
		metadata.put("name", tableInfoBO.getName());
		metadata.put("description", Optional.ofNullable(tableInfoBO.getDescription()).orElse(""));
		metadata.put("foreignKey", Optional.ofNullable(tableInfoBO.getForeignKey()).orElse(""));
		metadata.put("primaryKey", Optional.ofNullable(tableInfoBO.getPrimaryKeys()).orElse(new ArrayList<>()));
		metadata.put("vectorType", "table");

		Document document = new Document(id, text, metadata);
		log.debug("Created table document with ID: {} for agent: {}", id, agentId);
		return document;
	}

	/**
	 * Search vector data for specified agent
	 */
	public List<Document> searchWithVectorTypeForAgent(String agentId, SearchRequest searchRequestDTO) {
		log.debug("Searching for agent: {}, vectorType: {}, query: {}, topK: {}", agentId,
				searchRequestDTO.getVectorType(), searchRequestDTO.getQuery(), searchRequestDTO.getTopK());

		List<Document> results = agentVectorStoreManager.similaritySearchWithFilter(agentId,
				searchRequestDTO.getQuery(), searchRequestDTO.getTopK(), searchRequestDTO.getVectorType());

		log.info("Search completed for agent: {}. Found {} documents for vectorType: {}", agentId, results.size(),
				searchRequestDTO.getVectorType());
		return results;
	}

	/**
	 * Delete vector data for specified agent
	 */
	public Boolean deleteDocumentsForAgent(String agentId, DeleteRequest deleteRequest) throws Exception {
		log.info("Starting delete operation for agent: {}, id={}, vectorType={}", agentId, deleteRequest.getId(),
				deleteRequest.getVectorType());

		try {
			if (deleteRequest.getId() != null && !deleteRequest.getId().isEmpty()) {
				log.debug("Deleting documents by ID for agent: {}, ID: {}", agentId, deleteRequest.getId());
				agentVectorStoreManager.deleteDocuments(agentId, Arrays.asList(deleteRequest.getId()));
				log.info("Successfully deleted documents by ID for agent: {}", agentId);
			}
			else if (deleteRequest.getVectorType() != null && !deleteRequest.getVectorType().isEmpty()) {
				log.debug("Deleting documents by vectorType for agent: {}, vectorType: {}", agentId,
						deleteRequest.getVectorType());
				agentVectorStoreManager.deleteDocumentsByType(agentId, deleteRequest.getVectorType());
				log.info("Successfully deleted documents by vectorType for agent: {}", agentId);
			}
			else {
				log.warn("Invalid delete request for agent: {}: either id or vectorType must be specified", agentId);
				throw new IllegalArgumentException("Either id or vectorType must be specified.");
			}
			return true;
		}
		catch (Exception e) {
			log.error("Failed to delete documents for agent: {}: {}", agentId, e.getMessage(), e);
			throw new Exception("Failed to delete collection data for agent " + agentId + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Get agent vector storage manager (for other services to use)
	 */
	public AgentVectorStoreManager getAgentVectorStoreManager() {
		return agentVectorStoreManager;
	}

	/**
	 * Get documents from vector store for specified agent Override parent method, use
	 * agent-specific vector storage
	 */
	@Override
	public List<Document> getDocumentsForAgent(String agentId, String query, String vectorType) {
		log.debug("Getting documents for agent: {}, query: {}, vectorType: {}", agentId, query, vectorType);

		if (agentId == null || agentId.trim().isEmpty()) {
			log.warn("AgentId is null or empty, falling back to global search");
			return getDocuments(query, vectorType);
		}

		try {
			// Use agent vector storage manager for search
			List<Document> results = agentVectorStoreManager.similaritySearchWithFilter(agentId, query, 100, // topK
					vectorType);

			log.info("Found {} documents for agent: {}, vectorType: {}", results.size(), agentId, vectorType);
			return results;
		}
		catch (Exception e) {
			log.error("Error getting documents for agent: {}, falling back to global search", agentId, e);
			return getDocuments(query, vectorType);
		}
	}

}
