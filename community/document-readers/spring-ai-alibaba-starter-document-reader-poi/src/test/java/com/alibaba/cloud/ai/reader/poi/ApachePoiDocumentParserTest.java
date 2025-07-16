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
package com.alibaba.cloud.ai.reader.poi;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ApachePoiDocumentParserTest {

	@ParameterizedTest
	@ValueSource(strings = { "test-file.doc", "test-file.docx", "test-file.ppt", "test-file.pptx" })
	void should_parse_doc_and_ppt_files(String fileName) {

		DocumentReader reader = new PoiDocumentReader(fileName);
		List<Document> documents = reader.get();
		Document document = documents.get(0);
		assertThat(document.getText()).isEqualToIgnoringWhitespace("test content");
		System.out.println(document.getMetadata());
	}

	@ParameterizedTest
	@ValueSource(strings = { "test-file.xls", "test-file.xlsx" })
	void should_parse_xls_files(String fileName) {

		DocumentReader reader = new PoiDocumentReader(fileName);
		List<Document> documents = reader.get();
		Document document = documents.get(0);

		assertThat(document.getText()).isEqualToIgnoringWhitespace("Sheet1\ntest content\nSheet2\ntest content");
		System.out.println(document.getMetadata());
	}

	@ParameterizedTest
	@ValueSource(strings = { "empty-file.txt", "blank-file.txt", "blank-file.docx", "blank-file.pptx"
	// "blank-file.xlsx" TODO
	})
	void should_throw_BlankDocumentException(String fileName) {

		DocumentReader reader = new PoiDocumentReader(fileName);

	}

}
