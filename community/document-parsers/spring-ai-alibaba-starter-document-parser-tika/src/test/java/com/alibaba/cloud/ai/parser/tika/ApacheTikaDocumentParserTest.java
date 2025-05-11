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
package com.alibaba.cloud.ai.parser.tika;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.apache.tika.parser.AutoDetectParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.document.Document;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author HeYQ
 * @author brianxiadong
 */
class ApacheTikaDocumentParserTest {

	@ParameterizedTest
	@ValueSource(strings = { "test-file.doc", "test-file.docx", "test-file.ppt", "test-file.pptx", "test-file.pdf" })
	void should_parse_doc_ppt_and_pdf_files(String fileName) {

		DocumentParser parser = new TikaDocumentParser();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		Document document = parser.parse(inputStream).get(0);

		assertThat(document.getText()).isEqualToIgnoringWhitespace("test content");
		assertThat(document.getMetadata()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "test-file.xls", "test-file.xlsx" })
	void should_parse_xls_files(String fileName) {

		DocumentParser parser = new TikaDocumentParser(AutoDetectParser::new, null, null, null);
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		Document document = parser.parse(inputStream).get(0);

		assertThat(document.getText()).isEqualToIgnoringWhitespace("Sheet1\ntest content\nSheet2\ntest content");
		assertThat(document.getMetadata()).isEmpty();
	}

	@Test
	void should_parse_files_stateless() {

		DocumentParser parser = new TikaDocumentParser();
		InputStream inputStream1 = getClass().getClassLoader().getResourceAsStream("test-file.xls");
		InputStream inputStream2 = getClass().getClassLoader().getResourceAsStream("test-file.xls");

		Document document1 = parser.parse(inputStream1).get(0);
		Document document2 = parser.parse(inputStream2).get(0);

		assertThat(document1.getText()).isEqualToIgnoringWhitespace("Sheet1\ntest content\nSheet2\ntest content");
		assertThat(document2.getText()).isEqualToIgnoringWhitespace("Sheet1\ntest content\nSheet2\ntest content");
		assertThat(document1.getMetadata()).isEmpty();
		assertThat(document2.getMetadata()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "empty-file.txt", "blank-file.txt"
	// "blank-file.xlsx" TODO
	})
	void should_throw_BlankDocumentException(String fileName) {

		DocumentParser parser = new TikaDocumentParser();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		assertThatThrownBy(() -> parser.parse(inputStream)).isExactlyInstanceOf(RuntimeException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "example-utf8.html" })
	void should_parse_html_file(String fileName) {

		DocumentParser parser = new TikaDocumentParser(AutoDetectParser::new, null, null, null);
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		Document document = parser.parse(inputStream).get(0);

		System.out.println(document.getText());

	}

	@ParameterizedTest
	@ValueSource(strings = { "factbook.xml" })
	void should_parse_xml_file(String fileName) {

		DocumentParser parser = new TikaDocumentParser(AutoDetectParser::new, null, null, null);
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		Document document = parser.parse(inputStream).get(0);
		System.out.println(document.getText());
		assertThat(document.getMetadata()).isEmpty();
	}

	/**
	 * Test blank document files that contain newline characters. These files appear to be
	 * blank but actually contain special characters like newlines. This requires separate
	 * handling from completely empty files.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "blank-file.docx", "blank-file.pptx" })
	void should_handle_blank_files_with_newlines(String fileName) {
		DocumentParser parser = new TikaDocumentParser();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		// These files will be successfully parsed, but content only contains whitespace
		// and newlines
		Document document = parser.parse(inputStream).get(0);

		// Verify that document content only contains whitespace characters (spaces,
		// tabs, newlines, etc.)
		assertThat(document.getText().trim()).isEmpty();
		assertThat(document.getMetadata()).isEmpty();
	}

}
