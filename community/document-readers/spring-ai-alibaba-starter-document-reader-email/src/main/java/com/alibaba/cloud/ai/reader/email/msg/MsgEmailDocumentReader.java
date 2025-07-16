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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Microsoft Outlook MSG File Parser Handles Compound File Binary Format (CFB) used by
 * Microsoft Outlook for storing email messages
 *
 * @author brianxiadong
 * @since 2024-01-19
 */
public class MsgEmailDocumentReader implements DocumentReader {

	private static final Logger logger = LoggerFactory.getLogger(MsgEmailDocumentReader.class);

	private final String filename;

	public MsgEmailDocumentReader(String filename) {
		this.filename = filename;
	}

	@Override
	public List<Document> get() {
		try (InputStream inputStream = new FileInputStream(filename)) {
			// Parse MSG file structure
			MsgParser msgParser = new MsgParser(inputStream);
			MsgEmailElement emailElement = msgParser.parse();

			// Convert to unified Document format
			Document document = MsgEmailParser.convertToDocument(emailElement);
			return Collections.singletonList(document);
		}
		catch (Exception e) {
			logger.error("Failed to parse MSG file: {}", filename, e);
			throw new RuntimeException("Failed to parse MSG file: " + filename, e);
		}
	}

}
