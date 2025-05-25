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
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;

import java.io.InputStream;

/**
 * @author HeYQ
 * @since 2025-01-02 23:15
 */

public class BibtexDocumentParserText {

	@ParameterizedTest
	@ValueSource(strings = { "wiley.bib" })
	void should_parse_xml_file(String fileName) {
		// DocumentParser parser = new BibtexDocumentParser();
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
		;
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
		for (Document document : parser.parse(inputStream)) {
			System.out.println(document.getText());
		}

	}

}
