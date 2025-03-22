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
package com.alibaba.cloud.ai.reader.sqlite;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite document reader implementation Uses JDBC to connect and fetch data from SQLite,
 * use SQLite4j from roastedroot to run in pure Java.
 *
 * @author jens papenhagen
 **/
public class SQLiteDocumentReader implements DocumentReader {

    private final sqLiteResource sqLiteResource;

    public SQLiteDocumentReader(SQLiteResource sqLiteResource) {
        this.sqLiteResource = sqLiteResource;
    }

    @Override
    public List<Document> get() {
        List<Document> documents = new ArrayList<>();
        try {
            // Create database connection
            try (Connection connection = createConnection()) {
                documents = executeQueryAndProcessResults(connection);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        } catch (SQLException e) {
            throw new RuntimeException("Error executing SQLite query: " + e.getMessage(), e);
        }
        return documents;
    }

    /**
     * Create database connection
     */
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(sqLiteResource.getJdbcUrl(), sqLiteResource.getUsername(), sqLiteResource.getPassword());
    }

    /**
     * Execute query and process results
     */
    private List<Document> executeQueryAndProcessResults(final Connection connection) throws SQLException {
        final List<Document> documents = new ArrayList<>();
        try (final Statement statement = connection.createStatement();
             final ResultSet resultSet = statement.executeQuery(sqLiteResource.getQuery())) {

            final List<String> columnNames = getColumnNames(resultSet.getMetaData());
            while (resultSet.next()) {
                final Map<String, Object> rowData = extractRowData(resultSet, columnNames);
                final String content = buildContent(rowData);
                final Map<String, Object> metadata = buildMetadata(rowData);
                documents.add(new Document(content, metadata));
            }
        }
        return documents;
    }

    /**
     * Get list of column names
     */
    private List<String> getColumnNames(final ResultSetMetaData metaData) throws SQLException {
        final List<String> columnNames = new ArrayList<>();
        final int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i));
        }
        return columnNames;
    }

    /**
     * Extract row data
     */
    private Map<String, Object> extractRowData(final ResultSet resultSet, final List<String> columnNames) throws SQLException {
        final Map<String, Object> rowData = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            final String columnName = columnNames.get(i);
            final Object value = resultSet.getObject(i + 1);
            rowData.put(columnName, value);
        }
        return rowData;
    }

    /**
     * Build document content
     */
    private String buildContent(final Map<String, Object> rowData) {
        final StringBuilder contentBuilder = new StringBuilder();
        final List<String> contentColumns = sqLiteResource.getTextColumns();

        if (contentColumns == null || contentColumns.isEmpty()) {
            // If no content columns specified, use all columns
            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                appendColumnContent(contentBuilder, entry.getKey(), entry.getValue());
            }
        } else {
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
    private void appendColumnContent(final StringBuilder builder, final String column, final Object value) {
        builder.append(column).append(": ").append(value).append("\n");
    }

    /**
     * Build metadata
     */
    private Map<String, Object> buildMetadata(final Map<String, Object> rowData) {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put(sqLiteResource.SOURCE, sqLiteResource.getJdbcUrl());

        final List<String> metadataColumns = sqLiteResource.getMetadataColumns();
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
