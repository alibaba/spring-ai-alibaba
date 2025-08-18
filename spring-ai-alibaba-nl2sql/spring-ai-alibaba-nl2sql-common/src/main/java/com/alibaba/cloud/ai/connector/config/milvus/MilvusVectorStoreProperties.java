
package com.alibaba.cloud.ai.connector.config.milvus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(MilvusVectorStoreProperties.CONFIG_PREFIX)
public class MilvusVectorStoreProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.nl2sql.milvus";

	private String idFieldName;

	private String contentFieldName;

	private String metadataFieldName;

	private String embeddingFieldName;

	private Integer embeddingDimension;

	private String host;

	private Integer port = 19530;

	private String collectionName;

	private String databaseName;

	private String username;

	private String password;

	private int searchParamsTopK = 100;

	private String metrics = "COSINE";

	private boolean initializeSchema = false;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getSearchParamsTopK() {
		return searchParamsTopK;
	}

	public void setSearchParamsTopK(int searchParamsTopK) {
		this.searchParamsTopK = searchParamsTopK;
	}

	public String getMetrics() {
		return metrics;
	}

	public void setMetrics(String metrics) {
		this.metrics = metrics;
	}

	public boolean isInitializeSchema() {
		return initializeSchema;
	}

	public void setInitializeSchema(boolean initializeSchema) {
		this.initializeSchema = initializeSchema;
	}

	public String getIdFieldName() {
		return idFieldName;
	}

	public void setIdFieldName(String idFieldName) {
		this.idFieldName = idFieldName;
	}

	public String getContentFieldName() {
		return contentFieldName;
	}

	public void setContentFieldName(String contentFieldName) {
		this.contentFieldName = contentFieldName;
	}

	public String getMetadataFieldName() {
		return metadataFieldName;
	}

	public void setMetadataFieldName(String metadataFieldName) {
		this.metadataFieldName = metadataFieldName;
	}

	public String getEmbeddingFieldName() {
		return embeddingFieldName;
	}

	public void setEmbeddingFieldName(String embeddingFieldName) {
		this.embeddingFieldName = embeddingFieldName;
	}

	public Integer getEmbeddingDimension() {
		return embeddingDimension;
	}

	public void setEmbeddingDimension(Integer embeddingDimension) {
		this.embeddingDimension = embeddingDimension;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

}
