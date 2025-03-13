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
package com.alibaba.cloud.ai.transformer.splitter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test cases for SentenceSplitter. Tests include default constructor, custom chunk size,
 * text splitting functionality, and handling of various input scenarios.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class SentenceSplitterTests {

	private SentenceSplitter splitter;

	private static final int CUSTOM_CHUNK_SIZE = 100;

	@BeforeEach
	void setUp() {
		// Initialize with default chunk size
		splitter = new SentenceSplitter();
	}

	/**
	 * Test default constructor. Verifies that splitter can be created with default chunk
	 * size.
	 */
	@Test
	void testDefaultConstructor() {
		SentenceSplitter defaultSplitter = new SentenceSplitter();
		assertThat(defaultSplitter).isNotNull();
	}

	/**
	 * Test constructor with custom chunk size. Verifies that splitter can be created with
	 * specified chunk size.
	 */
	@Test
	void testCustomChunkSizeConstructor() {
		SentenceSplitter customSplitter = new SentenceSplitter(CUSTOM_CHUNK_SIZE);
		assertThat(customSplitter).isNotNull();
	}

	/**
	 * Test splitting simple sentences. Verifies basic sentence splitting functionality.
	 */
	@Test
	void testSplitSimpleSentences() {
		String text = "This is a test. This is another test. And this is a third test.";
		Document doc = new Document(text);
		List<Document> documents = splitter.apply(Collections.singletonList(doc));

		assertThat(documents).isNotNull();
		assertThat(documents).hasSize(1);
		assertThat(documents.get(0).getText()).contains("This is a test", "This is another test",
				"And this is a third test");
	}

	/**
	 * Test splitting empty text. Verifies handling of empty input.
	 */
	@Test
	void testSplitEmptyText() {
		Document doc = new Document("");
		List<Document> documents = splitter.apply(Collections.singletonList(doc));
		assertThat(documents).isEmpty();
	}

	/**
	 * Test splitting text with special characters. Verifies handling of text with various
	 * punctuation and special characters.
	 */
	@Test
	void testSplitTextWithSpecialCharacters() {
		String text = "Hello, world! How are you? I'm doing great... This is a test; with various punctuation.";
		Document doc = new Document(text);
		List<Document> documents = splitter.apply(Collections.singletonList(doc));

		assertThat(documents).isNotNull();
		assertThat(documents).hasSize(1);
		assertThat(documents.get(0).getText()).contains("Hello, world", "How are you", "I'm doing great",
				"This is a test");
	}

	/**
	 * Test splitting long text. Verifies handling of text that exceeds default chunk
	 * size.
	 */
	@Test
	void testSplitLongText() {
		// Generate a very long text that will exceed the default chunk size (1024
		// tokens)
		StringBuilder longText = new StringBuilder();
		String longSentence = "This is a very long sentence with many words that will contribute to the total token count and eventually force the text to be split into multiple chunks because it exceeds the default chunk size limit of 1024 tokens. ";
		// Repeat the sentence enough times to ensure we exceed the chunk size
		for (int i = 0; i < 50; i++) {
			longText.append(longSentence);
		}
		Document doc = new Document(longText.toString());

		List<Document> documents = splitter.apply(Collections.singletonList(doc));

		// Verify that the text was split into multiple documents
		assertThat(documents).isNotNull();
		assertThat(documents).hasSizeGreaterThan(1);
		// Verify that each document contains part of the original text
		documents.forEach(document -> assertThat(document.getText()).contains("This is a very long sentence"));
	}

	/**
	 * Test splitting text with multiple line breaks. Verifies handling of text with
	 * various types of line breaks.
	 */
	@Test
	void testSplitTextWithLineBreaks() {
		String text = "First sentence.\nSecond sentence.\r\nThird sentence.\rFourth sentence.";
		Document doc = new Document(text);
		List<Document> documents = splitter.apply(Collections.singletonList(doc));

		assertThat(documents).isNotNull();
		assertThat(documents.get(0).getText()).contains("First sentence", "Second sentence", "Third sentence",
				"Fourth sentence");
	}

	/**
	 * Test splitting text with single character sentences. Verifies handling of very
	 * short sentences.
	 */
	@Test
	void testSplitSingleCharacterSentences() {
		String text = "A. B. C. D.";
		Document doc = new Document(text);
		List<Document> documents = splitter.apply(Collections.singletonList(doc));

		assertThat(documents).isNotNull();
		assertThat(documents).hasSize(1);
		assertThat(documents.get(0).getText()).contains("A", "B", "C", "D");
	}

	/**
	 * Test splitting multiple documents. Verifies handling of multiple input documents.
	 */
	@Test
	void testSplitMultipleDocuments() {
		List<Document> inputDocs = new ArrayList<>();
		inputDocs.add(new Document("First document. With multiple sentences."));
		inputDocs.add(new Document("Second document. Also with multiple sentences."));

		List<Document> documents = splitter.apply(inputDocs);
		assertThat(documents).isNotNull();
		assertThat(documents).hasSizeGreaterThan(1);
	}

}
