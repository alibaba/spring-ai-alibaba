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
package com.alibaba.cloud.ai.reader.huggingface.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HuggingFaceFSDocumentReader.
 *
 * @author brianxiadong
 */
class HuggingFaceFSDocumentReaderTests {

	@TempDir
	Path tempDir;

	@Test
	void testReadJsonlFile() throws Exception {
		// Given
		String jsonContent = """
				{"text": "Hello world", "label": "greeting"}
				{"text": "How are you?", "label": "question"}
				""";
		Path testFile = tempDir.resolve("test.jsonl");
		Files.write(testFile, jsonContent.getBytes(StandardCharsets.UTF_8));

		HuggingFaceFSDocumentReader reader = new HuggingFaceFSDocumentReader(testFile.toString());

		// When
		List<Document> documents = reader.get();

		// Then
		assertThat(documents).hasSize(2);
		Document firstDoc = documents.get(0);
		assertNotNull(firstDoc);
		assertEquals(testFile.toString(), firstDoc.getMetadata().get(HuggingFaceFSDocumentReader.SOURCE));
		assertTrue(firstDoc.getText().contains("Hello world"));
	}

	@Test
	void testReadGzippedJsonlFile() throws Exception {
		// Given
		String jsonContent = """
				{"text": "Compressed content", "label": "test"}
				""";
		byte[] gzippedContent = createGzippedContent(jsonContent);
		Path testFile = tempDir.resolve("test.jsonl.gz");
		Files.write(testFile, gzippedContent);

		HuggingFaceFSDocumentReader reader = new HuggingFaceFSDocumentReader(testFile.toString());

		// When
		List<Document> documents = reader.get();

		// Then
		assertThat(documents).hasSize(1);
		Document doc = documents.get(0);
		assertNotNull(doc);
		assertEquals(testFile.toString(), doc.getMetadata().get(HuggingFaceFSDocumentReader.SOURCE));
		assertTrue(doc.getText().contains("Compressed content"));
	}

	private byte[] createGzippedContent(String content) throws Exception {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
			gzipStream.write(content.getBytes(StandardCharsets.UTF_8));
		}
		return byteStream.toByteArray();
	}

}
