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
package com.alibaba.cloud.ai.parser.apache.pdfbox;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ApachePdfBoxDocumentParserTest {

	@Test
	void should_parse_pdf_file() {
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test-file.pdf")) {
			DocumentParser parser = new ApachePdfBoxDocumentParser();
			Document document = parser.parse(inputStream).get(0);

			assertThat(document.getContent()).isEqualToIgnoringWhitespace("test content");
			assertThat(document.getMetadata()).isEmpty();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void should_parse_pdf_file_include_metadata() {
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test-file.pdf")) {
			DocumentParser parser = new ApachePdfBoxDocumentParser(true);
			Document document = parser.parse(inputStream).get(0);

			assertThat(document.getContent()).isEqualToIgnoringWhitespace("test content");
			assertThat(document.getMetadata()).containsEntry("Author", "ljuba")
				.containsEntry("Creator", "WPS Writer")
				.containsEntry("CreationDate", "D:20230608171011+15'10'")
				.containsEntry("SourceModified", "D:20230608171011+15'10'");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}