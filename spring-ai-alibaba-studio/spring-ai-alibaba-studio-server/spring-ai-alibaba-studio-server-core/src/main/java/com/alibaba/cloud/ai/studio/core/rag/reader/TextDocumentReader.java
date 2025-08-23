/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.rag.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A document reader implementation for text files. Reads and processes text content from
 * files or resources.
 *
 * @since 1.0.0.3
 */
public class TextDocumentReader implements DocumentReader {

	/** The resource containing the text content to be read */
	private final Resource resource;

	/**
	 * Creates a new TextDocumentReader for the specified file.
	 * @param file file
	 */
	public TextDocumentReader(File file) {
		resource = new FileSystemResource(file);
	}

	public TextDocumentReader(Resource resource) {
		this.resource = resource;
	}

	@Override
	public List<Document> get() {
		try {
			String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			if (text.isBlank()) {
				return List.of();
			}

			return List.of(new Document(text));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
