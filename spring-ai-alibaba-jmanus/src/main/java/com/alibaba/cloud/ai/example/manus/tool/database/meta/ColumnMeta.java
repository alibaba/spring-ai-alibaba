package com.alibaba.cloud.ai.example.manus.tool.database.meta;

import java.util.List;

public class ColumnMeta {

	private String columnName;

	private String columnComment;

	private String columnType;

	private String columnLength;

	private String defaultValue;

	private Boolean notNull;

	private List<IndexMeta> indexes;

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getColumnComment() {
		return columnComment;
	}

	public void setColumnComment(String columnComment) {
		this.columnComment = columnComment;
	}

	public String getColumnType() {
		return columnType;
	}

	public void setColumnType(String columnType) {
		this.columnType = columnType;
	}

	public String getColumnLength() {
		return columnLength;
	}

	public void setColumnLength(String columnLength) {
		this.columnLength = columnLength;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Boolean getNotNull() {
		return notNull;
	}

	public void setNotNull(Boolean notNull) {
		this.notNull = notNull;
	}

	public List<IndexMeta> getIndexes() {
		return indexes;
	}

	public void setIndexes(List<IndexMeta> indexes) {
		this.indexes = indexes;
	}

}