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
package com.alibaba.cloud.ai.reader.yuque;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author YunLong
 */
public class YuQueDocumentReader implements DocumentReader {

	private final DocumentParser parser;

	private final YuQueResource yuQueResource;

	public YuQueDocumentReader(YuQueResource yuQueResource, DocumentParser parser) {
		this.yuQueResource = yuQueResource;
		this.parser = parser;
	}

	@Override
	public List<Document> get() {
		try {
			List<Document> documents = parser.parse(yuQueResource.getInputStream());
			String source = yuQueResource.getResourcePath();

			for (Document doc : documents) {
				doc.getMetadata().put(YuQueResource.SOURCE, source);
			}

			return documents;
		}
		catch (IOException ioException) {
			throw new RuntimeException("Failed to load document from yuque: {}", ioException);
		}
	}

}
