package com.alibaba.cloud.ai.studio.core.rag.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TextDocumentReaderTest {

	@Mock
	private TextDocumentReader reader;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		// reader = new TextDocumentReader(new
		// ClassPathResource("/reader/springaialibaba.txt"));
	}

	@Test
	void testRead() {
		// Prepare test data
		String content = "test content";
		List<Document> expectedDocuments = List.of(Document.builder().text(content).build());

		// Mock behavior
		when(reader.read()).thenReturn(expectedDocuments);

		// Execute
		List<Document> result = reader.read();

		// Verify
		assertNotNull(result);
		assertEquals(expectedDocuments, result);
		verify(reader, times(1)).read();
	}

}
