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
import com.alibaba.cloud.ai.schema.ColumnDTO;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.schema.TableDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Type;
import java.util.*;
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

    public BaseSchemaService(DbConfig dbConfig, Gson gson) {
        this.dbConfig = dbConfig;
        this.gson = gson;
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
    public abstract SchemaDTO mixRag(String query, List<String> keywords);

    /**
     * 基于 RAG 构建 schema
     * @param query 查询
     * @return SchemaDTO
     */
    public SchemaDTO rag(String query) {
        SchemaDTO schemaDTO = new SchemaDTO();
        extractDatabaseName(schemaDTO);

        List<Document> tableDocuments = vectorStoreService.getDocuments(query, "table");
        List<Document> columnDocuments = vectorStoreService.getDocuments(query, "column");

        List<TableDTO> tableList = buildTableListFromDocuments(tableDocuments);
        attachColumnsToTables(columnDocuments, tableList);

        schemaDTO.setTable(tableList);
        return schemaDTO;
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
     * 将列附加到表
     * @param columnDocs 列文档列表
     * @param tableList 表列表
     */
    protected void attachColumnsToTables(List<Document> columnDocs, List<TableDTO> tableList) {
        for (Document doc : columnDocs) {
            Map<String, Object> meta = doc.getMetadata();
            String tableName = (String) meta.get("tableName");

            ColumnDTO columnDTO = new ColumnDTO();
            columnDTO.setName((String) meta.get("name"));
            columnDTO.setDescription((String) meta.get("description"));
            columnDTO.setType((String) meta.get("type"));

            if (meta.containsKey("samples")) {
                String samplesJson = (String) meta.get("samples");
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> samples = gson.fromJson(samplesJson, listType);
                columnDTO.setData(samples);
            }

            tableList.stream()
                .filter(dto -> dto.getName().equals(tableName))
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
        List<Document> tableDocuments = vectorStoreService.getDocuments(tableName, "table");
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
    protected void extractDatabaseName(SchemaDTO schemaDTO) {
        String pattern = "/([^/]+)$";
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
    }}
