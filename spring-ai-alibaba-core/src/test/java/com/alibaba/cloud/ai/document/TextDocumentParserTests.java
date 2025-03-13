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
package com.alibaba.cloud.ai.document;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test cases for TextDocumentParser
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class TextDocumentParserTests {

	@Test
	void testParseWithDefaultCharset() {
		// Test parsing text with default UTF-8 charset
		String text = "Sample text content";
		TextDocumentParser parser = new TextDocumentParser();

		List<Document> documents = parser.parse(toInputStream(text));

		assertThat(documents).hasSize(1);
		assertThat(documents.get(0).getText()).isEqualTo(text);
	}

	@Test
	void testParseWithCustomCharset() {
		// Test parsing text with custom charset (e.g., ISO-8859-1)
		String text = "Sample text with special characters: é, à, ç";
		TextDocumentParser parser = new TextDocumentParser(StandardCharsets.ISO_8859_1);

		List<Document> documents = parser.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));

		assertThat(documents).hasSize(1);
		assertThat(documents.get(0).getText()).isEqualTo(text);
	}

	@Test
	void testParseEmptyText() {
		// Test parsing empty text should throw exception
		String text = "";
		TextDocumentParser parser = new TextDocumentParser();

		RuntimeException exception = assertThrows(RuntimeException.class, () -> parser.parse(toInputStream(text)));

		assertThat(exception).hasRootCauseInstanceOf(Exception.class);
	}

	@Test
	void testParseBlankText() {
		// Test parsing blank text (only whitespace) should throw exception
		String text = "   \n\t   ";
		TextDocumentParser parser = new TextDocumentParser();

		RuntimeException exception = assertThrows(RuntimeException.class, () -> parser.parse(toInputStream(text)));

		assertThat(exception).hasRootCauseInstanceOf(Exception.class);
	}

	@Test
	void testConstructorWithNullCharset() {
		// Test constructor with null charset should throw exception
		assertThrows(IllegalArgumentException.class, () -> new TextDocumentParser(null));
	}

	private InputStream toInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

}
