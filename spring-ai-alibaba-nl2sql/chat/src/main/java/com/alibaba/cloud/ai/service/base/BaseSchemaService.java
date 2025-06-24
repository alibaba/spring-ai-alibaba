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

import com.alibaba.cloud.ai.dbconnector.BizDataSourceTypeEnum;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.schema.ColumnDTO;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.schema.TableDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;

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
 * Schema 服务基类，提供共同的方法实现
 */
public abstract class BaseSchemaService {

	protected final DbConfig dbConfig;

	protected final Gson gson;

	/**
	 * 向量存储服务
	 */
	protected BaseVectorStoreService vectorStoreService;

	protected final BaseVectorStoreService baseVectorStoreService;

	public BaseSchemaService(DbConfig dbConfig, Gson gson, BaseVectorStoreService vectorStoreService) {
		this.dbConfig = dbConfig;
		this.gson = gson;
		this.baseVectorStoreService = vectorStoreService;
	}

	/**
	 * 设置向量存储服务
	 * @param vectorStoreService 向量存储服务
	 */
	@Autowired
	public void setVectorStoreService(BaseVectorStoreService vectorStoreService) {
		this.vectorStoreService = vectorStoreService;
	}

	/**
	 * 基于 RAG 构建 schema
	 * @param query 查询
	 * @param keywords 关键词列表
	 * @return SchemaDTO
	 */
	public SchemaDTO mixRag(String query, List<String> keywords) {
		SchemaDTO schemaDTO = new SchemaDTO();
		extractDatabaseName(schemaDTO); // 设置数据库名或模式名

		List<Document> tableDocuments = getTableDocuments(query); // 获取表文档
		List<List<Document>> columnDocumentList = getColumnDocumentsByKeywords(keywords); // 获取列文档列表

		buildSchemaFromDocuments(columnDocumentList, tableDocuments, schemaDTO);

		return schemaDTO;
	}

	public void buildSchemaFromDocuments(List<List<Document>> columnDocumentList, List<Document> tableDocuments,
			SchemaDTO schemaDTO) {
		// 处理列权重，并按表关联排序
		processColumnWeights(columnDocumentList, tableDocuments);

		// 初始化列选择器
		Map<String, Document> weightedColumns = selectWeightedColumns(columnDocumentList, 100); // TODO
																								// 上限100存在问题
		Set<String> foreignKeySet = extractForeignKeyRelations(tableDocuments);

		// 构建表列表
		List<TableDTO> tableList = buildTableListFromDocuments(tableDocuments);

		// 补充缺失的外键对应表
		expandTableDocumentsWithForeignKeys(tableDocuments, foreignKeySet, "table");
		expandColumnDocumentsWithForeignKeys(weightedColumns, foreignKeySet, "column");

		// 将加权列附加到对应的表中
		attachColumnsToTables(weightedColumns, tableList);

		// 最终组装 SchemaDTO
		schemaDTO.setTable(tableList);

		Set<String> foreignKeys = tableDocuments.stream()
			.map(doc -> (String) doc.getMetadata().getOrDefault("foreignKey", ""))
			.flatMap(fk -> Arrays.stream(fk.split("、")))
			.filter(StringUtils::isNotBlank)
			.collect(Collectors.toSet());
		schemaDTO.setForeignKeys(List.of(new ArrayList<>(foreignKeys)));
	}

	/**
	 * 根据关键词获取所有表文档
	 */
	public List<Document> getTableDocuments(String query) {
		return vectorStoreService.getDocuments(query, "table");
	}

	/**
	 * 根据关键词获取所有列文档
	 */
	public List<List<Document>> getColumnDocumentsByKeywords(List<String> keywords) {
		return keywords.stream().map(kw -> vectorStoreService.getDocuments(kw, "column")).collect(Collectors.toList());
	}

	/**
	 * 扩展列文档（通过外键补充缺失的列）
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
	 * 扩展表文档（通过外键补充缺失的表）
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
	 * 添加缺失的表文档
	 * @param tableDocuments
	 * @param tableName
	 * @param vectorType
	 */
	protected abstract void addTableDocument(List<Document> tableDocuments, String tableName, String vectorType);

	protected abstract void addColumnsDocument(Map<String, Document> weightedColumns, String columnName,
			String vectorType);

	/**
	 * 按照权重选取最多 maxCount 个列
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
	 * 从文档构建表列表
	 * @param documents 文档列表
	 * @return 表列表
	 */
	protected List<TableDTO> buildTableListFromDocuments(List<Document> documents) {
		List<TableDTO> tableList = new ArrayList<>();
		for (Document doc : documents) {
			Map<String, Object> meta = doc.getMetadata();
			TableDTO dto = new TableDTO();
			dto.setName((String) meta.get("name"));
			dto.setDescription((String) meta.get("description"));
			if (meta.containsKey("primaryKey")) {
				String primaryKey = (String) meta.get("primaryKey");
				if (StringUtils.isNotBlank(primaryKey)) {
					dto.setPrimaryKeys(List.of(primaryKey));
				}
			}
			tableList.add(dto);
		}
		return tableList;
	}

	/**
	 * 给每个列打分（结合其所在表的评分）
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
	 * 提取外键关系
	 * @param tableDocuments 表文档列表
	 * @return 外键关系集合
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
	 * 将列文档附加到对应的表中
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
	 * 获取表元数据
	 * @param tableName 表名
	 * @return 表元数据
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
	 * 提取数据库名称
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
	 * 通用文档查询处理模板，减少子类冗余代码。
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
