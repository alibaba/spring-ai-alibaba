package com.alibaba.cloud.ai.parser.tika;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.apache.tika.exception.ZeroByteFileException;
import org.apache.tika.parser.AutoDetectParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.document.Document;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApacheTikaDocumentParserTest {

	@ParameterizedTest
	@ValueSource(strings = { "test-file.doc", "test-file.docx", "test-file.ppt", "test-file.pptx", "test-file.pdf" })
	void should_parse_doc_ppt_and_pdf_files(String fileName) {

		DocumentParser parser = new TikaDocumentParser();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		Document document = parser.parse(inputStream).get(0);

		assertThat(document.getContent()).isEqualToIgnoringWhitespace("test content");
		assertThat(document.getMetadata()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "test-file.xls", "test-file.xlsx" })
	void should_parse_xls_files(String fileName) {

		DocumentParser parser = new TikaDocumentParser(AutoDetectParser::new, null, null, null);
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		Document document = parser.parse(inputStream).get(0);

		assertThat(document.getContent()).isEqualToIgnoringWhitespace("Sheet1\ntest content\nSheet2\ntest content");
		assertThat(document.getMetadata()).isEmpty();
	}

	@Test
	void should_parse_files_stateless() {

		DocumentParser parser = new TikaDocumentParser();
		InputStream inputStream1 = getClass().getClassLoader().getResourceAsStream("test-file.xls");
		InputStream inputStream2 = getClass().getClassLoader().getResourceAsStream("test-file.xls");

		Document document1 = parser.parse(inputStream1).get(0);
		Document document2 = parser.parse(inputStream2).get(0);

		assertThat(document1.getContent()).isEqualToIgnoringWhitespace("Sheet1\ntest content\nSheet2\ntest content");
		assertThat(document2.getContent()).isEqualToIgnoringWhitespace("Sheet1\ntest content\nSheet2\ntest content");
		assertThat(document1.getMetadata()).isEmpty();
		assertThat(document2.getMetadata()).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "empty-file.txt", "blank-file.txt", "blank-file.docx", "blank-file.pptx"
	// "blank-file.xlsx" TODO
	})
	void should_throw_BlankDocumentException(String fileName) {

		DocumentParser parser = new TikaDocumentParser();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

		assertThatThrownBy(() -> parser.parse(inputStream)).isExactlyInstanceOf(ZeroByteFileException.class);
	}

}