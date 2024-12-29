package com.alibaba.cloud.ai.parser.apache.pdfbox;

import org.junit.jupiter.api.Test;

import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;

/**
 * @author HeYQ
 * @since 2024-12-24 00:52
 */

public class ParagraphPdfDocumentParserTests {

	@Test
	public void testPdfWithoutToc() throws IOException {

		String content = new ParagraphPdfDocumentParser(PdfDocumentReaderConfig.builder()
			.withPageTopMargin(0)
			.withPageBottomMargin(0)
			.withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
				.withNumberOfTopTextLinesToDelete(0)
				.withNumberOfBottomTextLinesToDelete(3)
				.withNumberOfTopPagesToSkipBeforeDelete(0)
				.build())
			.withPagesPerDocument(1)
			.build()).parse(new DefaultResourceLoader().getResource("classpath:/sample1.pdf").getInputStream())
			.get(0)
			.getContent();
		System.out.println(content);
	}

}
