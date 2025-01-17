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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for MySQL document reader Note: Requires a running MySQL instance with test
 * data
 *
 * @author brianxiadong
 **/
public class MySQLDocumentReaderTest {

	private MySQLResource mysqlResource;

	private MySQLDocumentReader reader;

	@BeforeEach
	void setUp() {
		// Setup test MySQL resource
		// Note: These are test credentials, change them according to your environment
		mysqlResource = new MySQLResource("localhost", 3306, "demo1", "root", "root",
				"SELECT * FROM user_table LIMIT 10;", Arrays.asList("username", "email"), // content
																							// columns
				Arrays.asList("username", "email") // metadata columns
		);

		reader = new MySQLDocumentReader(mysqlResource);
	}

	@Test
	void testGetDocuments() {
		// This test requires a running MySQL instance with test data
		// You may need to modify the connection details and query in setUp()

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
		MySQLResource invalidResource = new MySQLResource("invalid_host", 3306, "invalid_db", "invalid_user",
				"invalid_pass", "SELECT * FROM test_table", null, null);

		MySQLDocumentReader invalidReader = new MySQLDocumentReader(invalidResource);

		// Should throw RuntimeException
		assertThrows(RuntimeException.class, invalidReader::get);
	}

}
