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
package com.alibaba.cloud.ai.reader.mbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for MboxDocumentReader functionality
 *
 * @author brianxiadong
 */
public class MboxDocumentReaderTest {

	@TempDir
	Path tempDir;

	private static final String SAMPLE_MBOX = "sample.mbox";

	private static final String INVALID_MBOX = "invalid.mbox";

	private File sampleMboxFile;

	private File invalidMboxFile;

	private SimpleDateFormat dateFormat;

	@BeforeEach
	void setUp() throws IOException {
		// Create temporary test files
		sampleMboxFile = tempDir.resolve(SAMPLE_MBOX).toFile();
		invalidMboxFile = tempDir.resolve(INVALID_MBOX).toFile();

		// Copy test resource files from classpath to temporary directory
		FileCopyUtils.copy(new ClassPathResource(SAMPLE_MBOX).getInputStream(),
				Files.newOutputStream(sampleMboxFile.toPath()));

		FileCopyUtils.copy(new ClassPathResource(INVALID_MBOX).getInputStream(),
				Files.newOutputStream(invalidMboxFile.toPath()));

		// Initialize date formatter
		dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
	}

	/**
	 * Test reading plain text email
	 */
	@Test
	void testPlainTextEmail() {
		// Create reader instance
		MboxDocumentReader reader = new MboxDocumentReader(sampleMboxFile.getAbsolutePath(), 1,
				MboxDocumentReader.DEFAULT_MESSAGE_FORMAT);

		// Get document list
		List<Document> documents = reader.get();

		// Verify only one email was read
		assertEquals(1, documents.size(), "Should only read one email");

		// Get the first email
		Document doc = documents.get(0);
		Map<String, Object> metadata = doc.getMetadata();

		// Verify metadata
		assertEquals("Plain Text Email", metadata.get("subject"));
		assertEquals("Test Sender <test@example.com>", metadata.get("from"));
		assertEquals("recipient@example.com", metadata.get("to"));
		assertEquals("<test123@example.com>", doc.getId());

		// Verify plain text message content
		String content = doc.getContent();
		assertTrue(content.contains("This is a plain text email message"));
		assertTrue(content.contains("Best regards"));
	}

	/**
	 * Test reading HTML format email
	 */
	@Test
	void testHtmlEmail() {
		// Create reader instance and set to read first two emails
		MboxDocumentReader reader = new MboxDocumentReader(sampleMboxFile.getAbsolutePath(), 2,
				MboxDocumentReader.DEFAULT_MESSAGE_FORMAT);
		List<Document> documents = reader.get();

		// Verify two emails were read
		assertEquals(2, documents.size(), "Should read two emails");

		// Get the second HTML email
		Document doc = documents.get(1);
		Map<String, Object> metadata = doc.getMetadata();

		// Verify metadata
		assertEquals("HTML Email", metadata.get("subject"));
		assertEquals("Test Sender <test@example.com>", metadata.get("from"));
		assertEquals("recipient@example.com", metadata.get("to"));
		assertEquals("<test124@example.com>", doc.getId());

		// Verify HTML content was correctly parsed to text
		String content = doc.getContent();

		// Verify heading was correctly extracted
		assertTrue(content.contains("HTML Email Test"), "Should contain the h1 heading text");

		// Verify paragraph content was correctly extracted
		assertTrue(content.contains("This is a HTML formatted email message"),
				"Should contain the first paragraph text");
		assertTrue(content.contains("It contains styled text and multiple paragraphs"),
				"Should contain the second paragraph text");

		// Verify HTML tags were correctly removed
		assertFalse(content.contains("<html>"), "Should not contain html tag");
		assertFalse(content.contains("<head>"), "Should not contain head tag");
		assertFalse(content.contains("<body>"), "Should not contain body tag");
		assertFalse(content.contains("<h1>"), "Should not contain h1 tag");
		assertFalse(content.contains("<p>"), "Should not contain p tag");
		assertFalse(content.contains("<b>"), "Should not contain b tag");
		assertFalse(content.contains("<i>"), "Should not contain i tag");

		// Verify formatted content structure
		String expectedFormat = String.format(MboxDocumentReader.DEFAULT_MESSAGE_FORMAT, metadata.get("date"),
				metadata.get("from"), metadata.get("to"), metadata.get("subject"),
				"HTML Email Test This is a HTML formatted email message It contains styled text and multiple paragraphs"
					.trim());
	}

	/**
	 * Test reading multipart email (multipart/alternative)
	 */
	@Test
	void testMultipartEmail() {
		// Create reader instance and set to read third email
		MboxDocumentReader reader = new MboxDocumentReader(sampleMboxFile.getAbsolutePath(), 3,
				MboxDocumentReader.DEFAULT_MESSAGE_FORMAT);
		List<Document> documents = reader.get();

		// Get multipart email
		Document doc = documents.get(2);
		Map<String, Object> metadata = doc.getMetadata();

		// Verify metadata
		assertEquals("Multipart Email", metadata.get("subject"));
		assertEquals("<test125@example.com>", doc.getId());

		// Verify content - should prefer HTML part
		String content = doc.getContent();
		assertTrue(content.contains("Multipart Email Test"));
		assertTrue(content.contains("This is the HTML version"));
		// Should not contain plain text part
		assertFalse(content.contains("This is the plain text version"));
	}

	/**
	 * Test reading all emails
	 */
	@Test
	void testReadAllEmails() {
		// Create reader instance with no limit
		MboxDocumentReader reader = new MboxDocumentReader(sampleMboxFile.getAbsolutePath());

		// Get all emails
		List<Document> documents = reader.get();

		// Verify total count
		assertEquals(3, documents.size(), "Should read all four emails");

		// Verify email IDs in order
		assertEquals("<test123@example.com>", documents.get(0).getId());
		assertEquals("<test124@example.com>", documents.get(1).getId());
		assertEquals("<test125@example.com>", documents.get(2).getId());
	}

	/**
	 * Test custom message format
	 */
	@Test
	void testCustomMessageFormat() {
		// Create reader with custom format
		String customFormat = "Email Details:\nSubject: %4$s\nSender: %2$s\nReceiver: %3$s\nDate: %1$s\n\nMessage:\n%5$s";
		MboxDocumentReader reader = new MboxDocumentReader(sampleMboxFile.getAbsolutePath(), 1, customFormat);

		// Read first email
		Document doc = reader.get().get(0);
		String content = doc.getContent();

		// Verify custom format
		assertTrue(content.startsWith("Email Details:"));
		assertTrue(content.contains("Subject: Plain Text Email"));
		assertTrue(content.contains("Sender: Test Sender <test@example.com>"));
	}

	/**
	 * Test invalid mbox file
	 */
	@Test
	void testInvalidMboxFile() {
		// Create reader instance
		MboxDocumentReader reader = new MboxDocumentReader(invalidMboxFile.getAbsolutePath());

		// Read file
		List<Document> documents = reader.get();

		// Verify result is empty list
		assertTrue(documents.isEmpty(), "Should return empty list for invalid mbox file");
	}

	/**
	 * Test non-existent file
	 */
	@Test
	void testNonExistentFile() {
		// Verify constructor throws exception
		assertThrows(IllegalArgumentException.class, () -> {
			new MboxDocumentReader("non_existent.mbox");
		});
	}

}
