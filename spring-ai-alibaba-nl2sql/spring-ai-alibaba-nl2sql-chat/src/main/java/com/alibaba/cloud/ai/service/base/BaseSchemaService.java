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
package com.alibaba.cloud.ai.service.base;

import com.alibaba.cloud.ai.enums.BizDataSourceTypeEnum;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.dto.schema.ColumnDTO;
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.dto.schema.TableDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Schema service base class, providing common method implementations
 */
public abstract class BaseSchemaService {

	protected final DbConfig dbConfig;

	protected final Gson gson;

	/**
	 * Vector storage service
	 */
	protected final BaseVectorStoreService vectorStoreService;

	public BaseSchemaService(DbConfig dbConfig, Gson gson, BaseVectorStoreService vectorStoreService) {
		this.dbConfig = dbConfig;
		this.gson = gson;
		this.vectorStoreService = vectorStoreService;
	}

	/**
	 * Build schema based on RAG
	 * @param query query
	 * @param keywords keyword list
	 * @return SchemaDTO
	 */
	public SchemaDTO mixRag(String query, List<String> keywords) {
		return mixRagForAgent(null, query, keywords);
	}

	/**
	 * Build schema based on RAG - supports agent isolation
	 * @param agentId agent ID
	 * @param query query
	 * @param keywords keyword list
	 * @return SchemaDTO
	 */
	public SchemaDTO mixRagForAgent(String agentId, String query, List<String> keywords) {
		SchemaDTO schemaDTO = new SchemaDTO();
		extractDatabaseName(schemaDTO); // Set database name or schema name

		List<Document> tableDocuments = getTableDocuments(query, agentId); // Get table
																			// documents
		List<List<Document>> columnDocumentList = getColumnDocumentsByKeywords(keywords, agentId); // Get
																									// column
																									// document
																									// list

		buildSchemaFromDocuments(columnDocumentList, tableDocuments, schemaDTO);

		return schemaDTO;
	}

	public void buildSchemaFromDocuments(List<List<Document>> columnDocumentList, List<Document> tableDocuments,
			SchemaDTO schemaDTO) {
		// Process column weights and sort by table association
		processColumnWeights(columnDocumentList, tableDocuments);

		// Initialize column selector, TODO upper limit 100 has issues
		Map<String, Document> weightedColumns = selectWeightedColumns(columnDocumentList, 100);

		Set<String> foreignKeySet = extractForeignKeyRelations(tableDocuments);

		// Build table list
		List<TableDTO> tableList = buildTableListFromDocuments(tableDocuments);

		// Supplement missing foreign key corresponding tables
		expandTableDocumentsWithForeignKeys(tableDocuments, foreignKeySet, "table");
		expandColumnDocumentsWithForeignKeys(weightedColumns, foreignKeySet, "column");

		// Attach weighted columns to corresponding tables
		attachColumnsToTables(weightedColumns, tableList);

		// Finally assemble SchemaDTO
		schemaDTO.setTable(tableList);

		Set<String> foreignKeys = tableDocuments.stream()
			.map(doc -> (String) doc.getMetadata().getOrDefault("foreignKey", ""))
			.flatMap(fk -> Arrays.stream(fk.split("、")))
			.filter(StringUtils::isNotBlank)
			.collect(Collectors.toSet());
		schemaDTO.setForeignKeys(List.of(new ArrayList<>(foreignKeys)));
	}

	/**
	 * Get all table documents by keywords
	 */
	public List<Document> getTableDocuments(String query) {
		return getTableDocuments(query, null);
	}

	/**
	 * Get all table documents by keywords - supports agent isolation
	 */
	public List<Document> getTableDocuments(String query, String agentId) {
		if (agentId != null && !agentId.trim().isEmpty()) {
			return vectorStoreService.getDocumentsForAgent(agentId, query, "table");
		}
		else {
			return vectorStoreService.getDocuments(query, "table");
		}
	}

	/**
	 * Get all table documents by keywords for specified agent
	 */
	public List<Document> getTableDocumentsForAgent(String agentId, String query) {
		return vectorStoreService.getDocumentsForAgent(agentId, query, "table");
	}

	/**
	 * Get all column documents by keywords
	 */
	public List<List<Document>> getColumnDocumentsByKeywords(List<String> keywords) {
		return getColumnDocumentsByKeywords(keywords, null);
	}

	/**
	 * Get all column documents by keywords - supports agent isolation
	 */
	public List<List<Document>> getColumnDocumentsByKeywords(List<String> keywords, String agentId) {
		if (agentId != null) {
			return getColumnDocumentsByKeywordsForAgent(agentId, keywords);
		}
		else {
			return keywords.stream()
				.map(kw -> vectorStoreService.getDocuments(kw, "column"))
				.collect(Collectors.toList());
		}
	}

