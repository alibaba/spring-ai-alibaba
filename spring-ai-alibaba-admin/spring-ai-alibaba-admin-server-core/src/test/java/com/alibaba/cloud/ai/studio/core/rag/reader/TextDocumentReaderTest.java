/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.studio.core.rag.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import org.springframework.core.io.Resource;

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

	@ParameterizedTest
	@ValueSource(strings = { "text", "blank", "failure" })
	void shouldCloseActualFileStreamOnEveryReadPath(String mode, @TempDir Path dir) throws Exception {
		Path path = dir.resolve("lesson.txt");
		String content = mode.equals("blank") ? "  \n" : "Photosynthesis experiment instructions";
		Files.writeString(path, content, StandardCharsets.UTF_8);
		FileInputStream input = new FileInputStream(path.toFile());
		InputStream stream = mode.equals("failure") ? new FilterInputStream(input) {
			@Override
			public byte[] readAllBytes() throws IOException {
				throw new IOException("read failed");
			}
		} : input;
		Resource resource = mock(Resource.class);
		when(resource.getInputStream()).thenReturn(stream);
		try {
			TextDocumentReader realReader = new TextDocumentReader(resource);
			if (mode.equals("failure")) {
				RuntimeException error = assertThrows(RuntimeException.class, realReader::get);
				assertInstanceOf(IOException.class, error.getCause());
				assertEquals("read failed", error.getCause().getMessage());
			}
			else {
				List<Document> documents = realReader.get();
				if (mode.equals("blank")) {
					assertTrue(documents.isEmpty());
				}
				else {
					assertEquals(List.of(content), documents.stream().map(Document::getText).toList());
				}
			}
			assertFalse(input.getChannel().isOpen());
		}
		finally {
			stream.close();
		}
	}

}
