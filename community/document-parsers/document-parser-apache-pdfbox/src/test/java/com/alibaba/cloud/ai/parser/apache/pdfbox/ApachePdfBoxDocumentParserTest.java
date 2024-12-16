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