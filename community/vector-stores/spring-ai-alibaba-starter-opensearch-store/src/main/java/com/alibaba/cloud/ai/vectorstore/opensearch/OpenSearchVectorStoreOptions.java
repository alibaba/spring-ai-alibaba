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
package com.alibaba.cloud.ai.vectorstore.opensearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration options for the OpenSearch vector store. This class provides settings for
 * the index name, primary key field, output fields, and vector dimensions.
 *
 * @author fuyou.lxm 北极星
 * @since 1.0.0-M6
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.vectorstore.opensearch.options")
public class OpenSearchVectorStoreOptions {

	public static final String DEFAULT_TABLE_NAME = "spring_ai_opensearch_vector_store";

	public static final List<String> DEFAULT_OUTPUT_FIELDS = List.of("content", "metadata");

	public String getMappingJson() {
		return mappingJson;
	}

	public OpenSearchVectorStoreOptions setMappingJson(String mappingJson) {
		this.mappingJson = mappingJson;
		return this;
	}

	private String mappingJson = """
			{
			    "name": "api",
			    "partitionCount": 1,
			    "primaryKey": "id",
			    "fieldSchema": {
			        "id": "INT64",
			        "source_image": "STRING",
			        "namespace": "STRING",
			        "source_image_vector": "MULTI_FLOAT"
			    },
			    "vectorIndex": [
			        {
			            "indexName": "test_index_1",
			            "vectorField": "source_image_vector",
			            "vectorIndexType": "hnsw",
			            "dimension": "512",
			            "distanceType": "InnerProduct"
			        }
			    ]
			}
			""";

	/**
	 * Whether to initialize the schema for the vector store.
	 */
	private boolean initializeSchema = false;

	/**
	 * The name of the index to store the vectors.
	 */
	private String tableName = DEFAULT_TABLE_NAME;

	/**
	 * The primary key field for the index.
	 */
	private String primaryKeyField = "id";

	/**
	 * The index name for the vector store.
	 */
	private String index = "saa_default_index";

	public String getSimilarityFunction() {
		return similarityFunction;
	}

	public OpenSearchVectorStoreOptions setSimilarityFunction(String similarityFunction) {
		this.similarityFunction = similarityFunction;
		return this;
	}

	private String similarityFunction = "cosinesimil";

	/**
	 * The fields to be output in the search results.
	 */
	private List<String> outputFields = DEFAULT_OUTPUT_FIELDS;

	/**
	 * The number of dimensions in the vector.
	 */
	private int dimensions = 1536;

	public boolean isInitializeSchema() {
		return initializeSchema;
	}

	public OpenSearchVectorStoreOptions setInitializeSchema(boolean initializeSchema) {
		this.initializeSchema = initializeSchema;
		return this;
	}

	public String getIndex() {
		return index;
	}

	public OpenSearchVectorStoreOptions setIndex(String index) {
		this.index = index;
		return this;
	}

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
