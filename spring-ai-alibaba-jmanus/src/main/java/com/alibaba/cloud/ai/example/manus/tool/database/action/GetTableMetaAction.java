package com.alibaba.cloud.ai.example.manus.tool.database.action;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.database.AbstractDatabaseAction;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.TableMeta;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.ColumnMeta;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.IndexMeta;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class GetTableMetaAction extends AbstractDatabaseAction {
    private static final Logger log = LoggerFactory.getLogger(GetTableMetaAction.class);

    @Override
    public ToolExecuteResult execute(DatabaseRequestVO requestVO, DataSourceService dataSourceService) {
        String text = requestVO.getText();
        List<TableMeta> tableMetaList = new ArrayList<>();
        Map<String, TableMeta> tableMetaMap = new LinkedHashMap<>();
        // 1. 获取表信息
        String tableSql;
        boolean fuzzy = text != null && !text.trim().isEmpty();
        if (fuzzy) {
            tableSql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_COMMENT LIKE ? AND table_schema NOT IN ('sys','mysql','performance_schema','information_schema')";
        } else {
            tableSql = "SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE table_schema NOT IN ('sys','mysql','performance_schema','information_schema')";
        }
        try (Connection conn = dataSourceService.getConnection();
             PreparedStatement ps = conn.prepareStatement(tableSql)) {
            if (fuzzy) {
                ps.setString(1, "%" + text + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableComment = rs.getString("TABLE_COMMENT");
                    TableMeta tableMeta = new TableMeta();
                    tableMeta.setTableName(tableName);
                    tableMeta.setTableDesc(tableComment);
                    tableMeta.setColumns(new ArrayList<>());
                    tableMeta.setIndexes(new ArrayList<>());
                    tableMetaMap.put(tableName, tableMeta);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching table info", e);
            return new ToolExecuteResult("获取表信息出错: " + e.getMessage());
        }
        if (tableMetaMap.isEmpty()) {
            return new ToolExecuteResult("未找到符合条件的表");
        }
        // 2. 获取字段信息
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < tableMetaMap.size(); i++) {
            inClause.append("?");
            if (i < tableMetaMap.size() - 1) inClause.append(",");
        }
        String columnSql = "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH, COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_NAME IN (" + inClause + ") ORDER BY TABLE_NAME, ORDINAL_POSITION";
        try (Connection conn = dataSourceService.getConnection();
             PreparedStatement ps = conn.prepareStatement(columnSql)) {
            int idx = 1;
            for (String tableName : tableMetaMap.keySet()) {
                ps.setString(idx++, tableName);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    TableMeta tableMeta = tableMetaMap.get(tableName);
                    if (tableMeta == null) continue;
                    ColumnMeta columnMeta = new ColumnMeta();
                    columnMeta.setColumnName(rs.getString("COLUMN_NAME"));
                    columnMeta.setColumnType(rs.getString("COLUMN_TYPE"));
                    columnMeta.setColumnLength(rs.getObject("CHARACTER_MAXIMUM_LENGTH") == null ? null : rs.getObject("CHARACTER_MAXIMUM_LENGTH").toString());
                    columnMeta.setColumnDesc(rs.getString("COLUMN_COMMENT"));
                    columnMeta.setIndexes(new ArrayList<>());
                    tableMeta.getColumns().add(columnMeta);
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching column info", e);
            return new ToolExecuteResult("获取字段信息出错: " + e.getMessage());
        }
        // 3. 获取索引信息
        String indexSql = "SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, INDEX_TYPE FROM information_schema.STATISTICS WHERE TABLE_NAME IN (" + inClause + ") ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX";
        try (Connection conn = dataSourceService.getConnection();
             PreparedStatement ps = conn.prepareStatement(indexSql)) {
            int idx = 1;
            for (String tableName : tableMetaMap.keySet()) {
                ps.setString(idx++, tableName);
            }
            try (ResultSet rs = ps.executeQuery()) {
                // Map<tableName, Map<indexName, IndexMeta>>
                Map<String, Map<String, IndexMeta>> tableIndexMap = new HashMap<>();
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String indexName = rs.getString("INDEX_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    String indexType = rs.getString("INDEX_TYPE");
                    TableMeta tableMeta = tableMetaMap.get(tableName);
                    if (tableMeta == null) continue;
                    // Find the column
                    ColumnMeta refColumn = null;
                    for (ColumnMeta col : tableMeta.getColumns()) {
                        if (col.getColumnName().equals(columnName)) {
                            refColumn = col;
                            break;
                        }
                    }
                    // Group index by tableName+indexName
                    tableIndexMap.putIfAbsent(tableName, new HashMap<>());
                    Map<String, IndexMeta> indexMap = tableIndexMap.get(tableName);
                    IndexMeta indexMeta = indexMap.get(indexName);
                    if (indexMeta == null) {
                        indexMeta = new IndexMeta();
                        indexMeta.setIndexName(indexName);
                        indexMeta.setIndexType(indexType);
                        indexMeta.setRefColumnNames(new ArrayList<>());
                        indexMap.put(indexName, indexMeta);
                        tableMeta.getIndexes().add(indexMeta);
                    }
                    indexMeta.getRefColumnNames().add(columnName);
                    if (refColumn != null) {
                        refColumn.getIndexes().add(indexMeta);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching index info", e);
            return new ToolExecuteResult("获取索引信息出错: " + e.getMessage());
        }
        tableMetaList.addAll(tableMetaMap.values());
        // 4. 返回结构化对象
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(tableMetaList);
            return new ToolExecuteResult(json);
        } catch (JsonProcessingException e) {
            log.error("Error serializing result", e);
            return new ToolExecuteResult("结果序列化出错: " + e.getMessage());
        }
    }
} 