	/**
	 * Get all column documents by keywords for specified agent
	 */
	public List<List<Document>> getColumnDocumentsByKeywordsForAgent(String agentId, List<String> keywords) {
		return keywords.stream()
			.map(kw -> vectorStoreService.getDocumentsForAgent(agentId, kw, "column"))
			.collect(Collectors.toList());
	}

	/**
	 * Expand column documents (supplement missing columns through foreign keys)
	 */
	private void expandColumnDocumentsWithForeignKeys(Map<String, Document> weightedColumns, Set<String> foreignKeySet,
			String vectorType) {

		Set<String> existingColumnNames = weightedColumns.keySet();
		Set<String> missingColumns = new HashSet<>();
		for (String key : foreignKeySet) {
			if (!existingColumnNames.contains(key)) {
				missingColumns.add(key);
			}
		}

		for (String columnName : missingColumns) {
			addColumnsDocument(weightedColumns, columnName, vectorType);
		}

	}

	/**
	 * Expand table documents (supplement missing tables through foreign keys)
	 */
	private void expandTableDocumentsWithForeignKeys(List<Document> tableDocuments, Set<String> foreignKeySet,
			String vectorType) {
		Set<String> uniqueTableNames = tableDocuments.stream()
			.map(doc -> (String) doc.getMetadata().get("name"))
			.collect(Collectors.toSet());

		Set<String> missingTables = new HashSet<>();
		for (String key : foreignKeySet) {
			String[] parts = key.split("\\.");
			if (parts.length == 2) {
				String tableName = parts[0];
				if (!uniqueTableNames.contains(tableName)) {
					missingTables.add(tableName);
				}
			}
		}

		for (String tableName : missingTables) {
			addTableDocument(tableDocuments, tableName, vectorType);
		}
	}

	/**
	 * Add missing table documents
	 * @param tableDocuments
	 * @param tableName
	 * @param vectorType
	 */
	protected abstract void addTableDocument(List<Document> tableDocuments, String tableName, String vectorType);

	protected abstract void addColumnsDocument(Map<String, Document> weightedColumns, String columnName,
			String vectorType);

	/**
	 * Select up to maxCount columns by weight
	 */
	protected Map<String, Document> selectWeightedColumns(List<List<Document>> columnDocumentList, int maxCount) {
		Map<String, Document> result = new HashMap<>();
		int index = 0;

		while (result.size() < maxCount) {
			boolean completed = true;
			for (List<Document> docs : columnDocumentList) {
				if (index < docs.size()) {
					Document doc = docs.get(index);
					String id = doc.getId();
					if (!result.containsKey(id)) {
						result.put(id, doc);
					}
					completed = false;
				}
			}
			index++;
			if (completed) {
				break;
			}
		}
		return result;
	}

	/**
	 * Build table list from documents
	 * @param documents document list
	 * @return table list
	 */
	protected List<TableDTO> buildTableListFromDocuments(List<Document> documents) {
		List<TableDTO> tableList = new ArrayList<>();
		for (Document doc : documents) {
			Map<String, Object> meta = doc.getMetadata();
			TableDTO dto = new TableDTO();
			dto.setName((String) meta.get("name"));
			dto.setDescription((String) meta.get("description"));
			if (meta.containsKey("primaryKey")) {
				Object primaryKeyObj = meta.get("primaryKey");
				if (primaryKeyObj instanceof List) {
					@SuppressWarnings("unchecked")
					List<String> primaryKeys = (List<String>) primaryKeyObj;
					dto.setPrimaryKeys(primaryKeys);
				}
				else if (primaryKeyObj instanceof String) {
					String primaryKey = (String) primaryKeyObj;
					if (StringUtils.isNotBlank(primaryKey)) {
						dto.setPrimaryKeys(List.of(primaryKey));
					}
				}
			}
			tableList.add(dto);
		}
		return tableList;
	}

	/**
	 * Score each column (combining with its table's score)
	 */
	public void processColumnWeights(List<List<Document>> columnDocuments, List<Document> tableDocuments) {
		columnDocuments.replaceAll(docs -> docs.stream()
			.filter(column -> tableDocuments.stream()
				.anyMatch(table -> table.getMetadata().get("name").equals(column.getMetadata().get("tableName"))))
			.peek(column -> {
				Optional<Document> matchingTable = tableDocuments.stream()
					.filter(table -> table.getMetadata().get("name").equals(column.getMetadata().get("tableName")))
					.findFirst();
				matchingTable.ifPresent(tableDoc -> {
					Double tableScore = Optional.ofNullable((Double) tableDoc.getMetadata().get("score"))
						.orElse(tableDoc.getScore());
					Double columnScore = Optional.ofNullable((Double) column.getMetadata().get("score"))
						.orElse(column.getScore());
					if (tableScore != null && columnScore != null) {
						column.getMetadata().put("score", columnScore * tableScore);
					}
				});
			})
			.sorted(Comparator.comparing((Document d) -> (Double) d.getMetadata().get("score")).reversed())
			.collect(Collectors.toList()));
	}

