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

		String allText = docs.stream().map(Document::getContent).collect(Collectors.joining(System.lineSeparator()));
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
