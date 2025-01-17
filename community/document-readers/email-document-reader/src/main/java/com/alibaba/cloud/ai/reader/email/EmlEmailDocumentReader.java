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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

/**
 * EmlEmailDocumentReader - Used to read .eml email files and convert them to Document format
 * This reader supports:
 * 1. Reading email content (both HTML and plain text)
 * 2. Extracting email metadata (subject, from, to, date, etc.)
 * 3. Processing email attachments
 *
 * @author xiadong
 * @since 2024-01-06
 */
public class EmlEmailDocumentReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(EmlEmailDocumentReader.class);

    /**
     * Metadata key for email source
     */
    public static final String METADATA_SOURCE = "source";

    /**
     * The email resource to read from
     */
    private final Resource emailResource;

    /**
     * Whether to process attachments in the email
     */
    private final boolean processAttachments;

    /**
     * Whether to prefer HTML content over plain text
     */
    private final boolean preferHtml;

    /**
     * Constructor with email resource
     * @param emailResource The email resource to read from
     */
    public EmlEmailDocumentReader(Resource emailResource) {
        this(emailResource, false);
    }

    /**
     * Constructor with email resource and attachment processing flag
     * @param emailResource The email resource to read from
     * @param processAttachments Whether to process attachments
     */
    public EmlEmailDocumentReader(Resource emailResource, boolean processAttachments) {
        this(emailResource, processAttachments, true);
    }

    /**
     * Constructor with all options
     * @param emailResource The email resource to read from
     * @param processAttachments Whether to process attachments
     * @param preferHtml Whether to prefer HTML content over plain text
     */
    public EmlEmailDocumentReader(Resource emailResource, boolean processAttachments, boolean preferHtml) {
        Assert.notNull(emailResource, "Email resource must not be null");
        this.emailResource = emailResource;
        this.processAttachments = processAttachments;
        this.preferHtml = preferHtml;
    }

    @Override
    public List<Document> get() {
        try (InputStream inputStream = emailResource.getInputStream()) {
            // Create email message from input stream
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session, inputStream);

            // Parse email headers
            List<EmailElement> headerElements = EmailParser.parseEmailHeader(message);
            
            // Create metadata map from header elements
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(METADATA_SOURCE, emailResource.getFilename());
            
            for (EmailElement element : headerElements) {
                if (element instanceof Subject) {
                    metadata.put("subject", element.getText());
                }
                else if (element instanceof Sender) {
                    metadata.put("from", element.getText());
                    metadata.put("from_name", ((Sender) element).getName());
                }
                else if (element instanceof Recipient) {
                    metadata.put("to", element.getText());
                    metadata.put("to_name", ((Recipient) element).getName());
                }
                else if (element instanceof ReceivedInfo) {
                    ReceivedInfo info = (ReceivedInfo) element;
                    if (info.getDatestamp() != null) {
                        metadata.put("date", info.getDatestamp());
                    }
                }
                else if (element instanceof MetaData) {
                    metadata.put(((MetaData) element).getName().toLowerCase(), element.getText());
                }
            }

            // Get email content
            String content = EmailParser.getEmailContent(message, preferHtml);
            
            // Create main document
            List<Document> documents = new ArrayList<>();
            documents.add(new Document(content, metadata));

            // Process attachments if enabled
            if (processAttachments) {
                File tempDir = Files.createTempDirectory("email_attachments").toFile();
                try {
                    List<AttachmentInfo> attachments = EmailParser.extractAttachments(message, tempDir);
                    
                    // Create documents for attachments
                    for (AttachmentInfo attachment : attachments) {
                        Map<String, Object> attachmentMetadata = new HashMap<>(metadata);
                        attachmentMetadata.put("filename", attachment.getFilename());
                        attachmentMetadata.put("content_type", attachment.getContentType());
                        attachmentMetadata.put("size", attachment.getSize());
                        
                        // Read attachment content
                        File attachmentFile = new File(tempDir, attachment.getFilename());
                        String attachmentContent = Files.readString(attachmentFile.toPath());
                        
                        documents.add(new Document(attachmentContent, attachmentMetadata));
                    }
                }
                finally {
                    // Clean up temporary directory
                    org.apache.commons.io.FileUtils.deleteDirectory(tempDir);
                }
            }
            
            return documents;
        }
        catch (Exception e) {
            log.error("Failed to read email file: " + emailResource.getFilename(), e);
            throw new RuntimeException("Failed to read email file", e);
        }
    }

} 