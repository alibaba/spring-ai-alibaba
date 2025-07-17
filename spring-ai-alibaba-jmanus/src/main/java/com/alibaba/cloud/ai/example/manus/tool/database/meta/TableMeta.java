package com.alibaba.cloud.ai.example.manus.tool.database.meta;

import java.util.List;

public class TableMeta {
    private String tableName;
    private String tableDesc;
    private List<ColumnMeta> columns;
    private List<IndexMeta> indexes;
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getTableDesc() { return tableDesc; }
    public void setTableDesc(String tableDesc) { this.tableDesc = tableDesc; }
    public List<ColumnMeta> getColumns() { return columns; }
    public void setColumns(List<ColumnMeta> columns) { this.columns = columns; }
    public List<IndexMeta> getIndexes() { return indexes; }
    public void setIndexes(List<IndexMeta> indexes) { this.indexes = indexes; }
} 