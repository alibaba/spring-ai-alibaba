package com.alibaba.cloud.ai.example.manus.tool.database.meta;

import java.util.List;

public class IndexMeta {

	private String indexName;

	private List<String> refColumnNames;

	private String indexType;

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public List<String> getRefColumnNames() {
		return refColumnNames;
	}

	public void setRefColumnNames(List<String> refColumnNames) {
		this.refColumnNames = refColumnNames;
	}

	public String getIndexType() {
		return indexType;
	}

	public void setIndexType(String indexType) {
		this.indexType = indexType;
	}

}