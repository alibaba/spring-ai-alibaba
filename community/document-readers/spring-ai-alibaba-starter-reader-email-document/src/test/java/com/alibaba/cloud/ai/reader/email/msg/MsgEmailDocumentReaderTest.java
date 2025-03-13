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
package com.alibaba.cloud.ai.reader.email.msg;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for MSG Email Document Reader
 *
 * @author brianxiadong
 * @since 2024-01-19
 */
class MsgEmailDocumentReaderTest {

	@Test
	void should_read_msg_file() throws IOException {
		// Given
		ClassPathResource emailResource = new ClassPathResource("strangeDate.msg");
		MsgEmailDocumentReader reader = new MsgEmailDocumentReader(emailResource.getFile().getAbsolutePath());

		// When
		List<Document> documents = reader.get();

		// Then
		assertNotNull(documents);
		assertEquals(1, documents.size());

		Document emailDoc = documents.get(0);
		Map<String, Object> metadata = emailDoc.getMetadata();

		// Verify metadata
		assertNotNull(metadata);
		assertNotEquals(0, metadata.size());

		// Verify content
		String content = emailDoc.getText();
		assertNotEquals("", content);
	}

	@Test
	void should_read_msg_file2() throws IOException {
		// Given
		ClassPathResource emailResource = new ClassPathResource("unicode.msg");
		MsgEmailDocumentReader reader = new MsgEmailDocumentReader(emailResource.getFile().getAbsolutePath());

		// When
		List<Document> documents = reader.get();

		// Then
		assertNotNull(documents);
		assertEquals(1, documents.size());

		Document emailDoc = documents.get(0);
		Map<String, Object> metadata = emailDoc.getMetadata();

		// Verify metadata
		assertNotNull(metadata);
		assertNotEquals(0, metadata.size());

		// Verify content
		String content = emailDoc.getText();
		assertNotEquals("", content);
	}

	@Test
	void should_handle_missing_file() {
		// Given
		MsgEmailDocumentReader reader = new MsgEmailDocumentReader("non-existent.msg");

		// When & Then
		assertThrows(RuntimeException.class, reader::get);
	}

	@Test
	void should_handle_invalid_msg_file() throws IOException {
		// Given
		ClassPathResource invalidResource = new ClassPathResource("1.eml");
		MsgEmailDocumentReader reader = new MsgEmailDocumentReader(invalidResource.getFile().getAbsolutePath());

		// When & Then
		assertThrows(RuntimeException.class, reader::get);
	}

}
