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

import com.alibaba.cloud.ai.dbconnector.BizDataSourceTypeEnum;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.schema.ColumnDTO;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.schema.TableDTO;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Schema 构建服务，支持基于 RAG 的混合查询。
 */
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.analytic", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@Service
public class SchemaService extends BaseSchemaService {

	@Autowired
	public SchemaService(DbConfig dbConfig, Gson gson,
	                    @Qualifier("vectorStoreService") BaseVectorStoreService vectorStoreService) {
		super(dbConfig, gson);
		setVectorStoreService(vectorStoreService);
	}

	/**
	 * 混合 RAG 查询接口，��据 query + keywords 搜索并构建 Schema
	 */
	@Override
	public SchemaDTO mixRag(String query, List<String> keywords) {
		SchemaDTO schemaDTO = new SchemaDTO();
		extractDatabaseName(schemaDTO); // 设置数据库名或模式名

		List<Document> tableDocuments = vectorStoreService.getDocuments(query, "table"); // 获取表文档
		List<List<Document>> columnDocumentList = getColumnDocumentsByKeywords(keywords); // 获取列文档列表

		// 处理列权重，并按表关联排序
		processColumnWeights(columnDocumentList, tableDocuments);

		// 初始化列选择器
		Map<String, Document> weightedColumns = selectWeightedColumns(columnDocumentList, 100);
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
		schemaDTO.setForeignKeys(List.of(new ArrayList<>(foreignKeySet)));

		return schemaDTO;
	}



	/**
	 * 根据关键词获取所有列文档
	 */
	public List<List<Document>> getColumnDocumentsByKeywords(List<String> keywords) {
		return keywords.stream().map(kw -> vectorStoreService.getDocuments(kw, "column")).collect(Collectors.toList());
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
					Double tableScore = (Double) tableDoc.getMetadata().get("score");
					Double columnScore = (Double) column.getMetadata().get("score");
					if (tableScore != null && columnScore != null) {
						column.getMetadata().put("score", columnScore * tableScore);
					}
				});
			})
			.sorted(Comparator.comparing((Document d) -> (Double) d.getMetadata().get("score")).reversed())
			.collect(Collectors.toList()));
	}

	/**
	 * 按照权重选取最多 maxCount 个列
	 */
	private Map<String, Document> selectWeightedColumns(List<List<Document>> columnDocumentList, int maxCount) {
		Map<String, Document> result = new HashMap<>();
		int index = 0;

		while (result.size() < maxCount) {
			boolean added = false;
			for (List<Document> docs : columnDocumentList) {
				if (index < docs.size()) {
					Document doc = docs.get(index);
					String id = doc.getId();
					if (!result.containsKey(id)) {
						result.put(id, doc);
						added = true;
					}
				}
			}
			index++;
			if (!added)
				break;
		}
		return result;
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
			SearchRequest request = new SearchRequest();
			request.setQuery(null);
			request.setTopK(10);
			request.setFilterFormatted("jsonb_extract_path_text(metadata, 'vectorType') = '" + vectorType
					+ "' and refdocid = '" + tableName + "'");
			List<Document> docs = vectorStoreService.searchWithFilter(request);
			if (CollectionUtils.isNotEmpty(docs)) {
				tableDocuments.addAll(docs);
			}
		}
	}

	/**
	 * 扩展列文档（通过外键补充缺失的列）
	 */
	private void expandColumnDocumentsWithForeignKeys(Map<String, Document> weightedColumns, Set<String> foreignKeySet,
			String vectorType) {
		Set<String> existingColumnNames = weightedColumns.values()
			.stream()
			.map(doc -> (String) doc.getMetadata().get("name"))
			.collect(Collectors.toSet());

		Set<String> missingColumns = new HashSet<>();
		for (String key : foreignKeySet) {
			String[] parts = key.split("\\.");
			if (parts.length == 2) {
				String columnName = parts[1];
				if (!existingColumnNames.contains(columnName)) {
					missingColumns.add(columnName);
				}
			}
		}

		for (String columnName : missingColumns) {
			SearchRequest request = new SearchRequest();
			request.setQuery(null);
			request.setTopK(10);
			request.setFilterFormatted("jsonb_extract_path_text(metadata, 'vectorType') = '" + vectorType
					+ "' and refdocid = '" + columnName + "'");
			List<Document> docs = vectorStoreService.searchWithFilter(request);
			if (CollectionUtils.isNotEmpty(docs)) {
				for (Document doc : docs) {
					weightedColumns.putIfAbsent(doc.getId(), doc);
				}
			}
		}
	}

	/**
	 * 将列文档附加到对应的表中
	 */
	private void attachColumnsToTables(Map<String, Document> weightedColumns, List<TableDTO> tableList) {
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
				List<String> samples = gson.fromJson(samplesStr, new TypeToken<List<String>>() {}.getType());
				columnDTO.setData(samples);
			}

			String tableName = (String) meta.get("tableName");
			tableList.stream()
				.filter(t -> t.getName().equals(tableName))
				.findFirst()
				.ifPresent(dto -> dto.getColumn().add(columnDTO));
		}
	}
}
