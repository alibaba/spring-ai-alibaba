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

import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.schema.ColumnDTO;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.schema.TableDTO;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SimpleSchemaService extends BaseSchemaService {

	@Autowired
	public SimpleSchemaService(DbConfig dbConfig, Gson gson,
	                          @Qualifier("simpleVectorStoreService") BaseVectorStoreService vectorStoreService) {
		super(dbConfig, gson);
		setVectorStoreService(vectorStoreService);
	}

	/**
	 * 基于 RAG 构建 schema
	 * @param query 查询
	 * @param keywords 关键词列表
	 * @return SchemaDTO
	 */
	@Override
	public SchemaDTO mixRag(String query, List<String> keywords) {
		SchemaDTO schemaDTO = new SchemaDTO();
		extractDatabaseName(schemaDTO); // 设置数据库名或模式名

		// 获取表文档
		List<Document> tableDocuments = new ArrayList<>();
		for (String keyword : keywords) {
			List<Document> docs = vectorStoreService.getDocuments(keyword, "table");
			tableDocuments.addAll(docs);
		}

		// 去重
		tableDocuments = tableDocuments.stream()
			.collect(Collectors.toMap(Document::getId, d -> d, (d1, d2) -> d1))
			.values()
			.stream()
			.collect(Collectors.toList());

		// 构建表列表
		List<TableDTO> tableList = buildTableListFromDocuments(tableDocuments);

		// 获取外键关系
		Set<String> foreignKeySet = extractForeignKeyRelationships(tableList);

		// 最终组装 SchemaDTO
		schemaDTO.setTable(tableList);
		schemaDTO.setForeignKeys(List.of(new ArrayList<>(foreignKeySet)));

		return schemaDTO;
	}




	/**
	 * 根据关键词获取列文档
	 * @param keywords 关键词列表
	 * @return 列文档列表
	 */
	public List<Document> getColumnDocumentsByKeywords(List<String> keywords) {
		List<Document> columnDocuments = new ArrayList<>();
		for (String keyword : keywords) {
			List<Document> docs = vectorStoreService.getDocuments(keyword, "column");
			columnDocuments.addAll(docs);
		}

		// 去重
		columnDocuments = columnDocuments.stream()
			.collect(Collectors.toMap(Document::getId, d -> d, (d1, d2) -> d1))
			.values()
			.stream()
			.collect(Collectors.toList());

		return columnDocuments;
	}

	/**
	 * 处理列权重
	 * @param columnDocuments 列文档列表
	 * @param query 查询
	 * @return 带权重的列映射
	 */
	public Map<String, Document> processColumnWeights(List<Document> columnDocuments, String query) {
		Map<String, Document> weightedColumns = new HashMap<>();
		for (Document doc : columnDocuments) {
			Map<String, Object> metadata = doc.getMetadata();
			String columnName = (String) metadata.get("name");
			String tableName = (String) metadata.get("tableName");
			String key = tableName + "." + columnName;
			weightedColumns.put(key, doc);
		}
		return weightedColumns;
	}

	/**
	 * 选择带权重的列
	 * @param weightedColumns 带权重的列映射
	 * @param tableList 表列表
	 */
	public void selectWeightedColumns(Map<String, Document> weightedColumns, List<TableDTO> tableList) {
		attachColumnsToTables(weightedColumns.values().stream().toList(), tableList);
	}

	/**
	 * 提取外键关系
	 * @param tableList 表列表
	 * @return 外键关系集合
	 */
	private Set<String> extractForeignKeyRelationships(List<TableDTO> tableList) {
		Set<String> foreignKeySet = new HashSet<>();
		for (TableDTO tableDTO : tableList) {
			Map<String, Object> metadata = getTableMetadata(tableDTO.getName());
			if (metadata != null && metadata.containsKey("foreignKey")) {
				String foreignKey = (String) metadata.get("foreignKey");
				if (StringUtils.isNotBlank(foreignKey)) {
					String[] keys = foreignKey.split("、");
					foreignKeySet.addAll(Arrays.asList(keys));
				}
			}
		}
		return foreignKeySet;
	}

	/**
	 * 获取表元数据
	 * @param tableName 表名
	 * @return 表元数据
	 */
	@Override
	protected Map<String, Object> getTableMetadata(String tableName) {
		List<Document> tableDocuments = vectorStoreService.getDocuments(tableName, "table");
		for (Document doc : tableDocuments) {
			Map<String, Object> metadata = doc.getMetadata();
			if (tableName.equals(metadata.get("name"))) {
				return metadata;
			}
		}
		return null;
	}


}
