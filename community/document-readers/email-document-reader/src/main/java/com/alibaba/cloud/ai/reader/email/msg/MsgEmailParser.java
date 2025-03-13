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

import org.apache.commons.io.IOUtils;
import org.apache.poi.hmef.Attachment;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.document.MetadataMode;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MSG Email Parser Responsible for converting MsgEmailElement to Document object
 *
 * @author xiadong
 * @since 2024-01-19
 */
public class MsgEmailParser {

	private MsgEmailParser() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Convert MsgEmailElement to Document
	 * @param element MSG email element
	 * @return Document object
	 */
	public static Document convertToDocument(MsgEmailElement element) {
		if (element == null) {
			throw new IllegalArgumentException("MsgEmailElement cannot be null");
		}

		// Build metadata
		Map<String, Object> metadata = new HashMap<>();

		// Add metadata with null check
		if (StringUtils.hasText(element.getSubject())) {
			metadata.put("subject", element.getSubject());
		}

		if (StringUtils.hasText(element.getFrom())) {
			metadata.put("from", element.getFrom());
		}

		if (StringUtils.hasText(element.getFromName())) {
			metadata.put("from_name", element.getFromName());
		}

		if (StringUtils.hasText(element.getTo())) {
			metadata.put("to", element.getTo());
		}

		if (StringUtils.hasText(element.getToName())) {
			metadata.put("to_name", element.getToName());
		}

		if (StringUtils.hasText(element.getDate())) {
			metadata.put("date", element.getDate());
		}

		if (StringUtils.hasText(element.getTextType())) {
			metadata.put("content_type", element.getTextType());
		}

		// Create Document object with content null check
		String content = StringUtils.hasText(element.getText()) ? element.getText() : "";
		return new Document(content, metadata);
	}

}