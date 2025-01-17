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

import com.alibaba.cloud.ai.document.DocumentParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test cases for EmailDocumentReader.
 *
 * @author brianxiadong
 */
class EmailDocumentReaderTest {

    @Mock
    private DocumentParser mockParser;

    @TempDir
    Path tempDir;

    private Path emlFile;
    private Path msgFile;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        // Create test email files
        emlFile = tempDir.resolve("test.eml");
        msgFile = tempDir.resolve("test.msg");
        Files.write(emlFile, "Test EML content".getBytes());
        Files.write(msgFile, "Test MSG content".getBytes());
    }

    @Test
    void testConstructorValidation() {
        // Test null/empty file path
        assertThrows(IllegalArgumentException.class,
                () -> new EmailDocumentReader(null, mockParser, "single", false));
        assertThrows(IllegalArgumentException.class,
                () -> new EmailDocumentReader("", mockParser, "single", false));

        // Test null parser
        assertThrows(IllegalArgumentException.class,
                () -> new EmailDocumentReader(emlFile.toString(), null, "single", false));

        // Test null/empty mode
        assertThrows(IllegalArgumentException.class,
                () -> new EmailDocumentReader(emlFile.toString(), mockParser, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new EmailDocumentReader(emlFile.toString(), mockParser, "", false));
    }

    @Test
    void testFileValidation() {
        // Test non-existent file
        assertThrows(IllegalArgumentException.class,
                () -> new EmailDocumentReader("nonexistent.eml", mockParser, "single", false));

        // Test unsupported file extension
        assertThrows(IllegalArgumentException.class,
                () -> new EmailDocumentReader("test.txt", mockParser, "single", false));
    }

    @Test
    void testSuccessfulEmailParsing() throws IOException {
        // Mock parser response
        Document doc1 = new Document("Email content 1");
        Document doc2 = new Document("Email content 2");
        when(mockParser.parse(any())).thenReturn(Arrays.asList(doc1, doc2));

        // Test EML file
        EmailDocumentReader emlReader = new EmailDocumentReader(
                emlFile.toString(), mockParser, "single", false);
        List<Document> emlDocs = emlReader.get();
        assertEquals(2, emlDocs.size());
        assertEquals(emlFile.toString(), emlDocs.get(0).getMetadata().get("source"));
        assertEquals(emlFile.toString(), emlDocs.get(1).getMetadata().get("source"));

        // Test MSG file
        EmailDocumentReader msgReader = new EmailDocumentReader(
                msgFile.toString(), mockParser, "single", false);
        List<Document> msgDocs = msgReader.get();
        assertEquals(2, msgDocs.size());
        assertEquals(msgFile.toString(), msgDocs.get(0).getMetadata().get("source"));
        assertEquals(msgFile.toString(), msgDocs.get(1).getMetadata().get("source"));
    }

    @Test
    void testParsingError() throws IOException {
        // Mock parser to throw exception
        when(mockParser.parse(any())).thenThrow(new IOException("Parse error"));

        EmailDocumentReader reader = new EmailDocumentReader(
                emlFile.toString(), mockParser, "single", false);
        
        RuntimeException exception = assertThrows(RuntimeException.class, reader::get);
        assertTrue(exception.getMessage().contains("Failed to read email file"));
    }

} 