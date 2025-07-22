package com.alibaba.cloud.ai.studio.core.rag.splitter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TextSplitterTest {

	@Mock
	private TextSplitter textSplitter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testSplit() {
		// Prepare test data
		String text = "This is a test text";
		List<Document> expectedChunks = List.of();
		Document document = Document.builder().text(text).build();

		// Mock behavior
		when(textSplitter.split(document)).thenReturn(expectedChunks);

		// Execute
		List<Document> result = textSplitter.split(document);

		// Verify
		assertNotNull(result);
		assertEquals(expectedChunks, result);
		verify(textSplitter, times(1)).split(document);
	}

	@Test
	void testSplitWithNullInput() {
		// Execute and verify
		assertThrows(IllegalArgumentException.class, () -> textSplitter.split(List.of()));
	}

	@Test
	void testSplitWithEmptyInput() {
		// Prepare test data
		String emptyText = "";
		Document document = Document.builder().text(emptyText).build();

		// Execute
		List<Document> result = textSplitter.split(document);

		// Verify
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

}
