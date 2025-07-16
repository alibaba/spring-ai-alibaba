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

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.io.UnsupportedEncodingException;
import javax.mail.internet.MimeUtility;

/**
 * Base class for all email elements Represents different parts of an email like subject,
 * sender, recipient, etc.
 *
 * @author brianxiadong
 * @since 2024-01-19
 */
public abstract class EmailElement {

	/**
	 * The text content of the element
	 */
	protected String text;

	/**
	 * Metadata associated with this element
	 */
	protected Map<String, Object> metadata;

	/**
	 * Constructor
	 * @param text The text content
	 */
	protected EmailElement(String text) {
		this.text = text;
		this.metadata = new HashMap<>();
	}

	/**
	 * Get the text content
	 * @return The text content
	 */
	public String getText() {
		return text;
	}

	/**
	 * Set the text content
	 * @param text The text content
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Get the metadata
	 * @return The metadata map
	 */
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	/**
	 * Set the metadata
	 * @param metadata The metadata map
	 */
	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

}

/**
 * Represents the subject of an email
 */
class Subject extends EmailElement {

	// Pattern for Q-encoded content
	private static final Pattern Q_ENCODED_PATTERN = Pattern.compile("=\\?[^?]+\\?[qQbB]\\?[^?]+\\?=");

	public Subject(String text) {
		super(decodeSubject(text));
	}

	/**
	 * Decode the subject text, handling Q-encoded and Base64 encoded content
	 * @param text The subject text to decode
	 * @return The decoded subject text
	 */
	private static String decodeSubject(String text) {
		if (text == null || text.isEmpty()) {
			return text;
		}

		try {
			// 使用 JavaMail 的 MimeUtility 来解码
			return MimeUtility.decodeText(text);
		}
		catch (UnsupportedEncodingException e) {
			// 如果解码失败，返回原始文本
			return text;
		}
	}

}

/**
 * Represents a sender of an email
 */
class Sender extends EmailElement {

	private final String name;

	public Sender(String name, String email) {
		super(email);
		this.name = name;
	}

	public String getName() {
		return name;
	}

}

/**
 * Represents a recipient of an email
 */
class Recipient extends EmailElement {

	private final String name;

	public Recipient(String name, String email) {
		super(email);
		this.name = name;
	}

	public String getName() {
		return name;
	}

}

/**
 * Represents metadata information in an email
 */
class MetaData extends EmailElement {

	private final String name;

	public MetaData(String name, String value) {
		super(value);
		this.name = name;
	}

	public String getName() {
		return name;
	}

}

/**
 * Represents received information in an email header
 */
class ReceivedInfo extends EmailElement {

	private final String name;

	private final ZonedDateTime datestamp;

	public ReceivedInfo(String name, String text, ZonedDateTime datestamp) {
		super(text);
		this.name = name;
		this.datestamp = datestamp;
	}

	public String getName() {
		return name;
	}

	public ZonedDateTime getDatestamp() {
		return datestamp;
	}

}
