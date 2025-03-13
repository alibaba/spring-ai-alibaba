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
package com.alibaba.cloud.ai.reader.chatgpt.data;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for ChatGptDataDocumentReader
 *
 * @author brianxiadong
 */
class ChatGptDataDocumentReaderTests {

	// Path to test file
	private static final String TEST_FILE_PATH = "src/test/resources/conversations.json";

	@Test
	void shouldLoadAllDocuments() {
		// Create reader with fixed file path
		ChatGptDataDocumentReader reader = new ChatGptDataDocumentReader(TEST_FILE_PATH);
		List<Document> documents = reader.get();

		assertThat(documents).isNotEmpty();
	}

	@Test
	void shouldLoadLimitedDocuments() {
		// Load only 2 records
		ChatGptDataDocumentReader reader = new ChatGptDataDocumentReader(TEST_FILE_PATH, 2);
		List<Document> documents = reader.get();

		assertThat(documents).hasSize(2);
	}

	@Test
	void shouldThrowExceptionForInvalidFile() {
		String invalidPath = "non-existent-file.json";
		ChatGptDataDocumentReader reader = new ChatGptDataDocumentReader(invalidPath);

		assertThrows(RuntimeException.class, () -> {
			reader.get();
		});
	}

}
