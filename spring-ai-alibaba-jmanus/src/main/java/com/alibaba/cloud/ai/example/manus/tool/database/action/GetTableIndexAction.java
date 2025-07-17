package com.alibaba.cloud.ai.example.manus.tool.database.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.alibaba.cloud.ai.example.manus.tool.database.action.AbstractDatabaseAction;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.IndexMeta;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GetTableIndexAction extends AbstractDatabaseAction {
    @Override
    public ToolExecuteResult execute(DatabaseRequestVO requestVO, DataSourceService dataSourceService) {
        String text = requestVO.getText();
        if (text == null || text.trim().isEmpty()) {
            return new ToolExecuteResult("缺少查询语句");
        }
        String[] tableNames = text.split(",");
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < tableNames.length; i++) {
            inClause.append("?");
            if (i < tableNames.length - 1) inClause.append(",");
        }
        String sql = "SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, INDEX_TYPE FROM information_schema.STATISTICS WHERE TABLE_NAME IN (" + inClause + ") ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX";
        try (Connection conn = dataSourceService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < tableNames.length; i++) {
                ps.setString(i + 1, tableNames[i].trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                // Map<tableName, Map<indexName, IndexMeta>>
                java.util.Map<String, java.util.Map<String, IndexMeta>> tableIndexMap = new java.util.HashMap<>();
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    String indexType = rs.getString("INDEX_TYPE");
                    tableIndexMap.putIfAbsent(tableName, new java.util.HashMap<>());
                    java.util.Map<String, IndexMeta> indexMap = tableIndexMap.get(tableName);
                    IndexMeta indexMeta = indexMap.get(indexName);
                    if (indexMeta == null) {
                        indexMeta = new IndexMeta();
                        indexMeta.setIndexName(indexName);
                        indexMeta.setIndexType(indexType);
                        indexMeta.setRefColumnNames(new java.util.ArrayList<>());
                        indexMap.put(indexName, indexMeta);
                    }
                    indexMeta.getRefColumnNames().add(columnName);
                }
                // 合并所有表的所有索引为一个列表
                java.util.List<IndexMeta> allIndexes = new java.util.ArrayList<>();
                for (java.util.Map<String, IndexMeta> indexMap : tableIndexMap.values()) {
                    allIndexes.addAll(indexMap.values());
                }
                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(allIndexes);
                return new ToolExecuteResult(json);
            }
        } catch (Exception e) {
            return new ToolExecuteResult("执行查询时出错: " + e.getMessage());
        }
    }
} 