	/**
	 * Extract foreign key relationships
	 * @param tableDocuments table document list
	 * @return foreign key relationship set
	 */
	protected Set<String> extractForeignKeyRelations(List<Document> tableDocuments) {
		Set<String> result = new HashSet<>();

		for (Document doc : tableDocuments) {
			String foreignKeyStr = (String) doc.getMetadata().getOrDefault("foreignKey", "");
			if (StringUtils.isNotBlank(foreignKeyStr)) {
				Arrays.stream(foreignKeyStr.split("、")).forEach(pair -> {
					String[] parts = pair.split("=");
					if (parts.length == 2) {
						result.add(parts[0].trim());
						result.add(parts[1].trim());
					}
				});
			}
		}

		return result;
	}

	/**
	 * Attach column documents to corresponding tables
	 */
	protected void attachColumnsToTables(Map<String, Document> weightedColumns, List<TableDTO> tableList) {
		if (CollectionUtils.isEmpty(weightedColumns.values())) {
			return;
		}

		for (Document columnDoc : weightedColumns.values()) {
			Map<String, Object> meta = columnDoc.getMetadata();
			ColumnDTO columnDTO = new ColumnDTO();
			columnDTO.setName((String) meta.get("name"));
			columnDTO.setDescription((String) meta.get("description"));
			columnDTO.setType((String) meta.get("type"));

			String samplesStr = (String) meta.get("samples");
			if (StringUtils.isNotBlank(samplesStr)) {
				List<String> samples = gson.fromJson(samplesStr, new TypeToken<List<String>>() {
				}.getType());
				columnDTO.setData(samples);
			}

			String tableName = (String) meta.get("tableName");
			tableList.stream()
				.filter(t -> t.getName().equals(tableName))
				.findFirst()
				.ifPresent(dto -> dto.getColumn().add(columnDTO));
		}
	}

	/**
	 * Get table metadata
	 * @param tableName table name
	 * @return table metadata
	 */
	protected Map<String, Object> getTableMetadata(String tableName) {
		List<Document> tableDocuments = getTableDocuments(tableName);
		for (Document doc : tableDocuments) {
			Map<String, Object> metadata = doc.getMetadata();
			if (tableName.equals(metadata.get("name"))) {
				return metadata;
			}
		}
		return null;
	}

	/**
	 * Extract database name
	 * @param schemaDTO SchemaDTO
	 */
	public void extractDatabaseName(SchemaDTO schemaDTO) {
		String pattern = ":\\d+/([^/?&]+)";
		if (BizDataSourceTypeEnum.isMysqlDialect(dbConfig.getDialectType())) {
			Pattern regex = Pattern.compile(pattern);
			Matcher matcher = regex.matcher(dbConfig.getUrl());
			if (matcher.find()) {
				schemaDTO.setName(matcher.group(1));
			}
		}
		else if (BizDataSourceTypeEnum.isPgDialect(dbConfig.getDialectType())) {
			schemaDTO.setName(dbConfig.getSchema());
		}
	}

	/**
	 * Common document query processing template to reduce subclass redundant code.
	 */
	protected void handleDocumentQuery(List<Document> targetList, String key, String vectorType,
			Function<String, SearchRequest> requestBuilder, Function<SearchRequest, List<Document>> searchFunc) {
		SearchRequest request = requestBuilder.apply(key);
		request.setVectorType(vectorType);
		request.setTopK(10);
		List<Document> docs = searchFunc.apply(request);
		if (CollectionUtils.isNotEmpty(docs)) {
			targetList.addAll(docs);
		}
	}

	protected void handleDocumentQuery(Map<String, Document> targetMap, String key, String vectorType,
			Function<String, SearchRequest> requestBuilder, Function<SearchRequest, List<Document>> searchFunc) {
		SearchRequest request = requestBuilder.apply(key);
		request.setVectorType(vectorType);
		request.setTopK(10);
		List<Document> docs = searchFunc.apply(request);
		if (CollectionUtils.isNotEmpty(docs)) {
			for (Document doc : docs) {
				targetMap.putIfAbsent(doc.getId(), doc);
			}
		}
	}

}
