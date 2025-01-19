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

import com.alibaba.cloud.ai.parser.bshtml.BsHtmlDocumentParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A DocumentReader implementation that reads emails from Mbox format files. Mbox is a
 * common format for storing collections of email messages.
 *
 * @author brianxiadong
 */
public class MboxDocumentReader implements DocumentReader {

	private static final Logger logger = LoggerFactory.getLogger(MboxDocumentReader.class);

	public static final String DEFAULT_MESSAGE_FORMAT = "Date: %s\nFrom: %s\nTo: %s\nSubject: %s\nContent: %s";

	private static final Pattern FROM_LINE_PATTERN = Pattern.compile("^From .*\\d{4}$");

	private static final Pattern HEADER_PATTERN = Pattern.compile("^([^:]+):\\s*(.*)$");

	private static final Pattern BOUNDARY_PATTERN = Pattern.compile("boundary=\"?([^\"]+)\"?");

	private final File mboxFile;

	private final int maxCount;

	private final String messageFormat;

	private final BsHtmlDocumentParser htmlParser;

	private final SimpleDateFormat dateFormat;

	/**
	 * Creates a new MboxDocumentReader instance with default settings.
	 * @param mboxPath the absolute path to the mbox file
	 */
	public MboxDocumentReader(String mboxPath) {
		this(new File(mboxPath), 0, DEFAULT_MESSAGE_FORMAT);
	}

	/**
	 * Creates a new MboxDocumentReader instance with custom settings.
	 * @param mboxPath the absolute path to the mbox file
	 * @param maxCount maximum number of messages to read (0 for unlimited)
	 * @param messageFormat custom format for message content
	 */
	public MboxDocumentReader(String mboxPath, int maxCount, String messageFormat) {
		this(new File(mboxPath), maxCount, messageFormat);
	}

	/**
	 * Creates a new MboxDocumentReader instance with custom settings.
	 * @param mboxFile the mbox file
	 * @param maxCount maximum number of messages to read (0 for unlimited)
	 * @param messageFormat custom format for message content
	 */
	public MboxDocumentReader(File mboxFile, int maxCount, String messageFormat) {
		Assert.notNull(mboxFile, "Mbox file must not be null");
		Assert.isTrue(mboxFile.exists(), "Mbox file does not exist: " + mboxFile.getAbsolutePath());
		Assert.isTrue(mboxFile.isFile(), "Mbox path is not a file: " + mboxFile.getAbsolutePath());

		this.mboxFile = mboxFile;
		this.maxCount = maxCount;
		this.messageFormat = messageFormat;
		this.htmlParser = new BsHtmlDocumentParser();
		this.dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
	}

	@Override
	public List<Document> get() {
		try {
			return readMboxFile();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to read mbox file: " + mboxFile.getAbsolutePath(), e);
		}
	}

	private List<Document> readMboxFile() throws IOException {
		List<Document> documents = new ArrayList<>();
		int count = 0;
		StringBuilder currentMessage = new StringBuilder();
		boolean isFirstMessage = true;

		try (LineIterator it = FileUtils.lineIterator(mboxFile, StandardCharsets.UTF_8.name())) {
			while (it.hasNext()) {
				String line = it.nextLine();

				// Check if this is a new message
				if (FROM_LINE_PATTERN.matcher(line).matches()) {
					// Process previous message if exists
					if (!isFirstMessage && !currentMessage.isEmpty()) {
						Document doc = parseMessage(currentMessage.toString());
						if (doc != null) {
							documents.add(doc);
							count++;

							if (maxCount > 0 && count >= maxCount) {
								break;
							}
						}
						currentMessage.setLength(0);
					}
					isFirstMessage = false;
					// Start new message with the From line
					currentMessage.append(line).append("\n");
				}
				else {
					// Append line to current message
					currentMessage.append(line).append("\n");
				}
			}

			// Process the last message
			if (!currentMessage.isEmpty()) {
				Document doc = parseMessage(currentMessage.toString());
				if (doc != null && (maxCount == 0 || count < maxCount)) {
					documents.add(doc);
				}
			}
		}

		return documents;
	}

	private Document parseMessage(String messageContent) {
		Map<String, Object> metadata = new HashMap<>();
		Map<String, String> headers = new HashMap<>();
		StringBuilder content = new StringBuilder();
		String[] lines = messageContent.split("\n");

		boolean inHeaders = true;
		String boundary = null;
		boolean inHtmlPart = false;
		boolean skipCurrentPart = false;
		StringBuilder currentPart = new StringBuilder();

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];

