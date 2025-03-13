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
package com.alibaba.cloud.ai.reader.mysql;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL document reader implementation Uses JDBC to connect and fetch data from MySQL
 *
 * @author brianxiadong
 **/
public class MySQLDocumentReader implements DocumentReader {

	private final MySQLResource mysqlResource;

	public MySQLDocumentReader(MySQLResource mysqlResource) {
		this.mysqlResource = mysqlResource;
	}

	@Override
	public List<Document> get() {
		List<Document> documents = new ArrayList<>();
		try {
			// Register MySQL JDBC driver
			Class.forName("com.mysql.cj.jdbc.Driver");

			// Create database connection
			try (Connection connection = createConnection()) {
				documents = executeQueryAndProcessResults(connection);
			}
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException("MySQL JDBC driver not found", e);
		}
		catch (SQLException e) {
			throw new RuntimeException("Error executing MySQL query: " + e.getMessage(), e);
		}
		return documents;
	}

	/**
	 * Create database connection
	 */
	private Connection createConnection() throws SQLException {
		return DriverManager.getConnection(mysqlResource.getJdbcUrl(), mysqlResource.getUsername(),
				mysqlResource.getPassword());
	}

	/**
	 * Execute query and process results
	 */
	private List<Document> executeQueryAndProcessResults(Connection connection) throws SQLException {
		List<Document> documents = new ArrayList<>();
		try (Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(mysqlResource.getQuery())) {

			List<String> columnNames = getColumnNames(resultSet.getMetaData());
			while (resultSet.next()) {
				Map<String, Object> rowData = extractRowData(resultSet, columnNames);
				String content = buildContent(rowData);
				Map<String, Object> metadata = buildMetadata(rowData);
				documents.add(new Document(content, metadata));
			}
		}
		return documents;
	}

	/**
	 * Get list of column names
	 */
	private List<String> getColumnNames(ResultSetMetaData metaData) throws SQLException {
		List<String> columnNames = new ArrayList<>();
		int columnCount = metaData.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			columnNames.add(metaData.getColumnName(i));
		}
		return columnNames;
	}

	/**
	 * Extract row data
	 */
	private Map<String, Object> extractRowData(ResultSet resultSet, List<String> columnNames) throws SQLException {
		Map<String, Object> rowData = new HashMap<>();
		for (int i = 0; i < columnNames.size(); i++) {
			String columnName = columnNames.get(i);
			Object value = resultSet.getObject(i + 1);
			rowData.put(columnName, value);
		}
		return rowData;
	}

	/**
	 * Build document content
	 */
	private String buildContent(Map<String, Object> rowData) {
		StringBuilder contentBuilder = new StringBuilder();
		List<String> contentColumns = mysqlResource.getTextColumns();

		if (contentColumns == null || contentColumns.isEmpty()) {
			// If no content columns specified, use all columns
			for (Map.Entry<String, Object> entry : rowData.entrySet()) {
				appendColumnContent(contentBuilder, entry.getKey(), entry.getValue());
			}
		}
		else {
			// Only use specified content columns
			for (String column : contentColumns) {
				if (rowData.containsKey(column)) {
					appendColumnContent(contentBuilder, column, rowData.get(column));
				}
			}
		}
		return contentBuilder.toString().trim();
	}

	/**
	 * Append column content
	 */
	private void appendColumnContent(StringBuilder builder, String column, Object value) {
		builder.append(column).append(": ").append(value).append("\n");
	}

	/**
	 * Build metadata
	 */
	private Map<String, Object> buildMetadata(Map<String, Object> rowData) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(MySQLResource.SOURCE, mysqlResource.getJdbcUrl());

		List<String> metadataColumns = mysqlResource.getMetadataColumns();
		if (metadataColumns != null) {
			for (String column : metadataColumns) {
				if (rowData.containsKey(column)) {
					metadata.put(column, rowData.get(column));
				}
			}
		}
		return metadata;
	}

}
