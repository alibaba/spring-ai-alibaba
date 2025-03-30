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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for SQLite document reader Note: Requires a running SQLite instance with test
 * data
 *
 * @author jens papenhagen
 **/
@EnabledIfSystemProperty(named = "sqlite.host", matches = ".+")
public class SQLiteDocumentReaderTest {

    private SQLiteResource sqLiteResource;

    private SQLiteDocumentReader reader;

    @BeforeEach
    void setUp() {
        // Read SQLite connection information from system properties
        String host = System.getProperty("sqlite.host", "localhost");
        int port = Integer.parseInt(System.getProperty("sqlite.port", "3306"));
        String database = System.getProperty("sqlite.database", "sqLite"); // Use default
        // SQLite
        // database
        String username = System.getProperty("sqlite.username", "root");
        String password = System.getProperty("sqlite.password", "root");
        String query = System.getProperty("sqlite.query", "SELECT * FROM user LIMIT 10;"); // Use
        // user
        // table
        // in
        // sqLite
        // database

        // Read content and metadata columns from system properties
        String contentColumnsStr = System.getProperty("sqlite.content.columns", "User,Host");
        String metadataColumnsStr = System.getProperty("sqlite.metadata.columns", "User,Host");

        List<String> contentColumns = Arrays.asList(contentColumnsStr.split(","));
        List<String> metadataColumns = Arrays.asList(metadataColumnsStr.split(","));

        // Setup test SQLite resource
        sqLiteResource = new sqLiteResource(host, port, database, username, password, query, contentColumns,
                metadataColumns);

        reader = new SQLiteDocumentReader(sqLiteResource);
    }

    @Test
    void testGetDocuments() {
        // This test requires a running SQLite instance with test data
        // You may need to modify the connection details and query using system
        // properties:
        // -Dsqlite.host=your_host -Dsqlite.port=your_port -Dsqlite.database=your_db
        // -Dsqlite.username=your_user -Dsqlite.password=your_pass

        List<Document> documents = reader.get();

        // Basic assertions
        assertNotNull(documents);
        assertFalse(documents.isEmpty());

        // Test first document
        Document firstDoc = documents.get(0);
        assertNotNull(firstDoc);

        // Test document content
        String content = firstDoc.getText();
        assertNotNull(content);
    }

    @Test
    void testInvalidConnection() {
        // Test with invalid credentials
        SQLiteResource invalidResource = new SQLiteResource(
                "invalid_host",
                3306,
                "invalid_db",
                "invalid_user",
                "invalid_pass",
                "SELECT * FROM test_table",
                null,
                null);

        SQLiteDocumentReader invalidReader = new SQLiteDocumentReader(invalidResource);

        // Should throw RuntimeException
        assertThrows(RuntimeException.class, invalidReader::get);
    }

}