			if (inHeaders) {
				if (line.trim().isEmpty()) {
					inHeaders = false;
					// Check if this is a multipart message
					String contentType = headers.get("Content-Type");
					if (contentType != null && contentType.contains("multipart")) {
						Matcher m = BOUNDARY_PATTERN.matcher(contentType);
						if (m.find()) {
							boundary = m.group(1);
						}
					}
					continue;
				}

				Matcher m = HEADER_PATTERN.matcher(line);
				if (m.matches()) {
					String name = m.group(1).trim();
					String value = m.group(2).trim();
					headers.put(name, value);
				}
				continue;
			}

			// Process message body
			if (boundary != null) {
				if (line.contains("--" + boundary)) {
					// Process the previous part if it exists
					if (!currentPart.isEmpty()) {
						if (inHtmlPart && !skipCurrentPart) {
							// Parse HTML content and set as current content
							String parsedHtml = parseHtmlContent(currentPart.toString());
							if (!parsedHtml.isEmpty()) {
								content = new StringBuilder(parsedHtml);
							}
						}
						else if (content.isEmpty() && !skipCurrentPart) {
							content = currentPart;
						}
					}
					currentPart.setLength(0);
					inHtmlPart = false;
					skipCurrentPart = false;
					continue;
				}

				// Check content type of the part
				if (line.startsWith("Content-Type:")) {
					if (line.contains("text/html")) {
						inHtmlPart = true;
						skipCurrentPart = false;
					}
					else if (!line.contains("text/plain")) {
						// Skip non-text parts
						skipCurrentPart = true;
					}
					continue;
				}

				if (!skipCurrentPart) {
					currentPart.append(line).append("\n");
				}
			}
			else {
				// For non-multipart messages
				String contentType = headers.get("Content-Type");
				if (contentType != null && contentType.contains("text/html")) {
					// If it's an HTML message, collect all lines for parsing
					content.append(line).append("\n");
					if (i == lines.length - 1) {
						// Parse the complete HTML content at the end
						String parsedHtml = parseHtmlContent(content.toString());
						if (!parsedHtml.isEmpty()) {
							content = new StringBuilder(parsedHtml);
						}
					}
				}
				else {
					content.append(line).append("\n");
				}
			}
		}

		// Extract metadata
		metadata.put("subject", headers.getOrDefault("Subject", ""));
		metadata.put("from", headers.getOrDefault("From", ""));
		metadata.put("to", headers.getOrDefault("To", ""));
		try {
			String dateStr = headers.get("Date");
			if (dateStr != null) {
				metadata.put("date", dateFormat.parse(dateStr));
			}
		}
		catch (ParseException e) {
			throw new RuntimeException("Failed to parse date: " + e.getMessage(), e);
		}

		// Check if content is empty
		String contentStr = content.toString().trim();
		if (contentStr.isEmpty()) {
			throw new RuntimeException(
					"Empty content found for message: " + headers.getOrDefault("Message-ID", "unknown"));
		}

		// Format the content
		String formattedContent = String.format(messageFormat, metadata.getOrDefault("date", ""), metadata.get("from"),
				metadata.get("to"), metadata.get("subject"), contentStr);

		// Check if formatted content is empty
		if (formattedContent.trim().isEmpty()) {
			throw new RuntimeException(
					"Empty formatted content for message: " + headers.getOrDefault("Message-ID", "unknown"));
		}

		// Use Message-ID as document ID
		String id = headers.getOrDefault("Message-ID", "msg-" + System.currentTimeMillis());

		return new Document(id, formattedContent, metadata);
	}

	private String parseHtmlContent(String html) {
		if (html == null || html.trim().isEmpty()) {
			throw new RuntimeException("HTML content is null or empty");
		}

		try (InputStream is = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8))) {
			List<Document> docs = htmlParser.parse(is);
			if (!docs.isEmpty()) {
				// Get parsed text content
				String text = docs.get(0).getText();
				if (text == null || text.trim().isEmpty()) {
					throw new RuntimeException("Parsed HTML content is empty");
				}
				// Remove extra whitespace characters
				return text.replaceAll("\\s+", " ").trim();
			}
			throw new RuntimeException("No documents returned from HTML parser");
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to parse HTML content: " + e.getMessage(), e);
		}
	}

}
