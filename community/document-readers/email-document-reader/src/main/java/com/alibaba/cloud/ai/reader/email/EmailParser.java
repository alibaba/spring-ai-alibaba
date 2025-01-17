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

import com.alibaba.cloud.ai.parser.bshtml.BsHtmlDocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Email parser utility class
 * Provides methods to parse email content and extract metadata
 *
 * @author xiadong
 * @since 2024-01-06
 */
public class EmailParser {

    private static final Logger log = LoggerFactory.getLogger(EmailParser.class);

    // Valid content types for email body
    private static final List<String> VALID_CONTENT_TYPES = List.of("text/html", "text/plain");

    // Pattern for email address extraction
    private static final Pattern EMAIL_PATTERN = Pattern.compile("<([^>]+)>");

    // Pattern for IP address extraction
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    // Pattern for IP hostname extraction
    private static final Pattern IP_NAME_PATTERN = Pattern.compile("\\b[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b");

    // Pattern for MAPI ID extraction
    private static final Pattern MAPI_PATTERN = Pattern.compile("MapiId=([^;]+)");

    // Pattern for datetime extraction
    private static final Pattern DATETIME_PATTERN = Pattern.compile("\\w{3}, \\d{1,2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} [+-]\\d{4}");

    // HTML parser
    private static final BsHtmlDocumentParser HTML_PARSER = new BsHtmlDocumentParser();

    /**
     * Parse email header and extract elements
     * @param message The email message
     * @return List of email elements
     */
    public static List<EmailElement> parseEmailHeader(MimeMessage message) throws MessagingException {
        List<EmailElement> elements = new ArrayList<>();

        // Parse headers
        Enumeration<Header> headers = message.getAllHeaders();
        while (headers.hasMoreElements()) {
            Header header = headers.nextElement();
            String name = header.getName();
            String value = header.getValue();

            switch (name) {
                case "To":
                    parseRecipients(value).forEach(elements::add);
                    break;
                case "From":
                    parseSenders(value).forEach(elements::add);
                    break;
                case "Subject":
                    elements.add(new Subject(value));
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
    private static List<Recipient> parseRecipients(String data) {
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
    private static List<Sender> parseSenders(String data) {
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
    private static List<ReceivedInfo> parseReceivedData(String data) {
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

        // Extract datetime
        Matcher dtMatcher = DATETIME_PATTERN.matcher(data);
        if (dtMatcher.find()) {
            String dtStr = dtMatcher.group();
            ZonedDateTime dt = ZonedDateTime.parse(dtStr, DateTimeFormatter.RFC_1123_DATE_TIME);
            elements.add(new ReceivedInfo("received_datetimetz", dtStr, dt));
        }

        return elements;
    }

    /**
     * Parse email address string into name and email parts
     * @param data Email address string
     * @return Array containing [name, email]
     */
    private static String[] parseEmailAddress(String data) {
        Matcher matcher = EMAIL_PATTERN.matcher(data);
        String email = matcher.find() ? matcher.group(1) : data;
        String name = data.split(Pattern.quote(email))[0].trim();
        name = name.replace("<", "").replace(">", "");
        
        if (name.isEmpty()) {
            name = email.split("@")[0];
        }
        
        return new String[]{StringUtils.capitalize(name.toLowerCase()), email};
    }

    /**
     * Extract attachments from email message
     * @param message The email message
     * @param outputDir Output directory for attachments
     * @return List of attachment information
     */
    public static List<AttachmentInfo> extractAttachments(MimeMessage message, File outputDir) throws MessagingException, IOException {
        List<AttachmentInfo> attachments = new ArrayList<>();
        
        Object content = message.getContent();
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                Part part = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                    String filename = part.getFileName();
                    if (filename == null) {
                        filename = "attachment_" + i;
                    }
                    
                    // Save attachment to file
                    File file = new File(outputDir, filename);
                    try (InputStream is = part.getInputStream()) {
                        org.apache.commons.io.FileUtils.copyInputStreamToFile(is, file);
                    }
                    
                    // Create attachment info
                    AttachmentInfo info = new AttachmentInfo();
                    info.setFilename(filename);
                    info.setContentType(part.getContentType());
                    info.setSize(part.getSize());
                    attachments.add(info);
                }
            }
        }
        
        return attachments;
    }

    /**
     * Get email content in HTML or plain text format
     * @param message The email message
     * @param preferHtml Whether to prefer HTML content over plain text
     * @return The email content
     */
    public static String getEmailContent(MimeMessage message, boolean preferHtml) throws MessagingException, IOException {
        Object content = message.getContent();
        
        if (content instanceof String) {
            return (String) content;
        }
        else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            String htmlContent = null;
            String textContent = null;
            
            // Find HTML and text content
            for (int i = 0; i < multipart.getCount(); i++) {
                Part part = multipart.getBodyPart(i);
                String contentType = part.getContentType().toLowerCase();
                
                if (contentType.contains("text/html")) {
                    htmlContent = new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                }
                else if (contentType.contains("text/plain")) {
                    textContent = new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            
            // Return preferred content type
            if (preferHtml && htmlContent != null) {
                // Parse HTML content using BsHtmlDocumentParser
                try (InputStream is = new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))) {
                    List<Document> docs = HTML_PARSER.parse(is);
                    if (!docs.isEmpty()) {
                        return docs.get(0).getContent();
                    }
                }
                return htmlContent;
            }
            else if (textContent != null) {
                return textContent;
            }
            else if (htmlContent != null) {
                // Parse HTML content using BsHtmlDocumentParser
                try (InputStream is = new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))) {
                    List<Document> docs = HTML_PARSER.parse(is);
                    if (!docs.isEmpty()) {
                        return docs.get(0).getContent();
                    }
                }
                return htmlContent;
            }
        }
        
        return "";
    }
}

/**
 * Class to hold attachment information
 */
class AttachmentInfo {
    private String filename;
    private String contentType;
    private int size;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
} 