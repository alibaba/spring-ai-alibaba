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
package com.alibaba.cloud.ai.reader.arxiv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ArXiv Document Reader
 *
 * @author xiadong
 * @since 2024-01-06
 */
class ArxivDocumentReaderIT {

    private static final String PAPER_ID = "2401.00123";
    private static final String TITLE = "Test Paper";
    private static final List<String> AUTHORS = Arrays.asList("Author One", "Author Two");
    private static final String SUMMARY = "This is a test paper summary";
    private static final String CATEGORY = "cs.AI";

    ArxivDocumentReader reader;

    @BeforeEach
    void setUp() {
        reader = ArxivDocumentReader.builder()
                .paperId(PAPER_ID)
                .title(TITLE)
                .authors(AUTHORS)
                .summary(SUMMARY)
                .category(CATEGORY)
                .resourcePath(PAPER_ID + ".pdf")
                .build();
    }

    @Test
    void should_read_paper() {
        // when
        List<Document> documents = reader.get();

        // then
        assertThat(documents).isNotEmpty();
        
        // Verify document content and metadata
        Document doc = documents.get(0);
        assertThat(doc.getContent()).isNotEmpty();
        
        // Verify metadata
        assertThat(doc.getMetadata())
                .containsEntry(ArxivResource.SOURCE, PAPER_ID + ".pdf")
                .containsEntry("paperId", PAPER_ID)
                .containsEntry("title", TITLE)
                .containsEntry("authors", AUTHORS)
                .containsEntry("summary", SUMMARY)
                .containsEntry("category", CATEGORY)
                .containsEntry("pdfUrl", "https://arxiv.org/pdf/" + PAPER_ID + ".pdf");
        
        // Print for debugging
        System.out.println("Document source: " + doc.getMetadata().get(ArxivResource.SOURCE));
        System.out.println("Paper ID: " + doc.getMetadata().get("paperId"));
        System.out.println("Title: " + doc.getMetadata().get("title"));
        System.out.println("Authors: " + doc.getMetadata().get("authors"));
        System.out.println("Summary: " + doc.getMetadata().get("summary"));
        System.out.println("Category: " + doc.getMetadata().get("category"));
        System.out.println("PDF URL: " + doc.getMetadata().get("pdfUrl"));
        System.out.println("Content: " + doc.getContent());
    }

    @Test
    void should_handle_missing_paper() {
        // given
        reader = ArxivDocumentReader.builder()
                .paperId("0000.00000")  // Non-existent paper
                .build();

        // when
        List<Document> documents = reader.get();

        // then
        assertThat(documents).isEmpty();
    }
} 