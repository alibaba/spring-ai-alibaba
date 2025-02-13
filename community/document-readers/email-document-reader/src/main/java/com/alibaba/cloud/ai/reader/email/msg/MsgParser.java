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
package com.alibaba.cloud.ai.reader.email.msg;

import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.alibaba.cloud.ai.parser.bshtml.BsHtmlDocumentParser;
import org.springframework.ai.document.Document;

/**
 * MSG File Parser Uses Apache POI library to parse MSG files in Compound File Binary
 * Format
 *
 * @author xiadong
 * @since 2024-01-19
 */
public class MsgParser {

	private static final Logger logger = LoggerFactory.getLogger(MsgParser.class);

	private final InputStream inputStream;

	public MsgParser(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	/**
	 * Parse MSG file
	 * @return Parsed email element object
	 */
	public MsgEmailElement parse() {
		try {
			MAPIMessage msg = new MAPIMessage(inputStream);
			MsgEmailElement email = new MsgEmailElement();

			// Parse email properties
			parseEmailProperties(msg, email);

			// Parse email body
			parseEmailBody(msg, email);

			// Parse attachments
			parseAttachments(msg, email);

			return email;
		}
		catch (Exception e) {
			logger.error("Failed to parse MSG file", e);
			throw new RuntimeException("Failed to parse MSG file", e);
		}
	}

	private void parseEmailProperties(MAPIMessage msg, MsgEmailElement email) {
		try {
			// Subject
			try {
				email.setSubject(msg.getSubject());
			}
			catch (ChunkNotFoundException e) {
				// maybe not found
				logger.debug("Subject not found in MSG file");
			}

			// From
			try {
				String fromAddress = msg.getDisplayFrom();
				if (fromAddress != null) {
					email.setFrom(fromAddress);
				}
			}
			catch (ChunkNotFoundException e) {
				logger.debug("From address not found in MSG file");
			}

			// From Name
			try {
				String fromName = msg.getDisplayFrom();
				if (fromName != null) {
					email.setFromName(fromName);
				}
			}
			catch (ChunkNotFoundException e) {
				logger.debug("From name not found in MSG file");
			}

			// To
			try {
				String[] recipients = msg.getRecipientEmailAddressList();
				if (recipients != null && recipients.length > 0) {
					email.setTo(recipients[0]);
				}
			}
			catch (ChunkNotFoundException e) {
				logger.debug("To address not found in MSG file");
			}

			// To Name
			try {
				String recipientName = msg.getRecipientNames();
				if (recipientName != null) {
					email.setToName(recipientName);
				}
			}
			catch (ChunkNotFoundException e) {
				logger.debug("To name not found in MSG file");
			}

			// Date
			try {
				if (msg.getMessageDate() != null) {
					email.setDate(msg.getMessageDate().toString());
				}
			}
			catch (ChunkNotFoundException e) {
				logger.debug("Date not found in MSG file");
			}
		}
		catch (Exception e) {
			logger.error("Error parsing email properties", e);
			throw new RuntimeException("Error parsing email properties", e);
		}
	}

	private void parseEmailBody(MAPIMessage msg, MsgEmailElement email) {
		try {
			// Try to get HTML body
			try {
				String htmlBody = msg.getHtmlBody();
				if (htmlBody != null) {
					email.setContentType("text/html");
					// Parse HTML content using BsHtmlDocumentParser
					BsHtmlDocumentParser htmlParser = new BsHtmlDocumentParser();
					try (InputStream htmlStream = new ByteArrayInputStream(htmlBody.getBytes(StandardCharsets.UTF_8))) {
						List<Document> parsedDocs = htmlParser.parse(htmlStream);
						if (!parsedDocs.isEmpty()) {
							email.setContent(parsedDocs.get(0).getText());
						}
						else {
							email.setContent(htmlBody); // Fallback to original HTML if
														// parsing fails
						}
					}
					return;
				}
			}
			catch (ChunkNotFoundException e) {
				logger.debug("HTML body not found in MSG file");
			}

			// If no HTML body, try to get text body
			try {
				String textBody = msg.getTextBody();
				if (textBody != null) {
					email.setContentType("text/plain");
					email.setContent(textBody);
				}
			}
			catch (ChunkNotFoundException e) {
				logger.debug("Text body not found in MSG file");
			}
		}
		catch (Exception e) {
			logger.error("Error parsing email body", e);
			throw new RuntimeException("Error parsing email body", e);
		}
	}

	private void parseAttachments(MAPIMessage msg, MsgEmailElement email) {
		try {
			org.apache.poi.hsmf.datatypes.AttachmentChunks[] attachments = msg.getAttachmentFiles();
			if (attachments != null) {
				for (org.apache.poi.hsmf.datatypes.AttachmentChunks attachment : attachments) {
					MsgEmailElement attachmentElement = new MsgEmailElement();
					attachmentElement.setSubject(attachment.getAttachFileName().getValue());
					attachmentElement.setContent(new String(attachment.getAttachData().getValue()));
					email.addAttachment(attachmentElement);
				}
			}
		}
		catch (Exception e) {
			logger.error("Error parsing attachments", e);
			throw new RuntimeException("Error parsing attachments", e);
		}
	}

}
