package com.alibaba.cloud.ai.example.manus.tool.database.meta;

import java.util.List;

public class ColumnMeta {
    private String columnName;
    private String columnDesc;
    private String columnType;
    private String columnLength;
    private List<IndexMeta> indexes;
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }
    public String getColumnDesc() { return columnDesc; }
    public void setColumnDesc(String columnDesc) { this.columnDesc = columnDesc; }
    public String getColumnType() { return columnType; }
    public void setColumnType(String columnType) { this.columnType = columnType; }
    public String getColumnLength() { return columnLength; }
    public void setColumnLength(String columnLength) { this.columnLength = columnLength; }
    public List<IndexMeta> getIndexes() { return indexes; }
    public void setIndexes(List<IndexMeta> indexes) { this.indexes = indexes; }
} 