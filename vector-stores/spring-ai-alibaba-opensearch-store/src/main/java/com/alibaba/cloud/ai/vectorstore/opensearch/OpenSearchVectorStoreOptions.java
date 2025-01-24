package com.alibaba.cloud.ai.vectorstore.opensearch;

import java.util.List;

/**
 * Configuration options for the OpenSearch vector store. This class provides settings for
 * the index name, primary key field, output fields, and vector dimensions.
 *
 * @author fuyou.lxm
 * @since 1.0.0-M3
 */
public class OpenSearchVectorStoreOptions {

	public static final String DEFAULT_TABLE_NAME = "spring_ai_opensearch_vector_store";

	public static final List<String> DEFAULT_OUTPUT_FIELDS = List.of("content", "metadata");

	/**
	 * The name of the index to store the vectors.
	 */
	private String tableName = DEFAULT_TABLE_NAME;

	/**
	 * The primary key field for the index.
	 */
	private String primaryKeyField = "id";

	/**
	 * The fields to be output in the search results.
	 */
	private List<String> outputFields = DEFAULT_OUTPUT_FIELDS;

	/**
	 * The number of dimensions in the vector.
	 */
	private int dimensions = 1536;

	public String getTableName() {
		return this.tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getPrimaryKeyField() {
		return primaryKeyField;
	}

	public void setPrimaryKeyField(String primaryKeyField) {
		this.primaryKeyField = primaryKeyField;
	}

	public List<String> getOutputFields() {
		return outputFields;
	}

	public void setOutputFields(List<String> outputFields) {
		this.outputFields = outputFields;
	}

	public int getDimensions() {
		return this.dimensions;
	}

	public void setDimensions(int dims) {
		this.dimensions = dims;
	}

}
