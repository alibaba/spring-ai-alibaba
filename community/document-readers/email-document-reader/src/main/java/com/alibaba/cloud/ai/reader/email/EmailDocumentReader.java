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
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * EmailDocumentReader - Used to read email files (.eml, .msg) and convert them to Document format.
 * Supports both .eml and .msg file formats.
 *
 * @author brianxiadong
 */
public class EmailDocumentReader implements DocumentReader {

    private final Path filePath;
    private final DocumentParser parser;
    private final String mode;
    private final boolean processAttachments;

    /**
     * Constructor for EmailDocumentReader.
     *
     * @param filePath Path to the email file (.eml or .msg)
     * @param parser Document parser to use
     * @param mode Processing mode ("single", "elements", or "paged")
     * @param processAttachments Whether to process email attachments
     */
    public EmailDocumentReader(String filePath, DocumentParser parser, String mode, boolean processAttachments) {
        Assert.hasText(filePath, "filePath must not be empty");
        Assert.notNull(parser, "parser must not be null");
        Assert.hasText(mode, "mode must not be empty");
        
        this.filePath = Paths.get(filePath);
        this.parser = parser;
        this.mode = mode;
        this.processAttachments = processAttachments;
        
        validateFile();
    }

    /**
     * Validate the email file exists and has correct extension.
     */
    private void validateFile() {
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        String fileName = filePath.toString().toLowerCase();
        if (!fileName.endsWith(".eml") && !fileName.endsWith(".msg")) {
            throw new IllegalArgumentException("Unsupported file type. Only .eml and .msg files are supported.");
        }
    }

    @Override
    public List<Document> get() {
        try {
            // Parse the email file using the provided parser
            List<Document> documents = parser.parse(Files.newInputStream(filePath));
            
            // Add source metadata to each document
            String source = filePath.toString();
            for (Document doc : documents) {
                doc.getMetadata().put("source", source);
            }

            return documents;
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to read email file: " + filePath, e);
        }
    }

} 