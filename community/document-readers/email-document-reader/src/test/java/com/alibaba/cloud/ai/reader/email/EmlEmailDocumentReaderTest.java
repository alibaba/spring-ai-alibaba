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
package com.alibaba.cloud.ai.reader.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for EmlEmailDocumentReader
 * Tests various email scenarios including:
 * 1. Basic email content reading
 * 2. Email with attachments
 * 3. Invalid email handling
 *
 * @author xiadong
 * @since 2024-01-06
 */
class EmlEmailDocumentReaderTest {

    private EmlEmailDocumentReader reader;

    @BeforeEach
    void setUp() {
        // Initialize reader with test email file
        ClassPathResource emailResource = new ClassPathResource("test-email.eml");
        reader = new EmlEmailDocumentReader(emailResource);
    }

    @Test
    void should_read_basic_email_content() {
        // When
        List<Document> documents = reader.get();

        // Then
        assertNotNull(documents);
        assertEquals(1, documents.size());

        Document emailDoc = documents.get(0);
        assertNotNull(emailDoc.getContent());
        assertTrue(emailDoc.getContent().contains("This is a test email"));
        
        // Verify metadata
        Map<String, Object> metadata = emailDoc.getMetadata();
        assertEquals("Test Email", metadata.get("subject"));
        assertEquals("sender@example.com", metadata.get("from"));
        assertEquals("Sender", metadata.get("from_name"));
        assertEquals("recipient@example.com", metadata.get("to"));
        assertEquals("Recipient", metadata.get("to_name"));
        
        // Verify date
        Object date = metadata.get("date");
        assertNotNull(date);
        assertTrue(date instanceof ZonedDateTime);
        ZonedDateTime dateTime = (ZonedDateTime) date;
        assertEquals(2024, dateTime.getYear());
        assertEquals(1, dateTime.getMonthValue());
        assertEquals(6, dateTime.getDayOfMonth());
    }

    @Test
    void should_process_email_with_attachments() {
        // Given
        ClassPathResource emailResource = new ClassPathResource("test-email-with-attachments.eml");
        reader = new EmlEmailDocumentReader(emailResource, true);

        // When
        List<Document> documents = reader.get();

        // Then
        assertNotNull(documents);
        assertEquals(3, documents.size()); // Main content + 2 attachments

        // Verify main content
        Document mainDoc = documents.get(0);
        assertTrue(mainDoc.getContent().contains("This is a test email for the EmlEmailDocumentReader with attachments"));
        assertEquals("Test Email with Attachments", mainDoc.getMetadata().get("subject"));

        // Verify text attachment
        Document textAttachment = documents.get(1);
        assertEquals("test.txt", textAttachment.getMetadata().get("filename"));
        assertEquals("text/plain; charset=\"UTF-8\"", textAttachment.getMetadata().get("content_type"));
        assertTrue(textAttachment.getContent().contains("This is a test attachment file"));

        // Verify JSON attachment
        Document jsonAttachment = documents.get(2);
        assertEquals("test.json", jsonAttachment.getMetadata().get("filename"));
        assertEquals("application/json; charset=\"UTF-8\"", jsonAttachment.getMetadata().get("content_type"));
        assertTrue(jsonAttachment.getContent().contains("\"name\": \"Test JSON\""));
    }

    @Test
    void should_handle_invalid_email_file() {
        // Given
        ClassPathResource invalidResource = new ClassPathResource("invalid.eml");
        reader = new EmlEmailDocumentReader(invalidResource);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> reader.get());
        assertTrue(exception.getMessage().contains("Failed to read email file"));
    }

    @Test
    void should_prefer_html_content_when_specified() {
        // Given
        ClassPathResource emailResource = new ClassPathResource("test-email.eml");
        reader = new EmlEmailDocumentReader(emailResource, false, true);

        // When
        List<Document> documents = reader.get();

        // Then
        assertNotNull(documents);
        assertEquals(1, documents.size());

        Document emailDoc = documents.get(0);
        String content = emailDoc.getContent();
        
        // Content should be parsed HTML without tags
        assertTrue(content.contains("Test Email"));
        assertTrue(content.contains("This is a test email"));
        assertFalse(content.contains("<html>"));
        assertFalse(content.contains("<body>"));
        assertFalse(content.contains("<h1>"));
    }

    @Test
    void should_use_plain_text_when_html_not_preferred() {
        // Given
        ClassPathResource emailResource = new ClassPathResource("test-email-with-attachments.eml");
        reader = new EmlEmailDocumentReader(emailResource, false, false);

        // When
        List<Document> documents = reader.get();

        // Then
        assertNotNull(documents);
        assertEquals(1, documents.size());

        Document emailDoc = documents.get(0);
        String content = emailDoc.getContent();
        
        // Should use plain text content if available
        assertTrue(content.contains("This is a test attachment file"));
    }
} 