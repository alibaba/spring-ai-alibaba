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
package com.alibaba.cloud.ai.parser.bibtex;

import com.alibaba.cloud.ai.document.DocumentParser;
import com.alibaba.cloud.ai.parser.apache.pdfbox.PagePdfDocumentParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for BibtexDocumentParser. Tests the functionality of parsing BibTeX files
 * and extracting document content.
 *
 * @author HeYQ
 * @author brianxiadong
 * @since 2025-01-02 23:15
 */

public class BibtexDocumentParserTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Test that verifies the parser can correctly parse a BibTeX file. This test uses a
	 * sample BibTeX file (wiley.bib) and checks if the parser can extract the content
	 * properly.
	 * @param fileName The name of the BibTeX file to parse
	 */
	@ParameterizedTest
	@ValueSource(strings = { "wiley.bib" })
	void should_parse_bibtex_file(String fileName) {
		// Create a BibtexDocumentParser with custom configuration
		DocumentParser parser = new BibtexDocumentParser("UTF-8", 10, 2, null,
				new PagePdfDocumentParser(PdfDocumentReaderConfig.builder()
					.withPageTopMargin(0)
					.withPageBottomMargin(0)
					.withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
						.withNumberOfTopTextLinesToDelete(0)
						.withNumberOfBottomTextLinesToDelete(3)
						.withNumberOfTopPagesToSkipBeforeDelete(0)
						.build())
					.withPagesPerDocument(1)
					.build()));

		// Load the BibTeX file from the classpath resources
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		// Parse the BibTeX file and get the documents
		List<Document> documents = parser.parse(inputStream);

		// Verify that documents were parsed
		assertThat(documents).isNotEmpty();
		logger.info("Parsed {} documents from {}", documents.size(), fileName);

		// Verify each document
		for (Document document : documents) {
			// Log document content for debugging
			logger.info("Document text: {}", document.getText());

			// Verify document has text content
			assertThat(document.getText()).isNotNull();

			// Verify document has metadata
			Map<String, Object> metadata = document.getMetadata();
			assertThat(metadata).isNotEmpty();

			// Log metadata for debugging
			logger.info("Document metadata: {}", metadata);
		}
	}

}
