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

import com.alibaba.cloud.ai.parser.bshtml.BsHtmlDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

/**
 * Email parser utility class Provides methods to parse email content and extract metadata
 *
 * @author brianxiadong
 * @since 2024-01-06
 */
public class EmailParser {

	private final Logger log = LoggerFactory.getLogger(EmailParser.class);

	// Pattern for email address extraction
	private final Pattern EMAIL_PATTERN = Pattern.compile("<([^>]+)>");

	// Pattern for IP address extraction
	private final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

	// Pattern for IP hostname extraction
	private final Pattern IP_NAME_PATTERN = Pattern.compile("\\b[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b");

	// Pattern for MAPI ID extraction
	private final Pattern MAPI_PATTERN = Pattern.compile("MapiId=([^;]+)");

	// HTML parser
	private final BsHtmlDocumentParser htmlParser;

	/**
	 * Default constructor
	 */
	public EmailParser() {
		this.htmlParser = new BsHtmlDocumentParser();
	}

	/**
	 * Constructor with custom HTML parser
	 * @param htmlParser Custom HTML parser
	 */
	public EmailParser(BsHtmlDocumentParser htmlParser) {
		this.htmlParser = htmlParser;
	}

	/**
	 * Parse email header and extract elements
	 * @param message The email message
	 * @return List of email elements
	 */
	public List<EmailElement> parseEmailHeader(MimeMessage message) throws MessagingException {
		List<EmailElement> elements = new ArrayList<>();

		// Parse headers
		Enumeration<Header> headers = message.getAllHeaders();
		while (headers.hasMoreElements()) {
			Header header = headers.nextElement();
			String name = header.getName();
			String value = header.getValue();

			switch (name) {
				case "To":
					elements.addAll(parseRecipients(value));
					break;
				case "From":
					elements.addAll(parseSenders(value));
					break;
				case "Subject":
					elements.add(new Subject(value));
					break;
				case "Date":
					elements.add(new MetaData("date", value));
					break;
				case "Received":
					elements.addAll(parseReceivedData(value));
					break;
				default:
					elements.add(new MetaData(name, value));
			}
		}

		return elements;
	}

	/**
	 * Parse email recipients
	 * @param data Recipients string
	 * @return List of Recipient elements
	 */
	private List<Recipient> parseRecipients(String data) {
		List<Recipient> recipients = new ArrayList<>();
		String[] addresses = data.split(",");

		for (String address : addresses) {
			String[] parts = parseEmailAddress(address.trim());
			recipients.add(new Recipient(parts[0], parts[1]));
		}

		return recipients;
	}

	/**
	 * Parse email senders
	 * @param data Senders string
	 * @return List of Sender elements
	 */
	private List<Sender> parseSenders(String data) {
		List<Sender> senders = new ArrayList<>();
		String[] addresses = data.split(",");

		for (String address : addresses) {
			String[] parts = parseEmailAddress(address.trim());
			senders.add(new Sender(parts[0], parts[1]));
		}

		return senders;
	}

	/**
	 * Parse received data from email header
	 * @param data Received data string
	 * @return List of ReceivedInfo elements
	 */
	private List<ReceivedInfo> parseReceivedData(String data) {
		List<ReceivedInfo> elements = new ArrayList<>();

		// Extract IP hostnames
		Matcher ipNameMatcher = IP_NAME_PATTERN.matcher(data);
		List<String> ipNames = new ArrayList<>();
		while (ipNameMatcher.find()) {
			ipNames.add(ipNameMatcher.group());
		}

		// Extract IP addresses
		Matcher ipMatcher = IP_PATTERN.matcher(data);
		List<String> ips = new ArrayList<>();
		while (ipMatcher.find()) {
			ips.add(ipMatcher.group());
		}

		// Create ReceivedInfo elements for IP addresses
		for (int i = 0; i < Math.min(ipNames.size(), ips.size()); i++) {
			elements.add(new ReceivedInfo(ipNames.get(i), ips.get(i), null));
		}

		// Extract MAPI ID
		Matcher mapiMatcher = MAPI_PATTERN.matcher(data);
		if (mapiMatcher.find()) {
			elements.add(new ReceivedInfo("mapi_id", mapiMatcher.group(1), null));
		}

		return elements;
	}

