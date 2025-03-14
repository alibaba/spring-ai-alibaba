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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author HeYQ
 * @since 2024-12-24 00:52
 */
class PagePdfDocumentParserTests {

	@Test
	void classpathRead() throws IOException {

		PagePdfDocumentParser parser = new PagePdfDocumentParser(PdfDocumentReaderConfig.builder()
			.withPageTopMargin(0)
			.withPageBottomMargin(0)
			.withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
				.withNumberOfTopTextLinesToDelete(0)
				.withNumberOfBottomTextLinesToDelete(3)
				.withNumberOfTopPagesToSkipBeforeDelete(0)
				.build())
			.withPagesPerDocument(1)
			.build());

		List<Document> docs = parser
			.parse(new DefaultResourceLoader().getResource("classpath:/sample1.pdf").getInputStream());

		assertThat(docs).hasSize(4);

		String allText = docs.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));
		System.out.println(allText);

		// assertThat(allText).doesNotContain(
		// List.of("Page 1 of 4", "Page 2 of 4", "Page 3 of 4", "Page 4 of 4", "PDF
		// Bookmark Sample"));
	}

	@Test
	void testIndexOutOfBound() throws IOException {
		var documents = new PagePdfDocumentParser(PdfDocumentReaderConfig.builder()
			.withPageExtractedTextFormatter(ExtractedTextFormatter.builder().build())
			.withPagesPerDocument(1)
			.build()).parse(new DefaultResourceLoader().getResource("classpath:/sample2.pdf").getInputStream());

		assertThat(documents).hasSize(64);
	}

}
