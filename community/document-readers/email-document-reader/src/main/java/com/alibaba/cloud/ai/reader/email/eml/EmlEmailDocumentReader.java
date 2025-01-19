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
package com.alibaba.cloud.ai.reader.email.eml;

import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.alibaba.cloud.ai.parser.bshtml.BsHtmlDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.Multipart;
import javax.mail.Part;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

/**
 * EmlEmailDocumentReader - Used to read .eml email files and convert them to Document
 * format This reader supports: 1. Reading email content (both HTML and plain text) 2.
 * Extracting email metadata (subject, from, to, date, etc.) 3. Processing email
 * attachments
 *
 * @author xiadong
 * @since 2024-01-19
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
	 * Email parser instance
	 */
	private final EmailParser emailParser;

	/**
	 * TikaDocumentParser instance for parsing attachments
	 */
	private final TikaDocumentParser tikaDocumentParser;

	/**
	 * BsHtmlDocumentParser instance for parsing HTML attachments
	 */
	private final BsHtmlDocumentParser bsHtmlDocumentParser;

	/**
	 * Constructor with file path
	 * @param filePath The absolute path to the email file
	 */
	public EmlEmailDocumentReader(String filePath) {
		this(filePath, false);
	}

	/**
	 * Constructor with file path and attachment processing flag
	 * @param filePath The absolute path to the email file
	 * @param processAttachments Whether to process attachments
	 */
	public EmlEmailDocumentReader(String filePath, boolean processAttachments) {
		this(filePath, processAttachments, true);
	}

	/**
	 * Constructor with file path and all options
	 * @param filePath The absolute path to the email file
	 * @param processAttachments Whether to process attachments
	 * @param preferHtml Whether to prefer HTML content over plain text
	 */
	public EmlEmailDocumentReader(String filePath, boolean processAttachments, boolean preferHtml) {
		Assert.hasText(filePath, "File path must not be empty");
		this.emailResource = new org.springframework.core.io.FileSystemResource(filePath);
		this.processAttachments = processAttachments;
		this.preferHtml = preferHtml;
		this.emailParser = new EmailParser();
		this.tikaDocumentParser = new TikaDocumentParser();
		this.bsHtmlDocumentParser = new BsHtmlDocumentParser(org.jsoup.parser.Parser.htmlParser());
	}

	@Override
	public List<Document> get() {
		try (InputStream inputStream = emailResource.getInputStream()) {
			// Create email message from input stream
			Properties props = new Properties();
			Session session = Session.getDefaultInstance(props, null);
			MimeMessage message = new MimeMessage(session, inputStream);

			// Parse email headers
			List<EmailElement> headerElements = emailParser.parseEmailHeader(message);

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

			List<Document> documents = new ArrayList<>();
			processMessageContent(message, metadata, documents);
			return documents;
		}
		catch (Exception e) {
			log.error("Failed to read email file: " + emailResource.getFilename(), e);
			throw new RuntimeException("Failed to read email file", e);
		}
	}

	/**
	 * Process message content recursively, handling nested multipart content
	 * @param part The message part to process
	 * @param metadata The metadata to include in documents
	 * @param documents The list to add documents to
	 */
	private void processMessageContent(Part part, Map<String, Object> metadata, List<Document> documents)
			throws Exception {
		Object content = part.getContent();

		if (content instanceof Multipart) {
			// Handle multipart content
			Multipart multipart = (Multipart) content;

			for (int i = 0; i < multipart.getCount(); i++) {
				Part bodyPart = multipart.getBodyPart(i);
				String disposition = bodyPart.getDisposition();
				String contentType = bodyPart.getContentType().toLowerCase();

				if (disposition == null) {
					// This could be main content or nested multipart
					if (bodyPart.getContent() instanceof Multipart) {
						processMessageContent(bodyPart, metadata, documents);
					}
					else if (contentType.contains("text/plain") || contentType.contains("text/html")) {
						// Add each content part as a separate document
						String partContent = emailParser.getEmailContent(bodyPart, preferHtml);
						Map<String, Object> contentMetadata = new HashMap<>(metadata);
						contentMetadata.put("content_type", contentType);
						documents.add(new Document(partContent, contentMetadata));
					}
				}
				else if (processAttachments && Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
					// Handle attachment
					processAttachment(bodyPart, metadata, documents);
				}
			}
		}
		else if (content instanceof String) {
			// Handle simple text/html content
			String mainContent = emailParser.getEmailContent(part, preferHtml);
			Map<String, Object> mainMetadata = new HashMap<>(metadata);
			mainMetadata.put("content_type", part.getContentType().toLowerCase());
			documents.add(new Document(mainContent, mainMetadata));
		}
	}

	/**
	 * Process an attachment part
	 * @param part The attachment part
	 * @param metadata The base metadata
	 * @param documents The list to add the attachment document to
	 */
	private void processAttachment(Part part, Map<String, Object> metadata, List<Document> documents) throws Exception {
		String filename = part.getFileName();
		if (filename == null) {
			filename = "attachment_" + System.currentTimeMillis();
		}

		// Create attachment metadata
		Map<String, Object> attachmentMetadata = new HashMap<>(metadata);
		attachmentMetadata.put("filename", filename);
		attachmentMetadata.put("content_type", part.getContentType());
		attachmentMetadata.put("size", part.getSize());

		// Choose appropriate parser based on content type
		try (InputStream is = part.getInputStream()) {
			String contentType = part.getContentType().toLowerCase();
			List<Document> parsedDocuments;

			if (contentType.contains("text/html") || contentType.contains("application/html")) {
				// Use BsHtmlDocumentParser for HTML content
				parsedDocuments = bsHtmlDocumentParser.parse(is);
			}
			else {
				// Use TikaDocumentParser for other content types
				parsedDocuments = tikaDocumentParser.parse(is);
			}

			if (!parsedDocuments.isEmpty()) {
				// Add attachment metadata to parsed documents
				for (Document doc : parsedDocuments) {
					doc.getMetadata().putAll(attachmentMetadata);
				}
				documents.addAll(parsedDocuments);
			}
		}
	}

}