	/**
	 * Parse email address string into name and email parts
	 * @param data Email address string
	 * @return Array containing [name, email]
	 */
	private String[] parseEmailAddress(String data) {
		try {
			// Handle Base64 encoded name
			if (data.contains("=?utf-8?B?")) {
				// Extract email address first
				Matcher matcher = EMAIL_PATTERN.matcher(data);
				String email = matcher.find() ? matcher.group(1) : data;

				// Extract and decode Base64 name
				String encodedPart = data.substring(data.indexOf("=?utf-8?B?") + 10, data.indexOf("?="));
				String name = new String(Base64.getDecoder().decode(encodedPart), StandardCharsets.UTF_8);

				return new String[] { name, email };
			}

			// Handle normal email address format
			Matcher matcher = EMAIL_PATTERN.matcher(data);
			if (matcher.find()) {
				// Email with angle brackets
				String email = matcher.group(1);
				String name = data.substring(0, data.indexOf("<")).trim();
				if (name.isEmpty()) {
					name = email.split("@")[0];
				}
				return new String[] { StringUtils.capitalize(name.toLowerCase()), email };
			}
			else {
				// Plain email address without name
				String email = data.trim();
				String name = email.split("@")[0];
				return new String[] { StringUtils.capitalize(name), email };
			}
		}
		catch (Exception e) {
			log.warn("Failed to parse email address: {}", data, e);
			// If parsing fails, try to extract just the email address
			try {
				String email = data.trim();
				if (email.contains("@")) {
					String name = email.split("@")[0];
					return new String[] { StringUtils.capitalize(name), email };
				}
			}
			catch (Exception ex) {
				log.error("Failed to extract email from: {}", data, ex);
			}
			// Return a default value in case of parsing error
			return new String[] { "Unknown", data };
		}
	}

	/**
	 * Get email content in HTML or plain text format
	 * @param part The email part
	 * @param preferHtml Whether to prefer HTML content over plain text
	 * @return Email content
	 */
	public String getEmailContent(Part part, boolean preferHtml) throws MessagingException, IOException {
		String htmlContent = null;
		String textContent = null;

		Object content = part.getContent();
		if (content instanceof String) {
			// Check if content is HTML
			String contentType = part.getContentType().toLowerCase();
			if (contentType.contains("text/html")) {
				htmlContent = (String) content;
			}
			else {
				textContent = (String) content;
			}
		}
		else if (content instanceof Multipart) {
			Multipart multipart = (Multipart) content;
			for (int i = 0; i < multipart.getCount(); i++) {
				Part bodyPart = multipart.getBodyPart(i);
				String contentType = bodyPart.getContentType().toLowerCase();

				if (contentType.contains("text/html")) {
					htmlContent = new String(bodyPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
				}
				else if (contentType.contains("text/plain")) {
					textContent = new String(bodyPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
				}
			}
		}

		// Parse HTML content if available and preferred
		if (htmlContent != null && (preferHtml || textContent == null)) {
			return parseHtmlContent(htmlContent);
		}

		// Return plain text if available
		if (textContent != null) {
			return textContent;
		}

		return "";
	}

	/**
	 * Get email content in HTML or plain text format
	 * @param message The email message
	 * @param preferHtml Whether to prefer HTML content over plain text
	 * @return Email content
	 */
	public String getEmailContent(MimeMessage message, boolean preferHtml) throws MessagingException, IOException {
		return getEmailContent((Part) message, preferHtml);
	}

	/**
	 * Parse HTML content using BsHtmlDocumentParser
	 * @param htmlContent HTML content to parse
	 * @return Parsed text content
	 */
	private String parseHtmlContent(String htmlContent) throws IOException {
		try (InputStream is = new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))) {
			List<Document> docs = htmlParser.parse(is);
			if (!docs.isEmpty()) {
				return docs.get(0).getText();
			}
		}
		return htmlContent;
	}

}
